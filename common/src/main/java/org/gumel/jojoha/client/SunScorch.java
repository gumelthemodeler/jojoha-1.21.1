package org.gumel.jojoha.client;

import net.minecraft.util.Mth;

public final class SunScorch {
    private static final float RISE = 0.22F;
    private static final float FALL = 0.06F;

    private static float target;

    private static float shown;

    private SunScorch() {
    }

    public static void set(float exposure) {
        target = Mth.clamp(exposure, 0F, 1F);
    }

    public static void tick() {
        float gap = target - shown;
        shown += gap * (gap > 0F ? RISE : FALL);
        if (Math.abs(target - shown) < 0.001F) {
            shown = target;
        }
    }

    public static float exposure() {
        return shown;
    }

    public static void clear() {
        target = 0F;
        shown = 0F;
    }
}
