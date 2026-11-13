package org.gumel.jojoha.combat.fx;

import java.util.Locale;

public enum Palette {
    IMPACT,
    BREAK,
    CLASH,
    AWAKEN,
    DREAD,
    PIERCE;

    private static final Palette[] VALUES = values();

    public static Palette byId(int id) {
        return VALUES[Math.max(0, Math.min(id, VALUES.length - 1))];
    }

    public String file() {
        return name().toLowerCase(Locale.ROOT);
    }
}
