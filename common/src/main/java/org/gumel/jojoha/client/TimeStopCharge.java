package org.gumel.jojoha.client;

import net.minecraft.Util;
import net.minecraft.util.Mth;

/**
 * How far the time stop has been wound up while the key is held, client-side.
 *
 * <p>One number that the motes, the Stand's glow and the input all read, rather than each of them
 * working it out from the keyboard themselves. That matters more than it looks: the motes speed up,
 * the glow whitens and the release fires from the same value, so they cannot disagree about how
 * charged the move is - and the whole point of showing the charge is that what you see is what you
 * will get when you let go.
 *
 * <p>Purely a display of the client's own key state. The server is told how long the key was held
 * when it is released and clamps that against the move's real maximum, so nothing here is trusted
 * for anything but drawing.
 */
public final class TimeStopCharge {
    /** How long the burst lasts after release, in ticks. */
    private static final int DISPEL_TICKS = 7;

    /** How hard the camera trembles at full charge, in degrees. */
    private static final float SHAKE_DEGREES = 0.34F;

    private static int ticks;
    private static int max;
    private static int dispel;

    /**
     * How charged the move was on the tick it fired.
     *
     * <p>Kept because {@link #charge()} is zero the instant the key comes up, and the bar wants to
     * fade out at the length it reached rather than snapping shut first - what should be left on
     * screen is how hard the stop you just threw actually was.
     */
    private static float releasedCharge;

    private TimeStopCharge() {
    }

    /** Called every tick the key is down. */
    public static void hold(int heldTicks, int maxTicks) {
        ticks = heldTicks;
        max = Math.max(maxTicks, 1);
        dispel = 0;
    }

    /** Called on the tick the key comes up. */
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

    /** Call once per client tick, after the input has been read. */
    public static void tick() {
        if (dispel > 0) {
            dispel--;
        }
    }

    /** How many ticks the key has been down, which is what the move is priced in. */
    public static int heldTicks() {
        return ticks;
    }

    /** The full hold, in ticks - what {@link #heldTicks()} is counting toward. */
    public static int maxTicks() {
        return max;
    }

    /** 0 to 1 through the wind-up. */
    public static float charge() {
        return ticks <= 0 ? 0F : Mth.clamp(ticks / (float) max, 0F, 1F);
    }

    public static boolean charging() {
        return ticks > 0;
    }

    /** True for the few ticks after release, while the gathered motes are thrown off. */
    public static boolean dispelling() {
        return dispel > 0;
    }

    /** 1 down to 0 across that window - what the charge bar fades out on. */
    public static float dispelFade() {
        return dispel <= 0 ? 0F : dispel / (float) DISPEL_TICKS;
    }

    /** How full the bar was when the key came up. See the field for why it is kept. */
    public static float releasedCharge() {
        return releasedCharge;
    }

    /**
     * The camera tremble while the move is being held, or null when nothing is charging.
     *
     * <p>Scaled by the square of the charge rather than the charge itself, so almost all of it lands
     * in the last half of the hold. A tremble that grows evenly from the first tick reads as the
     * game stuttering; one that is imperceptible at first and unmistakable by the end reads as
     * something being held that does not want to be.
     *
     * <p>Off the wall clock, not the game clock, so it keeps trembling at the same rate whatever is
     * happening to the tick rate around it.
     */
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
