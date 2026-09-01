package org.gumel.jojoha.stand;

import net.minecraft.core.Direction;

/**
 * What shape a Utility placement takes.
 *
 * <p>Chosen, not guessed. The run used to work its axis out from wherever the crosshair happened to
 * be pointing, which reads fine in a description and badly in the hand: the axis flipped at a
 * threshold the player could not see, so a row became a column halfway through lining it up and
 * nothing on screen explained why. Every attempt to make that inference smarter moved the surprise
 * rather than removing it.
 *
 * <p>So the player says. The mode is picked from the slot bar - Utility swaps the combat moveset out
 * for these, since a Stand laying blocks has no use for a barrage - and from then on the aim decides
 * only <em>how far</em>, never which way. Which is the one thing an inference could never get wrong,
 * because it is no longer making one.
 */
public enum BuildMode {
    /**
     * One block, where you are pointing. The default, and deliberately first: the ordinal is what
     * goes to disk, and it is also what a player who has never opened the bar should get.
     */
    SINGLE,

    /**
     * A horizontal run, along whichever of X or Z the stretch is mostly going.
     *
     * <p>Left as "whichever", rather than splitting into two modes, because the player is already
     * telling it which one by dragging - and a bar with separate north-south and east-west tools
     * would be asking them to name a compass direction they are looking straight down.
     */
    ROW,

    /** A vertical run. Aim decides how high; nothing decides which way but this. */
    COLUMN,

    /**
     * The old behaviour, kept as a choice rather than deleted.
     *
     * <p>Inference is genuinely the fastest thing to use once you know where it switches, and
     * somebody who has learned it should not have to give it up. It is simply no longer what you
     * get without asking.
     */
    FREE;

    private static final BuildMode[] VALUES = values();

    public static BuildMode fromOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : SINGLE;
    }

    /** Whether this mode runs at all, or places exactly one block. */
    public boolean stretches() {
        return this != SINGLE;
    }

    /**
     * The axis this run must follow, given the way it is being dragged, or null to let the
     * displacement decide.
     *
     * @param dx horizontal displacement from the anchor, on X
     * @param dy vertical displacement
     * @param dz horizontal displacement from the anchor, on Z
     */
    public Direction.Axis axis(int dx, int dy, int dz) {
        return switch (this) {
            case COLUMN -> Direction.Axis.Y;
            case ROW -> Math.abs(dx) >= Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
            default -> null;
        };
    }

    public String translationKey() {
        return "buildmode.jojoha." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
