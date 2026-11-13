package org.gumel.jojoha.stand.combat;

import net.minecraft.util.Mth;

public record StandCombatProfile(double spread, double lead, double momentumGain, int extraAfterimages,
                                 double clashStrength, float finisherScale,
                                 double[] punchWeights) {
    public static final StandCombatProfile NEUTRAL = new StandCombatProfile(1.0, 0.0, 1.0, 0, 1.0,
            BarrageGenerator.FINISHER_SCALE, new double[] {1.0, 1.0, 1.0, 1.0, 1.0});

    public static final StandCombatProfile STAR_PLATINUM = new StandCombatProfile(0.7, 0.15, 1.2, 0, 1.1,
            BarrageGenerator.FINISHER_SCALE, new double[] {1.4, 0.9, 0.9, 0.8, 0.8});

    public static final StandCombatProfile THE_WORLD = new StandCombatProfile(1.15, 0.0, 0.9, 1, 1.0,
            3.2F, new double[] {0.8, 1.2, 1.0, 1.3, 1.1});

    public static final double MIN_SPREAD = 0.4, MAX_SPREAD = 1.6;
    public static final double MAX_LEAD = 0.4;
    public static final double MIN_MOMENTUM_GAIN = 0.5, MAX_MOMENTUM_GAIN = 1.5;
    public static final int MAX_EXTRA_AFTERIMAGES = 1;
    public static final double MIN_CLASH_STRENGTH = 1.0, MAX_CLASH_STRENGTH = 1.1;
    public static final float MIN_FINISHER_SCALE = 2.0F, MAX_FINISHER_SCALE = 3.3F;
    public static final double MAX_PUNCH_WEIGHT = 3.0;

    public StandCombatProfile bounded() {
        double[] weights = new double[PunchType.values().length];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = Mth.clamp(i < punchWeights.length ? punchWeights[i] : 1.0, 0.0, MAX_PUNCH_WEIGHT);
        }
        return new StandCombatProfile(
                Mth.clamp(spread, MIN_SPREAD, MAX_SPREAD),
                Mth.clamp(lead, 0.0, MAX_LEAD),
                Mth.clamp(momentumGain, MIN_MOMENTUM_GAIN, MAX_MOMENTUM_GAIN),
                Mth.clamp(extraAfterimages, 0, MAX_EXTRA_AFTERIMAGES),
                Mth.clamp(clashStrength, MIN_CLASH_STRENGTH, MAX_CLASH_STRENGTH),
                Mth.clamp(finisherScale, MIN_FINISHER_SCALE, MAX_FINISHER_SCALE),
                weights);
    }

    public double weight(PunchType type) {
        return type.ordinal() < punchWeights.length ? punchWeights[type.ordinal()] : 1.0;
    }
}
