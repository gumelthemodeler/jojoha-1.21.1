package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.gumel.jojoha.data.ClientPlayerDataCache;

/**
 * The client-side reactions to a time stop starting: the clock, and the jolt of the cast.
 *
 * <p>Both are edge-triggered off the synced counters rather than fired by a packet. The counters
 * are already there, they already survive a relog mid-stop, and a packet that goes missing would
 * leave a clock running over a world that had started moving again.
 */
public final class TimeStopClient {
    /** How hard the cast rattles the camera, and how long the jolt lasts. */
    private static final float SHAKE_DEGREES = 2.2F;
    private static final int SHAKE_TICKS = 14;

    private static boolean wasCasting;
    private static boolean wasStopped;
    private static float shakeStartTick = Float.NEGATIVE_INFINITY;

    private TimeStopClient() {
    }

    public static void clear() {
        wasCasting = false;
        wasStopped = false;
        shakeStartTick = Float.NEGATIVE_INFINITY;
    }

    /** Call once per client tick, after the data cache has been updated. */
    public static void tick(float clientTimeTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            clear();
            return;
        }

        boolean casting = ClientPlayerDataCache.data.isCastingTimeStop();

        // The stop anyone can see, not only one this player is inside the data for. The clock is
        // part of the world's state during a stop and everybody in it should hear the same thing.
        boolean stopped = TimeStopView.active();

        // The jolt lands on the cast, not on the freeze - it is the Stand throwing its hand out
        // that hits, and by the time the world actually stops the moment has already passed.
        if (casting && !wasCasting) {
            shakeStartTick = clientTimeTicks;
        }

        if (stopped) {
            // Started once, on the edge, and left to run. It used to be beaten out here one strike
            // at a time so the rate could be driven; the rate is gone and so is all of that - see
            // TimeStopClockSound for why a recording is the only way the spacing can be right.
            if (!wasStopped) {
                minecraft.getSoundManager().play(new TimeStopClockSound());
            }

            // The one number the fold, the colour and the sound all read from. Everything being
            // driven off the same progress is the whole point - previously the ground ran on its
            // own timeline and the clock on another, and no amount of tuning either one separately
            // was going to make them agree.
            TerrainCurve.setStopProgress(TimeStopView.progress());
        }

        wasCasting = casting;
        wasStopped = stopped;
    }

    /**
     * The camera offset for the cast, or null when nothing is shaking.
     *
     * <p>Same shape as the awakening's jolt: quadratic falloff so it hits hard and dies quickly,
     * and two frequencies that do not divide into each other so the motion never settles into a
     * readable wobble.
     */
    public static float[] cameraShake(float clientTimeTicks) {
        float elapsed = clientTimeTicks - shakeStartTick;
        if (elapsed < 0F || elapsed > SHAKE_TICKS) {
            return null;
        }

        float falloff = 1F - (elapsed / SHAKE_TICKS);
        float intensity = falloff * falloff * SHAKE_DEGREES;

        return new float[] {
                Mth.sin(elapsed * 3.1F) * intensity,
                Mth.cos(elapsed * 4.7F) * intensity,
        };
    }
}
