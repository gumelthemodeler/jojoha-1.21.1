package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class TimeStopClient {
    private static final float SHAKE_DEGREES = 3.2F;
    private static final int SHAKE_TICKS = 12;

    private static final int CLOCK_DELAY_TICKS = 22;

    private static boolean wasStopped;
    private static float shakeStartTick = Float.NEGATIVE_INFINITY;

    private TimeStopClient() {
    }

    public static void clear() {
        wasStopped = false;
        shakeStartTick = Float.NEGATIVE_INFINITY;
    }

    public static void tick(float clientTimeTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            clear();
            return;
        }

        boolean stopped = TimeStopView.active();

        if (stopped) {
            if (!wasStopped) {
                impact(minecraft, clientTimeTicks);
            }

            TerrainCurve.setStopProgress(TimeStopView.progress());
        }

        wasStopped = stopped;
    }

    private static void impact(Minecraft minecraft, float clientTimeTicks) {
        if (JojohaClientConfig.cameraMotion()) {
            shakeStartTick = clientTimeTicks;
        }
        if (JojohaClientConfig.impactFrames()) {
            ImpactFrame.begin(1F);
        }
        TimeStopWind.begin(TimeStopView.centre());
        int quiet = TimeStopView.quietTicks();
        minecraft.getSoundManager().playDelayed(new TimeStopClockSound(), quiet > 0 ? quiet : CLOCK_DELAY_TICKS);
    }

    public static float[] cameraShake(float clientTimeTicks) {
        float elapsed = clientTimeTicks - shakeStartTick;
        if (elapsed < 0F || elapsed > SHAKE_TICKS) {
            return null;
        }

        float falloff = 1F - (elapsed / SHAKE_TICKS);
        float intensity = falloff * falloff * falloff * SHAKE_DEGREES;

        return new float[] {
                Mth.sin(elapsed * 3.1F) * intensity,
                Mth.cos(elapsed * 4.7F) * intensity,
        };
    }
}
