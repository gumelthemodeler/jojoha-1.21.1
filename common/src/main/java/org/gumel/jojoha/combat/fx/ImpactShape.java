package org.gumel.jojoha.combat.fx;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class ImpactShape {

    private ImpactShape() {
    }

    public static Vec3[] cone(Vec3 heading, float spreadDegrees, int count, RandomSource random) {
        Vec3 forward = normalOr(heading, new Vec3(0, 1, 0));
        Vec3 right = perpendicular(forward);
        Vec3 up = forward.cross(right).normalize();

        Vec3[] out = new Vec3[Math.max(0, count)];
        for (int i = 0; i < out.length; i++) {
            float roll = random.nextFloat();
            float angle = spreadDegrees * roll * roll * Mth.DEG_TO_RAD;
            float around = random.nextFloat() * Mth.TWO_PI;

            double sin = Mth.sin(angle);
            out[i] = forward.scale(Mth.cos(angle))
                    .add(right.scale(Mth.cos(around) * sin))
                    .add(up.scale(Mth.sin(around) * sin))
                    .normalize();
        }
        return out;
    }

    public static Vec3[] ring(Vec3 heading, double radius, int count, RandomSource random) {
        Vec3 forward = normalOr(heading, new Vec3(0, 1, 0));
        Vec3 right = perpendicular(forward);
        Vec3 up = forward.cross(right).normalize();

        float start = random.nextFloat() * Mth.TWO_PI;
        Vec3[] out = new Vec3[Math.max(0, count)];
        for (int i = 0; i < out.length; i++) {
            float angle = start + Mth.TWO_PI * i / out.length;
            out[i] = right.scale(Mth.cos(angle) * radius).add(up.scale(Mth.sin(angle) * radius));
        }
        return out;
    }

    public static Vec3[] arc(Vec3 heading, Vec3 axis, float sweepDegrees, int count) {
        Vec3 forward = normalOr(heading, new Vec3(0, 0, 1));
        Vec3 spin = normalOr(axis, new Vec3(0, 1, 0));

        Vec3[] out = new Vec3[Math.max(0, count)];
        for (int i = 0; i < out.length; i++) {
            float through = out.length == 1 ? 0.5F : i / (float) (out.length - 1);
            float angle = (through - 0.5F) * sweepDegrees * Mth.DEG_TO_RAD;
            out[i] = rotateAbout(forward, spin, angle);
        }
        return out;
    }

    public static float easeOut(float through) {
        float t = Mth.clamp(through, 0F, 1F);
        return t < 0.5F ? 2F * t * t : 1F - (float) Math.pow(-2F * t + 2F, 2) / 2F;
    }

    public static Vec3 perpendicular(Vec3 of) {
        Vec3 axis = Math.abs(of.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 crossed = of.cross(axis);
        return crossed.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : crossed.normalize();
    }

    private static Vec3 rotateAbout(Vec3 vector, Vec3 axis, float radians) {
        double cos = Mth.cos(radians);
        double sin = Mth.sin(radians);
        return vector.scale(cos)
                .add(axis.cross(vector).scale(sin))
                .add(axis.scale(axis.dot(vector) * (1.0 - cos)));
    }

    private static Vec3 normalOr(Vec3 vector, Vec3 fallback) {
        return vector == null || vector.lengthSqr() < 1.0E-8 ? fallback : vector.normalize();
    }
}
