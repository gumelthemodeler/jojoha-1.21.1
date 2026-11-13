package org.gumel.jojoha.client.fx;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class NearFade {
    public static final double FADE_FROM = 3.5;

    public static final double FADE_TO = 0.9;

    public static final float FLOOR = 0.25F;

    private NearFade() {
    }

    public static float kept(double distance) {
        if (distance >= FADE_FROM) {
            return 1F;
        }
        float t = (float) Mth.clamp((distance - FADE_TO) / (FADE_FROM - FADE_TO), 0.0, 1.0);
        float eased = t * t * (3F - 2F * t);
        return Mth.lerp(eased, FLOOR, 1F);
    }

    public static float kept(Vec3 eye, double x, double y, double z) {
        double dx = x - eye.x;
        double dy = y - eye.y;
        double dz = z - eye.z;
        return kept(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }

    public static int apply(int argb, Vec3 eye, Vec3 at) {
        float kept = kept(eye, at.x, at.y, at.z);
        if (kept >= 1F) {
            return argb;
        }
        int a = Math.round(((argb >>> 24) & 0xFF) * kept);
        return a << 24 | (argb & 0xFFFFFF);
    }

    public interface Marked {
        void jojoha$fadeNear();
    }
}
