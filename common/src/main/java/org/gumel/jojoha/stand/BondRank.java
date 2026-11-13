package org.gumel.jojoha.stand;

import org.gumel.jojoha.data.StandData;

public enum BondRank {
    NONE(0, 0F, "none"),
    RANK_I(50, 10F, "rank_i"),
    RANK_II(75, 20F, "rank_ii"),
    RESONANT(99, 30F, "resonant");

    private static final BondRank[] VALUES = values();

    private final int average;
    private final float energyBonus;
    private final String key;

    BondRank(int average, float energyBonus, String key) {
        this.average = average;
        this.energyBonus = energyBonus;
        this.key = key;
    }

    public int average() {
        return average;
    }

    public float energyBonus() {
        return energyBonus;
    }

    public String translationKey() {
        return "bond_rank.jojoha." + key;
    }

    public static BondRank of(StandData stand) {
        if (!stand.isPresent() || stand.trust() != TrustTier.BONDED) {
            return NONE;
        }
        return forAverage(averageStat(stand));
    }

    public static BondRank forAverage(int average) {
        BondRank reached = NONE;
        for (BondRank rank : VALUES) {
            if (rank != NONE && average >= rank.average) {
                reached = rank;
            }
        }
        return reached;
    }

    public BondRank next() {
        return ordinal() + 1 < VALUES.length ? VALUES[ordinal() + 1] : null;
    }

    public static int averageStat(StandData stand) {
        if (!stand.isPresent()) {
            return 0;
        }
        int sum = 0;
        for (int stat : stand.stats()) {
            sum += stat;
        }
        return sum / stand.stats().length;
    }
}
