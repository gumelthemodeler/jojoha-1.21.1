package org.gumel.jojoha.stand;

public enum StandMovementState {
    SUMMON,

    PILOTED,

    WORKING,

    PURSUING,

    GUARDING,

    HOLDING,

    BARRAGE,

    ATTACK,

    COMBAT,

    FOLLOW,

    IDLE;

    public boolean isCombatStance() {
        return this == BARRAGE || this == ATTACK || this == COMBAT || this == GUARDING
                || this == HOLDING;
    }
}
