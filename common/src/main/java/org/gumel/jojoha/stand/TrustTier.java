package org.gumel.jojoha.stand;

import net.minecraft.util.Mth;

import java.util.Locale;

public enum TrustTier {
    DORMANT,
    PARTIAL,
    EMERGING,
    BONDED;

    public float maxStandEnergy() {
        return switch (this) {
            case DORMANT -> 60F;
            case PARTIAL -> 100F;
            case EMERGING -> 160F;
            case BONDED -> 250F;
        };
    }

    public static final int EMERGING_DURATION_TICKS = 12 * 20;
    public static final int EMERGING_FADE_OUT_TICKS = 2 * 20;

    private static final TrustTier[] VALUES = values();

    public static TrustTier fromLevel(int level) {
        return VALUES[Mth.clamp(level, 0, VALUES.length - 1)];
    }

    public int level() {
        return ordinal();
    }

    public boolean manifestsEntity() {
        return this != DORMANT;
    }

    public boolean isPartialManifestation() {
        return this == PARTIAL;
    }

    public boolean isTimeLimited() {
        return this == EMERGING;
    }

    public boolean canStandPunch() {
        return isFullManifestation();
    }

    public boolean canGuard() {
        return manifestsEntity();
    }

    public boolean canActAtRange() {
        return isFullManifestation();
    }

    public boolean isFullManifestation() {
        return manifestsEntity() && !isPartialManifestation();
    }

    public float energyDrainMultiplier() {
        return switch (this) {
            case DORMANT -> 0F;
            case PARTIAL -> 0.5F;
            default -> 1F;
        };
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
