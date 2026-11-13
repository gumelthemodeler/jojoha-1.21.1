package org.gumel.jojoha.fortune;

import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.accessory.Charms;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.FlairCuePacket;
import org.gumel.jojoha.network.packet.FortuneSensePacket;
import org.gumel.jojoha.registry.ModEffects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Wicked {
    public static final double FLEE_RANGE = 10.0;

    public static final double FLEE_CLOSE = 5.0;

    public static final double FLEE_SPEED = 1.25;

    public static final int FLEE_EVERY = 10;

    public static final double TREMBLE_RADIUS = 8.0;

    public static final int BOW_AGAIN_TICKS = 600;

    public static final int TREMBLE_TICKS = 20;

    public static final double PRESENCE_RANGE = 32.0;

    public static final int PRESENCE_DIM_TICKS = 30;

    public static final int PRESENCE_REQUEUE_TICKS = 2400;

    private static final int PRESENCE_EVERY = 20;

    private static final int PRUNE_EVERY = 200;

    private static final Map<UUID, Map<UUID, Long>> NEAR = new HashMap<>();

    private static final Map<UUID, Long> BOWED = new HashMap<>();

    private Wicked() {
    }

    static void tick(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        if (player.tickCount % FLEE_EVERY == 0) {
            flee(player);
            bow(player, now);
        }
        if (player.tickCount % PRESENCE_EVERY == 0) {
            presence(player, now);
        }
        if (player.tickCount % PRUNE_EVERY == 0) {
            BOWED.values().removeIf(at -> now - at > BOW_AGAIN_TICKS);
        }
    }

    private static void flee(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (PathfinderMob mob : level.getEntitiesOfClass(PathfinderMob.class,
                player.getBoundingBox().inflate(FLEE_RANGE * Charms.fortune(player)), Wicked::skittish)) {
            if (mob.getTarget() != null) {
                continue;
            }
            if (!mob.getNavigation().isDone() && mob.distanceToSqr(player) > FLEE_CLOSE * FLEE_CLOSE) {
                continue;
            }
            Vec3 away = DefaultRandomPos.getPosAway(mob, 12, 6, player.position());
            if (away != null) {
                mob.getNavigation().moveTo(away.x, away.y, away.z, FLEE_SPEED);
            }
        }
    }

    static boolean skittish(PathfinderMob mob) {
        if (mob instanceof Enemy || mob.isLeashed() || mob.isVehicle()) {
            return false;
        }
        if (mob instanceof OwnableEntity owned && owned.getOwnerUUID() != null) {
            return false;
        }
        if (mob instanceof AbstractHorse horse && horse.isTamed()) {
            return false;
        }
        return mob instanceof Animal || mob instanceof AbstractVillager || mob instanceof NeutralMob;
    }

    private static void bow(ServerPlayer player, long now) {
        double radius = JojohaServerConfig.WICKED_RADIUS.value() * Charms.fortune(player);
        if (radius <= 0.0) {
            return;
        }
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius),
                mob -> mob instanceof Enemy && mob.isAlive() && mob.distanceToSqr(player) <= radius * radius)) {
            Long last = BOWED.get(mob.getUUID());
            if (last != null && now - last < BOW_AGAIN_TICKS) {
                continue;
            }
            BOWED.put(mob.getUUID(), now);
            mob.getNavigation().stop();
            tremble(player, mob);
        }
    }

    private static void tremble(ServerPlayer player, LivingEntity victim) {
        int ticks = Math.round(JojohaServerConfig.WICKED_TREMBLE.value() * Charms.fortune(player));
        if (ticks <= 0) {
            return;
        }
        victim.addEffect(new MobEffectInstance(ModEffects.trembling(), ticks, 0, false, false, true));
        NetworkHandler.broadcastMenace(player);
    }

    private static void presence(ServerPlayer player, long now) {
        Map<UUID, Long> near = NEAR.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        float amp = Charms.fortune(player);
        double range = PRESENCE_RANGE * amp;
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other == player || other.isSpectator() || other.distanceToSqr(player) > range * range) {
                continue;
            }
            if (near.put(other.getUUID(), now) == null) {
                NetworkHandler.sendFortuneSense(other, FortuneSensePacket.DIM,
                        Math.round(PRESENCE_DIM_TICKS * amp), List.of());
            }
        }
        near.values().removeIf(at -> now - at > PRESENCE_REQUEUE_TICKS);
    }

    static void forget(Player player) {
        NEAR.remove(player.getUUID());
        NEXT_ITCH.remove(player.getUUID());
    }

    public static final int THIRST_GRACE_TICKS = 24000;

    public static final int THIRST_DAY_TICKS = 24000;

    public static final int ITCH_FEWEST_TICKS = 600;

    public static final int ITCH_SPREAD_TICKS = 600;

    public static final float ITCH_STRENGTH = 0.15F;

    public static final float ITCH_PER_DAY = 0.05F;

    public static final float ITCH_MOST = 0.3F;

    public static final int ITCH_STEPS = 100;

    private static final Map<UUID, Long> NEXT_ITCH = new HashMap<>();

    static void killed(ServerPlayer killer, JojohaPlayerData data) {
        data.lastConflict = killer.level().getGameTime();
        PlayerDataAccess.set(killer, data);
        NEXT_ITCH.remove(killer.getUUID());
    }

    static void thirst(ServerPlayer player, JojohaPlayerData data) {
        long now = player.level().getGameTime();
        if (data.lastConflict <= 0L || data.lastConflict > now) {
            data.lastConflict = now;
            PlayerDataAccess.set(player, data);
            return;
        }
        long dry = now - data.lastConflict - THIRST_GRACE_TICKS;
        if (dry < 0L) {
            return;
        }
        Long next = NEXT_ITCH.get(player.getUUID());
        if (next == null) {
            NEXT_ITCH.put(player.getUUID(), now + ITCH_FEWEST_TICKS + player.getRandom().nextInt(ITCH_SPREAD_TICKS));
            return;
        }
        if (now < next) {
            return;
        }
        NEXT_ITCH.put(player.getUUID(), now + ITCH_FEWEST_TICKS + player.getRandom().nextInt(ITCH_SPREAD_TICKS));
        float strength = Math.min(ITCH_MOST, ITCH_STRENGTH + ITCH_PER_DAY * (dry / THIRST_DAY_TICKS));
        NetworkManager.sendToPlayer(player, new FlairCuePacket(player.getId(), FlairCuePacket.ITCH,
                Math.round(strength * ITCH_STEPS)));
    }
}
