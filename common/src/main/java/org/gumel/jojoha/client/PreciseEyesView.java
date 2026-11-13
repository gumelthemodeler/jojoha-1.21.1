package org.gumel.jojoha.client;

import net.minecraft.util.Mth;

public final class PreciseEyesView {
    private static final float ZOOM = 0.25F;
    private static final float EASE = 0.4F;
    private static final float SNAP = 0.002F;

    private static boolean on;
    private static float scale = 1F;
    private static float previous = 1F;

    private PreciseEyesView() {
    }

    public static void set(boolean value) {
        on = value;
    }

    public static void tick() {
        previous = scale;
        float goal = on ? ZOOM : 1F;
        scale += (goal - scale) * EASE;
        if (Math.abs(goal - scale) < SNAP) {
            scale = goal;
        }
    }

    public static float fovScale(float partialTick) {
        return Mth.lerp(partialTick, previous, scale);
    }

    public static void clear() {
        on = false;
        scale = 1F;
        previous = 1F;
    }
}
