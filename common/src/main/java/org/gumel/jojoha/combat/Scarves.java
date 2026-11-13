package org.gumel.jojoha.combat;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.accessory.Accessories;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.moves.SnakeMufflerMove;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModItems;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scarves {
    private static final double RELAY_RANGE = 64.0;

    private static final Map<UUID, ScarfPhysics.State> SERVER = new ConcurrentHashMap<>();
    private static final Map<UUID, ScarfPhysics.State> CLIENT = new ConcurrentHashMap<>();

    private Scarves() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(Scarves::tick);
        PlayerEvent.PLAYER_QUIT.register(player -> SERVER.remove(player.getUUID()));
        LifecycleEvent.SERVER_STOPPING.register(server -> SERVER.clear());
    }

    public static Map<UUID, ScarfPhysics.State> states(boolean client) {
        return client ? CLIENT : SERVER;
    }

    public static boolean worn(Player player) {
        return !player.isSpectator() && Accessories.wearing(player, ModItems.LISA_SCARF.get());
    }

    public static ScarfPhysics.State step(Player player) {
        return step(player, null);
    }

    public static ScarfPhysics.State step(Player player, Vec3 grip) {
        Map<UUID, ScarfPhysics.State> states = states(player.level().isClientSide);
        if (!worn(player)) {
            states.remove(player.getUUID());
            return null;
        }
        ScarfPhysics.State state = states.computeIfAbsent(player.getUUID(), id -> new ScarfPhysics.State());
        ScarfPhysics.step(state, player, player.level(), grip != null ? grip : ScarfPhysics.anchor(player, 1F));
        return state;
    }

    public static ScarfPhysics.State ready(ServerPlayer player) {
        ScarfPhysics.State state = SERVER.get(player.getUUID());
        return state == null || state.striking() ? null : state;
    }

    public static void strike(ServerPlayer player, ScarfPhysics.State state, LivingEntity target) {
        ScarfPhysics.begin(state, player, target);
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other.distanceToSqr(player) < RELAY_RANGE * RELAY_RANGE) {
                NetworkHandler.sendScarfStrike(other, player, target.getId());
            }
        }
    }

    public static void heard(Player player, Entity target) {
        ScarfPhysics.State state = CLIENT.get(player.getUUID());
        if (state != null && target != null) {
            ScarfPhysics.begin(state, player, target);
        }
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ScarfPhysics.State state = step(player);
            if (state == null) {
                continue;
            }
            if (state.hitNow && player.serverLevel().getEntity(state.target) instanceof LivingEntity target) {
                SnakeMufflerMove.strike(player, state, target);
            } else if (state.striking() && state.age < state.reachTicks && state.tip != null && player.tickCount % 2 == 0) {
                HamonFx.fineSpark(player.serverLevel(), state.tip.x, state.tip.y, state.tip.z,
                        2, 0.05, 0.02);
            }
        }
    }
}
