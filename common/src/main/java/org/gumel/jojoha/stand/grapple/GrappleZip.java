package org.gumel.jojoha.stand.grapple;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class GrappleZip {
    private static final double SPEED = 2.05;

    public static final double ARRIVE = 1.5;

    public static double gap(Player player, Vec3 anchor) {
        return Math.min(player.getEyePosition().distanceTo(anchor), player.position().distanceTo(anchor));
    }

    private static final double LIFT = 0.3;
    private static final double LIFT_FROM = 4.0;

    private GrappleZip() {
    }

    public static boolean tick(Player player, Vec3 anchor) {
        Vec3 toAnchor = anchor.subtract(player.getEyePosition());
        double distance = toAnchor.length();

        if (gap(player, anchor) <= ARRIVE) {
            return false;
        }

        if (!closingIn(player, distance)) {
            return false;
        }

        Vec3 heading = toAnchor.scale(1.0 / distance);

        if (distance < LIFT_FROM) {
            double share = LIFT * (1.0 - distance / LIFT_FROM);
            heading = heading.add(0, share, 0).normalize();
        }

        player.setDeltaMovement(heading.scale(SPEED));

        player.resetFallDistance();
        return true;
    }

    private static final Map<Integer, Double> LAST_DISTANCE = new HashMap<>();

    private static final int STALL_TICKS = 4;

    private static final double PROGRESS = 0.05;

    private static final Map<Integer, Integer> STALLED = new HashMap<>();

    private static boolean closingIn(Player player, double distance) {
        int id = player.getId();
        Double previous = LAST_DISTANCE.put(id, distance);

        if (previous == null || previous - distance > PROGRESS) {
            STALLED.remove(id);
            return true;
        }

        int stalled = STALLED.merge(id, 1, Integer::sum);
        if (stalled < STALL_TICKS) {
            return true;
        }

        forget(player);
        return false;
    }

    public static void forget(Player player) {
        LAST_DISTANCE.remove(player.getId());
        STALLED.remove(player.getId());
    }
}
