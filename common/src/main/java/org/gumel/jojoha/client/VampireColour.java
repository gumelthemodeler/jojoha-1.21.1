package org.gumel.jojoha.client;

import net.minecraft.util.Mth;

public final class VampireColour {
    private static final float FLOOD_TICKS = 24F;
    private static final float BLEED_TICKS = 46F;

    private static final float HOLD_TICKS = 80F;

    private static final float MAX_RED = 0.55F;
    private static final float MAX_GREY = 0.9F;

    private static float startTick = Float.NEGATIVE_INFINITY;

    private VampireColour() {
    }

    public static void begin(float clientTimeTicks) {
        startTick = clientTimeTicks;
    }

    public static void release() {
        startTick = Float.NEGATIVE_INFINITY;
    }

    public static void clear() {
        release();
    }

    public static float grey(float clientTimeTicks) {
        float elapsed = elapsed(clientTimeTicks);
        if (elapsed < 0F) {
            return 0F;
        }

        float into = Mth.clamp((elapsed - FLOOD_TICKS) / BLEED_TICKS, 0F, 1F);
        return into * MAX_GREY;
    }

    public static float red(float clientTimeTicks) {
        float elapsed = elapsed(clientTimeTicks);
        if (elapsed < 0F) {
            return 0F;
        }

        if (elapsed < FLOOD_TICKS) {
            return (elapsed / FLOOD_TICKS) * MAX_RED;
        }

        float out = Mth.clamp(1F - (elapsed - FLOOD_TICKS) / BLEED_TICKS, 0F, 1F);
        return out * MAX_RED;
    }

    public static boolean expired(float clientTimeTicks) {
        float elapsed = elapsed(clientTimeTicks);
        return elapsed >= 0F && elapsed > FLOOD_TICKS + BLEED_TICKS + HOLD_TICKS;
    }

    private static float elapsed(float clientTimeTicks) {
        return startTick == Float.NEGATIVE_INFINITY ? -1F : clientTimeTicks - startTick;
    }
}
