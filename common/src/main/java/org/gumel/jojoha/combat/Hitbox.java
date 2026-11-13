package org.gumel.jojoha.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Hitbox {
    private static final double STEP = 0.2;

    private Hitbox() {
    }

    public static boolean inLane(Vec3 from, Vec3 look, double reach, double halfWidth, LivingEntity victim) {
        return distanceAlong(from, look, reach, halfWidth, victim) >= 0;
    }

    public static double distanceAlong(Vec3 from, Vec3 look, double reach, double halfWidth, LivingEntity victim) {
        AABB box = victim.getBoundingBox();
        double limit = halfWidth * halfWidth;
        int steps = Math.max(1, (int) Math.ceil(reach / STEP));
        for (int i = 0; i <= steps; i++) {
            double t = reach * i / steps;
            if (distanceSqr(box, from.add(look.scale(t))) <= limit) {
                return t;
            }
        }
        return -1;
    }

    private static double distanceSqr(AABB box, Vec3 point) {
        double dx = Math.max(Math.max(box.minX - point.x, 0.0), point.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - point.y, 0.0), point.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - point.z, 0.0), point.z - box.maxZ);
        return dx * dx + dy * dy + dz * dz;
    }
}
