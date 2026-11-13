package org.gumel.jojoha.client;

import net.minecraft.Util;
import net.minecraft.util.Mth;

public final class TimeStopCharge {
    private static final int DISPEL_TICKS = 7;

    private static final float SHAKE_DEGREES = 0.34F;

    private static int ticks;
    private static int max;
    private static int stopTicks;
    private static int dispel;

    private static float releasedCharge;

    private TimeStopCharge() {
    }

    public static void hold(int heldTicks, int maxTicks, int stopLength) {
        ticks = heldTicks;
        max = Math.max(maxTicks, 1);
        stopTicks = stopLength;
        dispel = 0;
    }

    public static void release() {
        if (ticks > 0) {
            dispel = DISPEL_TICKS;
            releasedCharge = charge();
        }
        ticks = 0;
    }

    public static void clear() {
        ticks = 0;
        dispel = 0;
        releasedCharge = 0F;
    }

    public static void tick() {
        if (dispel > 0) {
            dispel--;
        }
    }

    public static int heldTicks() {
        return ticks;
    }

    public static int maxTicks() {
        return max;
    }

    public static int stopTicks() {
        return stopTicks;
    }

    public static float charge() {
        return ticks <= 0 ? 0F : Mth.clamp(ticks / (float) max, 0F, 1F);
    }

    public static boolean charging() {
        return ticks > 0;
    }

    public static boolean dispelling() {
        return dispel > 0;
    }

    public static float dispelFade() {
        return dispel <= 0 ? 0F : dispel / (float) DISPEL_TICKS;
    }

    public static float releasedCharge() {
        return releasedCharge;
    }

    public static float[] cameraShake() {
        float charge = charge();
        if (charge <= 0.01F) {
            return null;
        }

        float seconds = (Util.getMillis() % 1000000L) / 1000F;
        float intensity = charge * charge * SHAKE_DEGREES;

        return new float[] {
                Mth.sin(seconds * 47.3F) * intensity,
                Mth.sin(seconds * 61.7F) * intensity,
        };
    }
}
