package org.gumel.jojoha.data;

import net.minecraft.server.level.ServerPlayer;

public final class StatPoints {
    public static final int STRENGTH = 0;
    public static final int VITALITY = 1;
    public static final int AGILITY = 2;
    public static final int ENDURANCE = 3;
    public static final int WORTHINESS = 4;

    public static final int COUNT = 5;

    public static final int POWER = 0;
    public static final int SPEED = 1;
    public static final int STAND_ENDURANCE = 2;
    public static final int PROTECTION = 3;
    public static final int POTENTIAL = 4;

    public static final int MAX_STAT = 99;

    public static final String[] PLAYER_NAMES = {"strength", "vitality", "agility", "endurance",
            "worthiness"};
    public static final String[] STAND_NAMES = {"power", "speed", "endurance", "protection",
            "potential"};

    public static final String[] PLAYER_ABBREVIATIONS = {"STR", "VIT", "AGI", "END", "WOR"};
    public static final String[] STAND_ABBREVIATIONS = {"POW", "SPD", "END", "PRO", "POT"};

    public static int playerStat(JojohaPlayerData data, int stat) {
        return switch (stat) {
            case STRENGTH -> data.strength;
            case VITALITY -> data.vitality;
            case AGILITY -> data.agility;
            case ENDURANCE -> data.endurance;
            case WORTHINESS -> data.worthiness;
            default -> 0;
        };
    }

    public static boolean setStandStat(ServerPlayer player, int stat, int value) {
        if (stat < 0 || stat >= COUNT) {
            return false;
        }

        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent()) {
            return false;
        }

        if (stat == STAND_ENDURANCE) {
            data.endurance = Math.max(0, Math.min(value, MAX_STAT));
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            StandGrowth.considerTrust(player);
            return true;
        }

        value = Math.min(value, MAX_STAT);
        int[] current = data.stand.stats();
        int steps = value - current[stat];
        StandData raised = data.stand;
        for (int i = 0; i < steps; i++) {
            raised = raised.plusStat(stat);
        }
        if (steps < 0) {
            int[] next = raised.stats();
            next[stat] = Math.max(0, value);
            raised = new StandData(raised.standId(), raised.trustTier(),
                    next[0], next[1], next[2], next[3], next[4], raised.usageCount(), raised.skin());
        }

        data.stand = raised;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        StandGrowth.considerTrust(player);
        return true;
    }

    private StatPoints() {
    }

    public static boolean spend(ServerPlayer player, boolean stand, int stat, int amount) {
        return false;
    }

}
