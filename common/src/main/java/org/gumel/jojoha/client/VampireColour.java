package org.gumel.jojoha.client;

import net.minecraft.util.Mth;

/**
 * The colour of the world while somebody is being turned, and the moment it comes back.
 *
 * <p>Three beats. It floods red as the mask wakes; the red then bleeds out into grey, which is the
 * world seen by something that has stopped being alive in the ordinary way; and then, all at once,
 * it lets go. The last beat is the one that matters - a grade that faded back would be a transition
 * ending, whereas a grade that snaps reads as the world rushing back in.
 *
 * <p>The grey is free: {@code JojohaDrain} already exists and already desaturates every terrain,
 * entity and sky shader in the game, because the time stop needed exactly that. Feeding this into
 * the same uniform means no shader was touched to add a second effect that wants the same thing -
 * see {@link TerrainCurve#drain()}, which now answers for both.
 *
 * <p>The red is drawn over the top instead, as a screen wash. Desaturating is something only a
 * shader can do; tinting is not, and adding a second uniform to eleven shader files to do what one
 * quad does would be paying a lot for the privilege.
 */
public final class VampireColour {
    /** How long each beat runs, in ticks. */
    private static final float FLOOD_TICKS = 24F;
    private static final float BLEED_TICKS = 46F;

    /**
     * How long the grey holds before it breaks.
     *
     * <p>Set against the transformation it belongs to rather than to taste: the break has to land on
     * the tick the ritual finishes, so this plus the two beats above is the whole of it.
     */
    private static final float HOLD_TICKS = 80F;

    /** How red it gets at the peak, and how much of the colour the world loses. */
    private static final float MAX_RED = 0.55F;
    private static final float MAX_GREY = 0.9F;

    /** When it started, or negative infinity for not running. */
    private static float startTick = Float.NEGATIVE_INFINITY;

    private VampireColour() {
    }

    public static void begin(float clientTimeTicks) {
        startTick = clientTimeTicks;
    }

    /**
     * The world comes back.
     *
     * <p>Called rather than waited for, so the grade ends on the same tick the ritual does instead
     * of on a length this file guessed at.
     */
    public static void release() {
        startTick = Float.NEGATIVE_INFINITY;
    }

    public static void clear() {
        release();
    }

    /** How much colour the world has lost, 0 to 1. Read by TerrainCurve for the shaders. */
    public static float grey(float clientTimeTicks) {
        float elapsed = elapsed(clientTimeTicks);
        if (elapsed < 0F) {
            return 0F;
        }

        // Comes up as the red goes down, so the two cross rather than following one another.
        float into = Mth.clamp((elapsed - FLOOD_TICKS) / BLEED_TICKS, 0F, 1F);
        return into * MAX_GREY;
    }

    /** How red the screen is, 0 to 1. Read by the overlay. */
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

    /**
     * Whether the grade should have ended by now on its own.
     *
     * <p>A backstop rather than the intended ending. The ritual calls {@link #release()} when it
     * finishes; this covers the case where that packet never arrives - a relog mid-turn, say - so a
     * player cannot be left permanently grey.
     */
    public static boolean expired(float clientTimeTicks) {
        float elapsed = elapsed(clientTimeTicks);
        return elapsed >= 0F && elapsed > FLOOD_TICKS + BLEED_TICKS + HOLD_TICKS;
    }

    private static float elapsed(float clientTimeTicks) {
        return startTick == Float.NEGATIVE_INFINITY ? -1F : clientTimeTicks - startTick;
    }
}
