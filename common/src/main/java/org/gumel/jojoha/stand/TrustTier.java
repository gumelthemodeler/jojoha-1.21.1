package org.gumel.jojoha.stand;

import net.minecraft.util.Mth;

/**
 * How far a user and their Stand have bonded, per the design doc's Trust Tiers. The tier gates
 * what the Stand can physically do, not just how strong it is - see each constant.
 *
 * <p>Stored as the raw ordinal in {@link org.gumel.jojoha.data.StandData#trustTier()} (so saves
 * and the sync packet stay plain ints) and mirrored onto the summoned entity as synced entity
 * data, since the client renderer needs it to decide partial-vs-full manifestation.
 */
public enum TrustTier {
    /** The Stand can be cast - its aura answers - but it never takes form. Passive effects only. */
    DORMANT,
    /**
     * Up to two arms manifest as reinforcement, cast around the user's own body. They guard, but
     * they don't throw punches - at this tier your M1 is still your own fist.
     */
    PARTIAL,
    /** Full manifestation, but unstable - it flickers translucently and expires on its own after a short window. */
    EMERGING,
    /** Full trust: the complete moveset and a stable manifestation with no time limit. Requiems become possible here. */
    BONDED;

    /**
     * How much Stand energy the user can hold at this tier.
     *
     * <p>Trust is the whole progression, so the pool grows with it rather than staying flat and
     * being spent faster. A Bonded Stand holds two and a half times what an untrusted one does,
     * which is what turns full trust into sustained use rather than a slightly longer leash.
     */
    public float maxStandEnergy() {
        return switch (this) {
            case DORMANT -> 60F;
            case PARTIAL -> 100F;
            case EMERGING -> 160F;
            case BONDED -> 250F;
        };
    }

    /** How long an {@link #EMERGING} manifestation survives before collapsing on its own. */
    public static final int EMERGING_DURATION_TICKS = 12 * 20;
    /** Trailing slice of that window spent fading out, so it dissolves rather than blinking away. */
    public static final int EMERGING_FADE_OUT_TICKS = 2 * 20;

    private static final TrustTier[] VALUES = values();

    /** Clamps rather than throwing - saved data predates this enum and may hold anything. */
    public static TrustTier fromLevel(int level) {
        return VALUES[Mth.clamp(level, 0, VALUES.length - 1)];
    }

    public int level() {
        return ordinal();
    }

    /**
     * Whether casting actually produces a Stand entity. Every tier can be <em>cast</em> (which
     * raises the aura); DORMANT simply never takes form, so the cast is aura and passive effects
     * alone. Guards combat actions too - there are no arms to punch or block with at DORMANT.
     */
    public boolean manifestsEntity() {
        return this != DORMANT;
    }

    /** {@link #PARTIAL} shows arms only - see {@code StandModel.setCustomAnimations}. */
    public boolean isPartialManifestation() {
        return this == PARTIAL;
    }

    /** Whether the manifestation expires on a timer instead of lasting until energy runs out. */
    public boolean isTimeLimited() {
        return this == EMERGING;
    }

    /**
     * Whether the Stand throws the punches. PARTIAL manifests arms as pure reinforcement - they
     * block, but the user's M1 stays their own ordinary attack rather than becoming a Stand
     * strike, so the punch request is dropped server-side and vanilla's attack is left alone
     * (the client only ever peeks at the attack key, never consumes it - see StandCombatInput).
     */
    public boolean canStandPunch() {
        return isFullManifestation();
    }

    /** Whether the Stand can guard for its user. Arms alone are enough for this. */
    public boolean canGuard() {
        return manifestsEntity();
    }

    /** Whether the Stand can detach from its user to chase down a distant target. */
    public boolean canActAtRange() {
        return isFullManifestation();
    }

    /** Only a fully-manifested Stand renders its whole body; PARTIAL shows arms alone. */
    public boolean isFullManifestation() {
        return manifestsEntity() && !isPartialManifestation();
    }

    /**
     * Upkeep while cast: nothing manifested at DORMANT so nothing to sustain, half while only
     * arms are out, full for a whole body.
     */
    public float energyDrainMultiplier() {
        return switch (this) {
            case DORMANT -> 0F;
            case PARTIAL -> 0.5F;
            default -> 1F;
        };
    }

    /** Lowercase name for commands and translation keys. */
    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
