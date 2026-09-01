package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/**
 * The clock behind the black and white frame - when one was asked for, and how far through it is.
 *
 * <p>Kept apart from {@link ImpactFramePost}, which knows how to draw the thing but has no business
 * knowing why. Anything in the mod that wants the world to go monochrome for a beat calls
 * {@link #begin} and never touches the render pass.
 *
 * <p>Timed in real milliseconds rather than ticks. A hit landing is a thing the eye judges, and a
 * twenty-hertz counter shows every one of its steps in a shape this short - which is exactly the
 * complaint the drawn version earned. This one is smooth by construction at any frame rate.
 */
public final class ImpactFrame {
    /**
     * How the frame is shaped, in milliseconds.
     *
     * <p>Instant on. There is no ramp into an impact frame - the whole reason it reads as impact is
     * that the change happens between two frames with nothing in between. The ramp is all on the way
     * out, and it is most of the second: the colour bleeding back is what turns a flicker into a
     * held moment.
     */
    private static final long HOLD_MS = 260L;
    private static final long FADE_MS = 740L;

    private static long startedAt = Long.MIN_VALUE;
    private static float peak;

    private ImpactFrame() {
    }

    /**
     * Asks for a frame at the given strength, from nought to one.
     *
     * <p>A second call while one is running restarts it rather than stacking, which is what you want
     * when two hits land close together - the second is a fresh punctuation mark, not a longer one.
     */
    public static void begin(float strength) {
        startedAt = now();
        peak = Mth.clamp(strength, 0F, 1F);
    }

    /** Drops any frame in progress - on death, on disconnect, on anything that ends the scene. */
    public static void clear() {
        startedAt = Long.MIN_VALUE;
        peak = 0F;
    }

    /** How grey the world should be right now, from one down to nought. */
    public static float strength() {
        if (startedAt == Long.MIN_VALUE || Minecraft.getInstance().level == null) {
            return 0F;
        }

        long elapsed = now() - startedAt;
        if (elapsed < 0L) {
            return 0F;
        }
        if (elapsed < HOLD_MS) {
            return peak;
        }

        long fading = elapsed - HOLD_MS;
        if (fading >= FADE_MS) {
            startedAt = Long.MIN_VALUE;
            return 0F;
        }

        // Cubed on the way out. A linear fade spends its whole length in visible grey and reads as
        // the effect being slow to leave; this one is most of the way back to colour early and then
        // takes its time over the last little bit, which is the part nobody notices ending.
        float left = 1F - (float) fading / FADE_MS;
        return peak * left * left * left;
    }

    /** The monotonic clock, kept in one place so the two readings above cannot disagree. */
    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }
}
