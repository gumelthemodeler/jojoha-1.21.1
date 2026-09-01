package org.gumel.jojoha.stand;

/**
 * How far a Stand can operate from its user, in the sense the series uses the term.
 *
 * <p>This is not just a number - it decides which generic moves a Stand is given at all. A
 * close-range Stand fights at arm's length from its user and gets a leap to close distance; a
 * long-range one can be sent out alone, which is what makes piloting it meaningful. Handing both
 * sets to every Stand would erase the trade-off the classification exists to express.
 */
public enum StandRange {
    /** Fights beside its user. Powerful up close, useless at distance. */
    CLOSE,
    /** Can operate away from its user, and can be flown directly - see the pilot move. */
    LONG
}
