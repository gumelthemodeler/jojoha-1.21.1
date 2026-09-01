package org.gumel.jojoha.stand;

/**
 * How a summoned Stand picks its fights - the standing order its user has given it.
 *
 * <p>Distinct from {@link TrustTier}, which is about what a Stand is <em>capable</em> of. The mode
 * is a moment-to-moment choice the user makes and can flip at will.
 */
public enum StandMode {
    /**
     * Analog: nothing happens unless you make it happen.
     *
     * <p>Every strike, block and move is a direct instruction - the Stand trails you and waits. It
     * is deliberately first in this enum and stays there: the ordinal is what goes to disk and over
     * the wire, so reordering these would silently turn every saved DEFENSE into something else.
     */
    ANALOG,

    /**
     * Off the leash: the Stand roams for hostile mobs near its user and engages them on its own,
     * moving to the next once one drops. Nothing needs pointing at.
     */
    DEFENSE,

    /**
     * Hands rather than fists: the Stand is put to work on the world instead of on what is in it.
     *
     * <p>This is the mode that lets a Stand use the item you are holding, from where it is standing
     * - see {@link StandHands}. Water goes into a hole you are nowhere near, a block is set on the
     * far side of a gap, a pearl leaves from two blocks further out than your own arm could throw
     * it.
     *
     * <p>A mode rather than something always available, for two reasons. The first is that a Stand
     * reaching for the world and a Stand reaching for a throat are different intents and should not
     * share a button by accident. The second is that it is a real trade: the stance is what the HUD
     * is announcing with those eyes, so a player in Utility is telling everyone - themselves
     * included - that their Stand is currently busy being useful rather than watchful.
     *
     * <p>Appended rather than inserted. The ordinal is persisted, so a new value may only ever go
     * on the end; putting it anywhere else would reinterpret every Stand already saved to disk.
     */
    UTILITY;

    private static final StandMode[] VALUES = values();

    public StandMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public static StandMode fromOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : ANALOG;
    }

    /** Whether the Stand hunts on its own rather than waiting to be aimed. */
    public boolean isAutonomous() {
        return this == DEFENSE;
    }

    /**
     * Whether pointing at something and pressing the stance key sends the Stand at it.
     *
     * <p>Only Analog. DEFENSE picks its own targets and does not need pointing at; UTILITY is not
     * looking for a fight at all, and having it lunge at the nearest mob because one happened to be
     * in the crosshair when the stance key was pressed would make the stance impossible to leave
     * without first finding somewhere empty to look.
     */
    public boolean takesAimedOrders() {
        return this == ANALOG;
    }

    /** Whether the Stand will use its user's items for them - see {@link StandHands}. */
    public boolean handlesItems() {
        return this == UTILITY;
    }

    /** Translation key for the action-bar announcement and anywhere else the stance is named. */
    public String translationKey() {
        return switch (this) {
            case DEFENSE -> "hud.jojoha.stand_mode.defense";
            case UTILITY -> "hud.jojoha.stand_mode.utility";
            default -> "hud.jojoha.stand_mode.analog";
        };
    }
}
