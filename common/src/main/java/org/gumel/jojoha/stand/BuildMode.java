package org.gumel.jojoha.stand;

import net.minecraft.core.Direction;

import java.util.Locale;

public enum BuildMode {
    SINGLE,

    ROW,

    COLUMN,

    FREE,

    SELECTION;

    private static final BuildMode[] VALUES = values();

    public static BuildMode fromOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : SINGLE;
    }

    public boolean stretches() {
        return this != SINGLE;
    }

    public boolean fillsAVolume() {
        return this == SELECTION;
    }

    public Direction.Axis axis(int dx, int dy, int dz) {
        return switch (this) {
            case COLUMN -> Direction.Axis.Y;
            case ROW -> Math.abs(dx) >= Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
            default -> null;
        };
    }

    public String translationKey() {
        return "buildmode.jojoha." + name().toLowerCase(Locale.ROOT);
    }
}
