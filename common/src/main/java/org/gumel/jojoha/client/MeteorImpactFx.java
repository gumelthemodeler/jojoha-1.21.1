package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class MeteorImpactFx {
    private static final float SHAKE_TICKS = 55F;
    private static final float SHAKE_DEGREES = 7.5F;

    private static final double FELT_RANGE = 220.0;

    private static final double FULL_RANGE = 30.0;

    private static float startTick = Float.NEGATIVE_INFINITY;
    private static float strength;

    private MeteorImpactFx() {
    }

    public static void impact(double x, double y, double z, float scale) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        double distance = Math.sqrt(minecraft.player.distanceToSqr(x, y, z));
        if (distance > FELT_RANGE) {
            return;
        }

        double near = 1.0 - Math.max(0.0, distance - FULL_RANGE) / (FELT_RANGE - FULL_RANGE);
        strength = (float) Mth.clamp(near, 0.0, 1.0) * scale;
        startTick = clientTicks();
    }

    public static void clear() {
        startTick = Float.NEGATIVE_INFINITY;
        strength = 0F;
    }

    public static float[] cameraShake() {
        float elapsed = clientTicks() - startTick;
        if (elapsed < 0F || elapsed > SHAKE_TICKS || strength <= 0F) {
            return null;
        }

        float falloff = 1F - (elapsed / SHAKE_TICKS);
        float intensity = falloff * falloff * SHAKE_DEGREES * strength;

        return new float[] {
                Mth.sin(elapsed * 2.7F) * intensity,
                Mth.cos(elapsed * 4.1F) * intensity,
        };
    }

    private static float clientTicks() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0F : minecraft.level.getGameTime() % 100000L;
    }
}
