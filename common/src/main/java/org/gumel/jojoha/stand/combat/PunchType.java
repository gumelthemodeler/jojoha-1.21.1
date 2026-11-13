package org.gumel.jojoha.stand.combat;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public enum PunchType {
    STRAIGHT(0.03, 0.12, -0.08, 0.08, 0.60, 0.0),
    HOOK(0.50, 0.80, -0.10, 0.10, 0.62, 0.0),
    UPPERCUT(0.05, 0.20, -0.80, -0.55, 0.82, 0.0),
    DOWNWARD(0.05, 0.20, 0.50, 0.75, 0.45, 0.0),
    SIDE(0.25, 0.45, -0.10, 0.10, 0.55, 0.45);

    private final double bendSideMin;
    private final double bendSideMax;
    private final double bendUpMin;
    private final double bendUpMax;
    private final double height;
    private final double flank;

    PunchType(double bendSideMin, double bendSideMax, double bendUpMin, double bendUpMax, double height, double flank) {
        this.bendSideMin = bendSideMin;
        this.bendSideMax = bendSideMax;
        this.bendUpMin = bendUpMin;
        this.bendUpMax = bendUpMax;
        this.height = height;
        this.flank = flank;
    }

    public float bendSide(RandomSource random) {
        return (float) Mth.lerp(random.nextDouble(), bendSideMin, bendSideMax);
    }

    public float bendUp(RandomSource random) {
        return (float) Mth.lerp(random.nextDouble(), bendUpMin, bendUpMax);
    }

    public double height() {
        return height;
    }

    public double flank() {
        return flank;
    }
}
