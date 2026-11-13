package org.gumel.jojoha.stand.combat;

import net.minecraft.world.phys.Vec3;

public record StandFist(int slot, long spawnTick, Vec3 aim, float bendSide, float bendUp) {
    public static final int SLOTS = 12;

    public static final int BODY_SLOTS = 2;

    public static final int OUT_TICKS = 3;

    public static final int BACK_TICKS = 2;

    private static final double[][] SHOULDERS = {
            {7, 22, -1}, {-7, 22, -1},
            {8, 24.5, 0}, {-8, 24.5, 0},
            {6.5, 19.5, -2}, {-6.5, 19.5, -2},
            {9.5, 21, 1}, {-9.5, 21, 1},
            {5, 26, 1}, {-5, 26, 1},
            {10, 17.5, 0}, {-10, 17.5, 0},
    };

    public boolean fromRight() {
        return slot % 2 == 0;
    }

    public boolean finished(double age) {
        return age >= OUT_TICKS + BACK_TICKS;
    }

    public boolean outbound(double age) {
        return age > 0 && age <= OUT_TICKS;
    }

    public Vec3 at(Vec3 shoulder, Vec3 right, double age) {
        if (age <= 0) {
            return shoulder;
        }
        if (age < OUT_TICKS) {
            double t = age / OUT_TICKS;
            t = 1 - (1 - t) * (1 - t);
            double side = fromRight() ? bendSide : -bendSide;
            Vec3 control = shoulder.lerp(aim, 0.5).add(right.scale(side)).add(0, bendUp, 0);
            double u = 1 - t;
            return shoulder.scale(u * u).add(control.scale(2 * u * t)).add(aim.scale(t * t));
        }
        double back = (age - OUT_TICKS) / BACK_TICKS;
        if (back >= 1) {
            return shoulder;
        }
        return aim.lerp(shoulder, back * back);
    }

    public static final double LOOP_PEAK = 2.0 / 3.0;

    public static final double LOOP_HANDOFF_TICKS = 2.0;

    public double loopSeconds(double age, double loopLength) {
        double half = loopLength / 2.0;
        double start = fromRight() ? half : 0.0;
        double a = Math.max(0.0, Math.min(age, OUT_TICKS + BACK_TICKS));
        double share = a <= OUT_TICKS
                ? LOOP_PEAK * a / OUT_TICKS
                : LOOP_PEAK + (1.0 - LOOP_PEAK) * (a - OUT_TICKS) / BACK_TICKS;
        return start + half * share;
    }

    public static double loopShare(double age) {
        double life = OUT_TICKS + BACK_TICKS;
        if (age <= 0.0 || age >= life) {
            return 0.0;
        }
        return Math.min(1.0, Math.min(age, life - age) / LOOP_HANDOFF_TICKS);
    }

    public static Vec3 shoulderModel(int slot) {
        double[] s = SHOULDERS[Math.floorMod(slot, SHOULDERS.length)];
        return new Vec3(s[0], s[1], s[2]);
    }

    public static Vec3 shoulderWorld(Vec3 origin, float yaw, int slot) {
        return toWorld(origin, yaw, shoulderModel(slot));
    }

    public static Vec3 right(float yaw) {
        return toWorld(Vec3.ZERO, yaw, new Vec3(16, 0, 0));
    }

    public static Vec3 toWorld(Vec3 origin, float yaw, Vec3 modelPx) {
        double theta = Math.toRadians(180.0 - yaw);
        double cos = Math.cos(theta), sin = Math.sin(theta);
        double x = modelPx.x * cos + modelPx.z * sin;
        double z = -modelPx.x * sin + modelPx.z * cos;
        return origin.add(x / 16.0, modelPx.y / 16.0, z / 16.0);
    }

    public static Vec3 toModel(Vec3 origin, float yaw, Vec3 world) {
        Vec3 offset = world.subtract(origin).scale(16.0);
        double theta = -Math.toRadians(180.0 - yaw);
        double cos = Math.cos(theta), sin = Math.sin(theta);
        return new Vec3(offset.x * cos + offset.z * sin, offset.y, -offset.x * sin + offset.z * cos);
    }
}
