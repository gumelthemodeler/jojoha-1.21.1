package org.gumel.jojoha.stand;

public enum StandMode {
    ANALOG,

    DEFENSE,

    UTILITY;

    private static final StandMode[] VALUES = values();

    public StandMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public static StandMode fromOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : ANALOG;
    }

    public boolean isAutonomous() {
        return this == DEFENSE;
    }

    public boolean takesAimedOrders() {
        return this == ANALOG;
    }

    public boolean handlesItems() {
        return this == UTILITY;
    }

    public String translationKey() {
        return switch (this) {
            case DEFENSE -> "hud.jojoha.stand_mode.defense";
            case UTILITY -> "hud.jojoha.stand_mode.utility";
            default -> "hud.jojoha.stand_mode.analog";
        };
    }
}
