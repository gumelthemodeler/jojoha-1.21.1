package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.TimeStopSettle;
import org.gumel.jojoha.stand.skill.TimeStopSystem;

import java.util.List;
import java.util.Set;

public final class TimeStopView {
    private static boolean active;
    private static double centreX;
    private static double centreY;
    private static double centreZ;

    private static int remainingTicks;
    private static int totalTicks;
    private static double radius = TimeStopSystem.RADIUS;

    private static Set<Integer> frozenPlayers = Set.of();

    private static int quietTicks;

    private static int elapsedTicks;

    private static int introTicks;

    private TimeStopView() {
    }

    public static void set(boolean nowActive, double x, double y, double z, int remaining,
                           List<Integer> frozen, double reach, int quiet, int intro) {
        if (nowActive && !active) {
            elapsedTicks = 0;
            introTicks = intro;
        }
        active = nowActive;
        remainingTicks = remaining;

        if (nowActive) {
            radius = reach > 0.0 ? reach : TimeStopSystem.RADIUS;
            centreX = x;
            centreY = y;
            centreZ = z;
            totalTicks = Math.max(remaining, 1);
            frozenPlayers = Set.copyOf(frozen);
            quietTicks = quiet;
        }
    }

    public static void clear() {
        active = false;
        remainingTicks = 0;
        totalTicks = 0;
        elapsedTicks = 0;
        frozenPlayers = Set.of();
    }

    public static boolean movesFreely(Entity entity) {
        if (entity instanceof StandEntity stand) {
            Player owner = stand.getOwner();
            return owner != null && movesFreely(owner);
        }
        if (!(entity instanceof Player player)) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.player) {
            JojohaPlayerData data = ClientPlayerDataCache.data;
            return data.timeStopHeldTicks > 0 || !data.isTimeStopFrozen();
        }
        return !frozenPlayers.contains(player.getId());
    }

    public static void tick() {
        if (!active || Minecraft.getInstance().isPaused()) {
            return;
        }

        elapsedTicks++;
        if (--remainingTicks <= 0) {
            active = false;
            remainingTicks = 0;
        }
    }

    public static boolean active() {
        return active;
    }

    public static Vec3 centre() {
        return new Vec3(centreX, centreY, centreZ);
    }

    public static boolean settlingHere() {
        return active && ClientPlayerDataCache.data.timeStopHeldTicks > 0
                && elapsedTicks < introTicks;
    }

    public static int waveTicks() {
        return introTicks > 0 ? introTicks : TimeStopSettle.TAP_WAVE_TICKS;
    }

    public static int quietTicks() {
        return quietTicks;
    }

    public static int remainingTicks() {
        return remainingTicks;
    }

    public static int totalTicks() {
        return totalTicks;
    }

    public static boolean holds(Entity entity) {
        if (!active) {
            return false;
        }

        return global() || entity.position().add(0, entity.getBbHeight() * 0.5, 0)
                .distanceToSqr(centreX, centreY, centreZ) <= radius * radius;
    }

    public static boolean clientHolds(Entity entity) {
        return active
                && !(entity instanceof Player)
                && !(entity instanceof StandEntity)
                && holds(entity);
    }

    public static boolean holdsPosition(double x, double y, double z) {
        if (!active) {
            return false;
        }

        if (global()) {
            return true;
        }
        double dx = x - centreX;
        double dy = y - centreY;
        double dz = z - centreZ;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    public static boolean global() {
        return active && TimeStopSystem.global(radius);
    }

    public static float fieldRadius() {
        return global() ? GLOBAL_FIELD : (float) radius;
    }

    private static final float GLOBAL_FIELD = 4096F;

    public static float progress() {
        if (!active || totalTicks <= 0) {
            return 0F;
        }

        return Math.min(1F, (totalTicks - remainingTicks) / (float) totalTicks);
    }
}
