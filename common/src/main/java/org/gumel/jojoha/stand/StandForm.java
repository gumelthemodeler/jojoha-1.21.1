package org.gumel.jojoha.stand;

public enum StandForm {
    HUMANOID,

    BOUND,

    WEAPON;

    public boolean isFreeStanding() {
        return this == HUMANOID;
    }

    public boolean isBound() {
        return this == BOUND;
    }

    public boolean hasBody() {
        return this != WEAPON;
    }
}
