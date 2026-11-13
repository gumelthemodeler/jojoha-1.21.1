package org.gumel.jojoha.combat;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.moves.ClackerVolleyMove;
import org.gumel.jojoha.item.ClackersItem;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.ClackerSwingPacket;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.StandBeat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Clackers {
    public static final float THROW_DAMAGE = 5F;
    public static final float SPIN_DAMAGE = 6.5F;
    public static final float WHIRL_SHARE = 0.35F;

    private static final double THROW_KNOCKBACK = 0.45;
    private static final double WHIRL_KNOCKBACK = 0.3;
    private static final double LAUNCH_KNOCKBACK = 0.8;
    private static final double CLACK_SPEED = 0.12;
    private static final double RELAY_RANGE = 64.0;

    private static final Map<UUID, ClackerPhysics.State> SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, ClackerPhysics.State> CLIENT = new ConcurrentHashMap<>();

    private Clackers() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(Clackers::tick);
        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, hand, hit) ->
                player.getMainHandItem().getItem() instanceof ClackersItem
                        ? EventResult.interruptFalse()
                        : EventResult.pass());
        PlayerEvent.PLAYER_QUIT.register(player -> SERVER.remove(player.getUUID()));
        LifecycleEvent.SERVER_STOPPING.register(server -> SERVER.clear());
    }

    public static Map<UUID, ClackerPhysics.State> states(boolean client) {
        return client ? CLIENT : SERVER;
    }

    public static InteractionHand heldHand(Player player) {
        if (player.isSpectator()) {
            return null;
        }
        if (player.getMainHandItem().getItem() instanceof ClackersItem) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().getItem() instanceof ClackersItem) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    public static ClackerPhysics.State step(Player player) {
        return step(player, null);
    }

    public static ClackerPhysics.State step(Player player, Vec3 grip) {
        Map<UUID, ClackerPhysics.State> states = states(player.level().isClientSide);
        InteractionHand hand = heldHand(player);
        if (hand == null) {
            states.remove(player.getUUID());
            return null;
        }
        Vec3 anchor = grip != null ? grip : ClackerPhysics.anchor(player, hand, 1.0F);
        ClackerPhysics.State state = states.get(player.getUUID());
        if (state == null || state.hand != hand) {
            state = new ClackerPhysics.State(anchor, hand);
            states.put(player.getUUID(), state);
        }
        ClackerPhysics.step(state, anchor, player);
        return state;
    }

    public static boolean throwFrom(Player player) {
        if (!(player.getMainHandItem().getItem() instanceof ClackersItem)) {
            return false;
        }
        ClackerPhysics.State state = states(player.level().isClientSide).get(player.getUUID());
        if (state == null || state.hand != InteractionHand.MAIN_HAND || !ClackerPhysics.canThrow(state)) {
            return false;
        }
        ClackerPhysics.windUp(state);
        return true;
    }

    public static boolean spin(Player player, InteractionHand hand) {
        ClackerPhysics.State state = states(player.level().isClientSide).get(player.getUUID());
        if (state == null || state.hand != hand) {
            return false;
        }
        boolean started = ClackerPhysics.spin(state, player);
        if (started && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.serverLevel().playSound(null, serverPlayer.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 0.5F, 1.5F);
        }
        return started;
    }

    public static void handleThrow(ServerPlayer player) {
        if (!throwFrom(player)) {
            return;
        }
        MovementFx.pose(player, MovementFx.CLACKER_HIT);
        StandBeat.at(ClackerPhysics.THROW_WINDUP_TICKS, player, () ->
                player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                        SoundSource.PLAYERS, 0.7F, 1.3F));
        relay(player, ClackerSwingPacket.THROW, -1, false);
    }

    public static void handleSpin(ServerPlayer player, InteractionHand hand) {
        if (spin(player, hand)) {
            MovementFx.pose(player, MovementFx.CLACKER_SPIN);
            StandBeat.at(ClackerPhysics.WHIRL_LAUNCH_TICKS, player, () ->
                    player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG,
                            SoundSource.PLAYERS, 0.8F, 1.2F));
            relay(player, ClackerSwingPacket.SPIN, -1, false);
        }
    }

    public static ClackerPhysics.State ready(ServerPlayer player) {
        ClackerPhysics.State state = SERVER.get(player.getUUID());
        if (state == null || state.hand != heldHand(player) || state.volleying()) {
            return null;
        }
        return state;
    }

    public static void volley(ServerPlayer player, ClackerPhysics.State state, LivingEntity target) {
        ClackerPhysics.volley(state, target.getId());
        relay(player, ClackerSwingPacket.VOLLEY, target.getId(), true);
    }

    private static void relay(ServerPlayer player, int kind, int targetId, boolean self) {
        for (ServerPlayer other : player.serverLevel().players()) {
            if ((self || other != player) && other.distanceToSqr(player) < RELAY_RANGE * RELAY_RANGE) {
                NetworkHandler.sendClackerSwing(other, player, kind, targetId);
            }
        }
    }

    public static boolean hittable(Entity owner, Entity target) {
        return target != owner && target.isAlive() && !target.isSpectator() && target instanceof LivingEntity
                && !(target instanceof StandEntity stand && stand.getOwner() == owner)
                && !owner.isPassengerOfSameVehicle(target);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ClackerPhysics.State state = step(player);
            if (state == null) {
                continue;
            }
            sounds(player, state);
            if (state.volleyStrike >= 0) {
                ClackerVolleyMove.strike(player, state,
                        state.heads[state.volleyStrike]);
            } else if (state.throwTicks > 0 || state.spinning) {
                strike(player, state);
            }
        }
    }

    private static void sounds(ServerPlayer player, ClackerPhysics.State state) {
        ServerLevel level = player.serverLevel();
        ClackerPhysics.Head a = state.heads[0];
        if (state.contact > CLACK_SPEED) {
            level.playSound(null, a.pos.x, a.pos.y, a.pos.z, SoundEvents.BAMBOO_HIT, SoundSource.PLAYERS,
                    (float) Math.min(1.0, 0.3 + state.contact), 1.5F + level.random.nextFloat() * 0.2F);
        }
        for (ClackerPhysics.Head head : state.heads) {
            if (head.groundImpact > 0.4) {
                level.playSound(null, head.pos.x, head.pos.y, head.pos.z, SoundEvents.BAMBOO_WOOD_HIT,
                        SoundSource.PLAYERS, 0.5F, 1.3F);
            }
        }
        if (state.spinning && state.spinElapsed % 4 == 0) {
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS,
                    0.35F, 1.2F + state.spinElapsed / (float) ClackerPhysics.WHIRL_LAUNCH_TICKS * 0.6F);
        }
    }

    private static void strike(ServerPlayer player, ClackerPhysics.State state) {
        ServerLevel level = player.serverLevel();
        boolean spinning = state.spinning;
        for (ClackerPhysics.Head head : state.heads) {
            AABB sweep = new AABB(head.prevPos, head.pos).inflate(ClackerPhysics.HEAD_RADIUS + 0.25);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, sweep,
                    e -> hittable(player, e) && !state.struck.contains(e.getId()))) {
                state.struck.add(target.getId());
                float base = spinning
                        ? JojohaServerConfig.CLACKER_SPIN_DAMAGE.value() * WHIRL_SHARE
                        : state.launched ? JojohaServerConfig.CLACKER_SPIN_DAMAGE.value()
                        : JojohaServerConfig.CLACKER_THROW_DAMAGE.value();
                if (target.hurt(player.damageSources().playerAttack(player),
                        DamageScale.combat(player, target, base))) {
                    double push = spinning ? WHIRL_KNOCKBACK : state.launched ? LAUNCH_KNOCKBACK : THROW_KNOCKBACK;
                    Vec3 away = head.vel.lengthSqr() > 1.0E-4 ? head.vel.normalize() : target.position()
                            .subtract(player.position()).normalize();
                    target.push(away.x * push, 0.15, away.z * push);
                    level.playSound(null, target.getX(), target.getY(0.5), target.getZ(), SoundEvents.BAMBOO_HIT,
                            SoundSource.PLAYERS, 0.9F, 1.1F + level.random.nextFloat() * 0.3F);
                    if (Fate.breathesHamon(PlayerDataAccess.get(player))) {
                        HamonFx.fineSpark(level, head.pos.x, head.pos.y, head.pos.z, 6, 0.15,
                                0.05);
                    }
                }
            }
        }
    }
}
