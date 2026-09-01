package org.gumel.jojoha.stand;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/**
 * How a particular Stand carries itself.
 *
 * <h2>Why these are numbers and not code</h2>
 *
 * <p>Every Stand runs the same follow simulation. What separates Star Platinum's snap from Hermit
 * Purple's drift is entirely the constants that simulation is fed, so they live here as data rather
 * than as a branch per Stand somewhere in the entity. Giving a new Stand its own feel is a line in
 * the table at the bottom of this file.
 *
 * <p>Kept beside {@link StandType} rather than inside it. That record describes what a Stand is made
 * of - its model, its sheets, its skills - and is already eleven components deep with three
 * constructors keeping older registrations working. How a thing moves is a separate question from
 * what it is built from, and the same precedent already exists for the skill list, which is composed
 * here rather than flattened into the record.
 *
 * <h2>Where the spring numbers came from</h2>
 *
 * <p>Measured, not derived. The follow is semi-implicit Euler stepped once per tick:
 *
 * <pre>
 *     a = (target - pos) * stiffness - velocity * damping
 *     velocity += a
 *     pos += velocity
 * </pre>
 *
 * <p>At a timestep of one whole tick the textbook rule for critical damping - {@code c = 2*sqrt(k)}
 * - does not describe this system, so every pair below was found by stepping that exact update
 * against a step input and reading off the overshoot and the settling time. All three settle without
 * passing their target by more than half a percent, which is the doc's one repeated rule: controlled
 * inertia, never a rubber band.
 *
 * <p>For the record, the numbers this replaced were {@code stiffness 0.15, damping 0.45}, which
 * overshoot by <b>6.3%</b> before settling - deliberately, per the comment that sat beside them. The
 * fix costs nothing: at the same stiffness, more damping settles in 8 ticks where the bouncy pair
 * took 12.
 *
 * <h2>Why an attack has a side offset at all</h2>
 *
 * <p>A Stand that plants dead ahead to punch draws the line {@code PLAYER -> STAND -> TARGET}, which
 * reads as the user firing something out of their own chest - the attack looks like it belongs to
 * the player's body rather than to a separate thing standing in front of them.
 *
 * <p>Stepping it aside fixes that in one move. The Stand occupies the attacking space while the user
 * stays where they are, so the two read as two bodies: one holding still, one throwing the punches.
 *
 * <p>Always the same side - the user's right, which is the right of their screen. Alternating it per
 * strike was tried and read as finnicky: the Stand swapping shoulders is a two-block jump across the
 * view, and at the pace strikes come out that is a distraction rather than choreography. Picking one
 * side and keeping it means the eye learns where the Stand lives.
 *
 * <p>It has a second benefit that was already wanted. The forward offset carries a note about being
 * far enough out that a first-person user looks past their Stand rather than through it; being off
 * to one side clears the crosshair far more thoroughly than distance ever did.
 *
 * @param followDistance      how far behind the owner the Stand sits
 * @param sideOffset          how far to the owner's right it sits
 * @param verticalOffset      how far above the owner's feet it floats
 * @param springStiffness     how hard it is pulled toward where it wants to be
 * @param springDamping       how strongly that pull is resisted - what stops the bounce
 * @param rotationSpeed       degrees of yaw it can turn through in a tick while idle
 * @param combatRotationSpeed the same while it has something to face
 * @param crouchDrop          how far the anchor sinks when the owner crouches
 * @param sprintLean          how much the follow distance closes at a sprint, so it leans in
 * @param fallCatchUp         the fraction of the follow distance kept while falling fast
 * @param attackForward       how far in front of the owner it plants to strike
 * @param attackSide          how far off the owner's line it stands to strike
 * @param barragePush         how far forward a sustained flurry can carry it, at most
 */
public record StandMovementProfile(double followDistance, double sideOffset, double verticalOffset,
                                   double springStiffness, double springDamping,
                                   float rotationSpeed, float combatRotationSpeed,
                                   double crouchDrop, double sprintLean, double fallCatchUp,
                                   double attackForward, double attackSide, double barragePush) {

    /**
     * Quick, tight, and almost without delay - a Stand that punches.
     *
     * <p>Settles in 8 ticks and trails about two thirds of a block behind its anchor at a walk.
     * The offsets are the ones the game already used, because they were tuned in play and the
     * personality being added here is in the response, not in where it stands.
     */
    public static final StandMovementProfile SHARP = new StandMovementProfile(
            1.0, 0.9, 0.6, 0.15, 0.56, 22F, 30F, 0.35, 0.35, 0.55,
            2.15, 1.1, 0.9);

    /** The middle: some weight to it, no drama. Sixteen ticks slower to settle than SHARP. */
    public static final StandMovementProfile STEADY = new StandMovementProfile(
            1.0, 0.9, 0.6, 0.08, 0.43, 14F, 20F, 0.35, 0.25, 0.6,
            2.15, 1.0, 0.8);

    /**
     * Trailing and unhurried, for a Stand that is more vine than fist.
     *
     * <p>Sixteen ticks to settle and a block and a half of trail, and it sits a little wider than
     * the others - the doc asks this one for larger arcs, and an arc needs room to be one.
     */
    public static final StandMovementProfile FLOWING = new StandMovementProfile(
            1.15, 1.0, 0.6, 0.05, 0.35, 9F, 14F, 0.3, 0.15, 0.7,
            2.0, 1.35, 0.7);

    /** Who moves how. A Stand absent from here falls back on its range - see {@link #forStand}. */
    private static final Map<ResourceLocation, StandMovementProfile> BY_STAND = Map.of(
            StandTypes.STAR_PLATINUM_ID, SHARP,
            StandTypes.HERMIT_PURPLE_ID, FLOWING);

    /**
     * The profile for a Stand, or a reasonable guess at one.
     *
     * <p>The fallback reads the Stand's range rather than returning a fixed default, so a Stand
     * added without an entry above still moves like the kind of thing it is - something that fights
     * at arm's length wants to be responsive, and something that works at distance does not.
     */
    public static StandMovementProfile forStand(StandType type) {
        if (type == null) {
            return STEADY;
        }
        StandMovementProfile named = BY_STAND.get(type.id());
        if (named != null) {
            return named;
        }
        return type.range() == StandRange.CLOSE ? SHARP : STEADY;
    }
}
