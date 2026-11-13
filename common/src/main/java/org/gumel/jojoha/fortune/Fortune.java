package org.gumel.jojoha.fortune;

import org.gumel.jojoha.data.JojohaPlayerData;

import java.util.Locale;

public enum Fortune {
    WICKED,
    LUCKY,
    BIZARRE,
    HONORBOUND,
    CURSED,
    IDEALOGUE,
    TRICKSTER,
    KINDRED,
    GAMBLER,
    DEVOUT;

    public static final int TETRAPHOBIA = 0;

    public static final int SUPERSTITION = 1;

    public static final int PHOBIAS = 2;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String nameKey() {
        return "fortune.jojoha." + id();
    }

    public String descriptionKey() {
        return nameKey() + ".desc";
    }

    public static String phobiaKey(int phobia) {
        return "fortune.jojoha.phobia." + (phobia == SUPERSTITION ? "superstition" : "tetraphobia");
    }

    public static Fortune byId(String id) {
        for (Fortune fortune : values()) {
            if (fortune.id().equals(id)) {
                return fortune;
            }
        }
        return null;
    }

    public static Fortune of(JojohaPlayerData data) {
        return byId(data.fortune);
    }

    public static boolean has(JojohaPlayerData data, Fortune fortune) {
        return fortune.id().equals(data.fortune);
    }
}
