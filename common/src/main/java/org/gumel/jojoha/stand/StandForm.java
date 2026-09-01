package org.gumel.jojoha.stand;

/**
 * What shape a Stand actually is.
 *
 * <p>The pipeline was written around Star Platinum and quietly assumed every Stand would be like it:
 * a person-shaped figure that stands beside its user, walks, has a head to turn and legs to swing.
 * Most of the famous ones are, and none of that is wrong - it is just not the whole set. Hermit
 * Purple is a handful of thorned vines coming off Joseph's arms. It has no legs to swing, no head to
 * turn and nowhere to stand.
 *
 * <p>Rather than special-casing that one Stand, this says what kind of thing a Stand is and lets the
 * renderer and the entity ask. The alternative - a flag on Hermit Purple that suppresses the walk
 * cycle - solves the same problem once and leaves the next non-humanoid to solve it again.
 *
 * <h2>What actually differs</h2>
 *
 * <p>Less than you would expect, because the model code was already written defensively: every bone
 * lookup null-checks, so a model without a {@code Head} simply has no head posed rather than
 * throwing. What is left is the behaviour that is wrong rather than absent - a walk cycle applied to
 * arms that are not legs, and a follow spring trailing a body that is supposed to be attached to
 * somebody.
 */
public enum StandForm {
    /**
     * A figure. Stands beside its user, follows on the spring, walks when it moves.
     *
     * <p>Star Platinum, The World, and most of what a Stand is usually drawn as.
     */
    HUMANOID,

    /**
     * Part of its user rather than a separate body.
     *
     * <p>Vines, chains, threads - anything that emerges from the person and never leaves them. It is
     * drawn on them rather than near them, so there is no gap for a follow spring to close and no
     * gait to animate: it goes exactly where they go because it is attached to them.
     *
     * <p>Its reach is whatever it throws, which is the interesting consequence. A humanoid Stand has
     * a range because the body can only get so far from its user; a bound one has a range because
     * the thing it extends only goes so far, and that is a property of the move rather than of the
     * Stand.
     */
    BOUND;

    /** Whether this Stand has a body that moves about under its own steam. */
    public boolean isFreeStanding() {
        return this == HUMANOID;
    }

    /** Whether it should be pinned to its user rather than following them. */
    public boolean isBound() {
        return this == BOUND;
    }
}
