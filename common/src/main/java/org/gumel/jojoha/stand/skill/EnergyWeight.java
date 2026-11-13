package org.gumel.jojoha.stand.skill;

public enum EnergyWeight {
    TRIVIAL(1F),
    UPKEEP(5F),
    LIGHT(10F),
    STANDARD(16F),
    HEAVY(26F),
    ULTIMATE(70F);

    private final float cost;

    EnergyWeight(float cost) {
        this.cost = cost;
    }

    public float cost() {
        return cost;
    }
}
