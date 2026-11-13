package org.gumel.jojoha.data;

import java.util.Locale;

public enum Fate {
    HUMAN,
    VAMPIRE,
    PILLAR_MAN,
    ULTIMATE_LIFEFORM;

    public static Fate of(JojohaPlayerData data) {
        return switch (data.vampireStage) {
            case NONE -> HUMAN;
            case VAMPIRE -> VAMPIRE;
            case PILLAR_MAN -> PILLAR_MAN;
            case ULTIMATE_LIFEFORM -> ULTIMATE_LIFEFORM;
        };
    }

    public static boolean undead(JojohaPlayerData data) {
        return of(data) != HUMAN;
    }

    public static boolean ultimate(JojohaPlayerData data) {
        return data.vampireStage == VampireStage.ULTIMATE_LIFEFORM;
    }

    public static boolean pillar(JojohaPlayerData data) {
        return data.vampireStage == VampireStage.PILLAR_MAN || data.vampireStage == VampireStage.ULTIMATE_LIFEFORM;
    }

    public static boolean barredFromHamon(JojohaPlayerData data) {
        return undead(data) && !ultimate(data);
    }

    public static boolean barredFromStands(JojohaPlayerData data) {
        return ultimate(data);
    }

    public static boolean breathesHamon(JojohaPlayerData data) {
        return data.discipline == Discipline.HAMON && !barredFromHamon(data);
    }

    public String translationKey() {
        return "fate.jojoha." + name().toLowerCase(Locale.ROOT);
    }
}
