package org.gumel.jojoha.hamon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.combat.Climbing;
import org.gumel.jojoha.network.NetworkHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HamonState {

    private static final Map<UUID, Long> OVERDRIVE_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> BARRIER_UNTIL = new HashMap<>();
    private static final Map<UUID, Long> CHANNEL_UNTIL = new HashMap<>();

    private static final Set<UUID> BREATHING = new HashSet<>();
    private static final Set<UUID> WALKING = new HashSet<>();

    private static final Map<UUID, Integer> FIRING = new HashMap<>();

    private HamonState() {
    }

    public static void chargeOverdrive(ServerPlayer player, int ticks) {
        OVERDRIVE_UNTIL.put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    public static boolean hasOverdrive(Player player) {
        return until(OVERDRIVE_UNTIL, player) > player.level().getGameTime();
    }

    public static void consumeOverdrive(Player player) {
        OVERDRIVE_UNTIL.remove(player.getUUID());
    }

    public static void channel(ServerPlayer player, int ticks) {
        CHANNEL_UNTIL.put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    public static boolean isChannelling(Player player) {
        return until(CHANNEL_UNTIL, player) > player.level().getGameTime();
    }

    public static void raiseBarrier(ServerPlayer player, int ticks) {
        BARRIER_UNTIL.put(player.getUUID(), player.level().getGameTime() + ticks);
    }

    public static boolean barrierRunOut(Player player) {
        Long until = BARRIER_UNTIL.get(player.getUUID());
        if (until == null || until > player.level().getGameTime()) {
            return false;
        }
        BARRIER_UNTIL.remove(player.getUUID());
        return true;
    }

    public static boolean hasBarrier(Player player) {
        return until(BARRIER_UNTIL, player) > player.level().getGameTime();
    }

    public static void dropBarrier(Player player) {
        BARRIER_UNTIL.remove(player.getUUID());
    }

    public static boolean isBreathing(Player player) {
        return BREATHING.contains(player.getUUID());
    }

    public static void setBreathing(ServerPlayer player, boolean breathing) {
        if (breathing) {
            BREATHING.add(player.getUUID());
        } else {
            BREATHING.remove(player.getUUID());
        }
    }

    public static boolean isWalking(Player player) {
        return WALKING.contains(player.getUUID());
    }

    public static void setWalking(ServerPlayer player, boolean walking) {
        if (walking) {
            WALKING.add(player.getUUID());
        } else {
            WALKING.remove(player.getUUID());
        }
        NetworkHandler.sendClimbing(player,
                Climbing.Grip.RIPPLE, walking);
    }

    public static boolean isFiring(Player player) {
        return FIRING.containsKey(player.getUUID());
    }

    public static void startFiring(ServerPlayer player) {
        FIRING.put(player.getUUID(), 0);
    }

    public static void stopFiring(Player player) {
        FIRING.remove(player.getUUID());
    }

    public static int advanceFiring(Player player) {
        Integer held = FIRING.computeIfPresent(player.getUUID(), (id, ticks) -> ticks + 1);
        return held == null ? -1 : held;
    }

    public static void clear(LivingEntity entity) {
        OVERDRIVE_UNTIL.remove(entity.getUUID());
        BARRIER_UNTIL.remove(entity.getUUID());
        CHANNEL_UNTIL.remove(entity.getUUID());

        BREATHING.remove(entity.getUUID());
        WALKING.remove(entity.getUUID());
        FIRING.remove(entity.getUUID());
    }

    private static long until(Map<UUID, Long> from, Player player) {
        Long value = from.get(player.getUUID());
        return value == null ? Long.MIN_VALUE : value;
    }
}
