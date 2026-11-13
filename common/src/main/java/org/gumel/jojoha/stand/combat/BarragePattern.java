package org.gumel.jojoha.stand.combat;

import net.minecraft.util.RandomSource;

public enum BarragePattern {
    STANDARD(1.0, 0.5, 35, 25, 12, 12, 16),
    CONCENTRATED(0.45, 0.6, 60, 15, 10, 10, 5),
    WIDE(1.3, 0.5, 20, 30, 8, 7, 35),
    TRACKING(0.8, 1.0, 55, 20, 10, 5, 10);

    private final double spread;
    private final double lead;
    private final int[] weights;

    BarragePattern(double spread, double lead, int... weights) {
        this.spread = spread;
        this.lead = lead;
        this.weights = weights;
    }

    public double spread() {
        return spread;
    }

    public double lead() {
        return lead;
    }

    public PunchType pick(RandomSource random) {
        int total = 0;
        for (int weight : weights) {
            total += weight;
        }
        int roll = random.nextInt(total);
        PunchType[] types = PunchType.values();
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0) {
                return types[i];
            }
        }
        return PunchType.STRAIGHT;
    }

    public PunchType pick(RandomSource random, StandCombatProfile profile) {
        PunchType[] types = PunchType.values();
        double total = 0.0;
        for (int i = 0; i < weights.length; i++) {
            total += weights[i] * profile.weight(types[i]);
        }
        if (total <= 0.0) {
            return pick(random);
        }
        double roll = random.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i] * profile.weight(types[i]);
            if (roll < 0.0) {
                return types[i];
            }
        }
        return types[weights.length - 1];
    }
}
