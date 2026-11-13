package org.gumel.jojoha.hamon;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class BubbleSheet {
    public static final float ARC_DEGREES = 70F;
    public static final double RADIUS = 1.3;
    public static final double HALF_HEIGHT = 0.8;
    public static final double BILLOW = 0.25;
    public static final double BELOW_EYE = 0.4;
    public static final double CLOSED_WIDTH = 0.3;
    public static final double CLOSED_REACH = 0.4;
    public static final double CLOSED_HEIGHT = 0.22;
    public static final double TAPER = 0.15;
    public static final double SLOT_HEIGHT = 0.82;

    public record Slot(double theta, double v, double appear) {
        public double u(float arcDegrees) {
            return theta / (arcDegrees / 2.0);
        }
    }

    public record Frame(Vec3 origin, Vec3 look, Vec3 across, Vec3 up) {
        public static Frame of(Vec3 eye, Vec3 look, float yaw) {
            Vec3 forward = look.normalize();
            Vec3 across = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
            Vec3 up = across.cross(forward).normalize();
            if (up.y < 0.0) {
                up = up.reverse();
            }
            return new Frame(eye.subtract(0.0, BELOW_EYE, 0.0), forward, across, up);
        }

        public Vec3 point(double u, double v, double open, double wobble) {
            double width = Mth.lerp(open, CLOSED_WIDTH, 1.0);
            double theta = Math.toRadians(u * ARC_DEGREES / 2.0 * width);
            double reach = RADIUS * Mth.lerp(open, CLOSED_REACH, 1.0)
                    + BILLOW * open * (1.0 - u * u) * (1.0 - v * v) + wobble;
            double height = HALF_HEIGHT * Mth.lerp(open, CLOSED_HEIGHT, 1.0) * (1.0 - TAPER * u * u);
            Vec3 way = look.scale(Math.cos(theta)).add(across.scale(Math.sin(theta)));
            return origin.add(way.scale(reach)).add(up.scale(v * height));
        }
    }

    private BubbleSheet() {
    }

    public static List<Slot> slots(long seed, int count, float arcDegrees) {
        RandomSource layout = RandomSource.create(seed);
        double spacing = arcDegrees / count;
        List<Slot> slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double theta = -arcDegrees / 2.0 + spacing * (i + 0.5) + (layout.nextDouble() - 0.5) * spacing * 0.3;
            double v = (layout.nextDouble() * 2.0 - 1.0) * SLOT_HEIGHT;
            double appear = 0.2 + layout.nextDouble() * 0.6;
            slots.add(new Slot(theta, v, appear));
        }
        return slots;
    }
}
