package org.gumel.jojoha.stand;

/**
 * What the Stand is doing with itself, as one name.
 *
 * <h2>Read, not stored</h2>
 *
 * <p>Deliberately derived from the flags the entity already keeps rather than being a field that
 * something has to remember to set. A second copy of the truth is a second thing to get wrong, and
 * the entity has a dozen callers that start and stop these behaviours - every one of them would have
 * to keep the enum in step. Reading it means the name can never disagree with the behaviour.
 *
 * <p>{@link StandEntity#movementState()} resolves it in priority order, which is the same order the
 * entity's own tick already runs its branches in: a major action wins over an attack, an attack wins
 * over standing ready, and standing ready wins over following. The enum does not enforce that
 * ordering - it names it, so that the ordering can be discussed and tested rather than only existing
 * as the shape of an if-chain.
 *
 * <p>The doc this comes from separates GRAB, HOLD and THROW. Only HOLDING is here: the other two are
 * moments rather than states - a grab is the tick a hold begins and a throw is the tick it ends, and
 * neither is ever something the Stand is still doing when you ask.
 */
public enum StandMovementState {

    /** Manifesting, or still dissolving. The Stand is not under movement control yet. */
    SUMMON,

    /** Being flown directly by its user. Their input is the position. */
    PILOTED,

    /** Holding a breath at a fixed spot, anchored to the world rather than to its user. */
    WORKING,

    /**
     * Away from its user on business of its own - hunting something down, or sent out to a job.
     *
     * <p>The two are one state here because only their combination is synced, and telling them apart
     * would mean this enum meant different things on the client and the server.
     */
    PURSUING,

    /** Planted across its user's front, holding a blow off them. */
    GUARDING,

    /** Carrying something in its hand. The held thing rides the Stand - see StandGrip. */
    HOLDING,

    /** Mid-flurry. The one state that moves the Stand itself rather than only its anchor. */
    BARRAGE,

    /** Throwing a single strike, out at full reach. */
    ATTACK,

    /** Recently fought and still squared up, waiting beside its user rather than drifting back. */
    COMBAT,

    /** Keeping up with a user who is moving. */
    FOLLOW,

    /** Nothing is happening. */
    IDLE;

    /** Whether the Stand is holding a fighting position rather than an ordinary following one. */
    public boolean isCombatStance() {
        return this == BARRAGE || this == ATTACK || this == COMBAT || this == GUARDING
                || this == HOLDING;
    }
}
