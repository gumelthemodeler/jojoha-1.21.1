package org.gumel.jojoha.data;

import net.minecraft.server.level.ServerPlayer;

/**
 * Spending an earned point, and the one place that decides whether it may be spent.
 *
 * <p>Server side only, and deliberately not a method on the data. The interface asks; this answers.
 * Everything a client sends is a request, and the difference between a request and an instruction
 * is exactly this class: the pool is checked here, the index is checked here, and a message that
 * fails either is dropped without comment rather than clamped into something that half works.
 */
public final class StatPoints {
    /** The five, in the order the interface lists them. */
    public static final int STRENGTH = 0;
    public static final int VITALITY = 1;
    public static final int AGILITY = 2;
    public static final int ENDURANCE = 3;
    public static final int WORTHINESS = 4;

    public static final int COUNT = 5;

    /**
     * As high as any one stat goes.
     *
     * <p>Forty, against a start of five, so a maxed stat is thirty-five points of investment. The
     * ceiling is what makes the curves in {@link StatEffects} mean anything: every bonus there is a
     * fraction of the way from five to this, so without a cap the fraction has no denominator and
     * "a maxed stat is worth one enchantment" stops being a statement about anything.
     */
    public static final int MAX_STAT = 40;

    /** The Stand's five, in the order its plate lists them. */
    public static final String[] PLAYER_NAMES = {"strength", "vitality", "agility", "endurance",
            "worthiness"};
    public static final String[] STAND_NAMES = {"power", "speed", "endurance", "protection",
            "potential"};

    /** Grants points rather than spending them - the command path. */
    public static void grant(ServerPlayer player, int amount) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.availableStatPoints = Math.max(0, data.availableStatPoints + amount);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    /** Hands over Stand points. Mirrors {@link #grant} for the other pool. */
    public static void grantStand(ServerPlayer player, int amount) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.availableStandPoints = Math.max(0, data.availableStandPoints + amount);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static void setStand(ServerPlayer player, int amount) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.availableStandPoints = Math.max(0, amount);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static void set(ServerPlayer player, int amount) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.availableStatPoints = Math.max(0, amount);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    /** One of the player's five by index, in the order the interface lists them. */
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

    /** Sets one of the Stand's five outright. Command path; the interface only ever adds one. */
    public static boolean setStandStat(ServerPlayer player, int stat, int value) {
        if (stat < 0 || stat >= COUNT) {
            return false;
        }

        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.stand.isPresent()) {
            return false;
        }

        value = Math.min(value, MAX_STAT);
        int[] current = data.stand.stats();
        int steps = value - current[stat];
        org.gumel.jojoha.data.StandData raised = data.stand;
        for (int i = 0; i < steps; i++) {
            raised = raised.plusStat(stat);
        }
        if (steps < 0) {
            // Down is rebuilt outright rather than stepped, since there is no minus-one wither and
            // a command setting a stat lower is not something the game does to itself.
            int[] next = raised.stats();
            next[stat] = Math.max(0, value);
            raised = new org.gumel.jojoha.data.StandData(raised.standId(), raised.trustTier(),
                    next[0], next[1], next[2], next[3], next[4], raised.usageCount(), raised.skin());
        }

        data.stand = raised;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        return true;
    }

    private StatPoints() {
    }

    /**
     * Puts one point into one of the player's five, or one of their Stand's.
     *
     * <p>Two pools, not one. A Stand's growth is its own: it draws on points earned as a Stand user
     * and spends them on the Stand, while the player's five draw on the player's. Sharing one pool
     * made every point a choice between yourself and your Stand, which sounds like a decision and in
     * practice just means the Stand is always last in the queue.
     *
     * @return whether anything was spent, so the caller can decide whether a sync is worth sending
     */
    public static boolean spend(ServerPlayer player, boolean stand, int stat, int amount) {
        if (stat < 0 || stat >= COUNT || amount <= 0) {
            return false;
        }

        // Asked for many, given as many as are actually there. A client can request twenty points
        // it does not have; what it gets is however many of them exist, which is the same answer a
        // client asking one at a time twenty times would have got.
        boolean any = false;
        for (int i = 0; i < amount; i++) {
            if (!spendOne(player, stand, stat)) {
                break;
            }
            any = true;
        }
        return any;
    }

    private static boolean spendOne(ServerPlayer player, boolean stand, int stat) {
        if (stat < 0 || stat >= COUNT) {
            return false;
        }

        JojohaPlayerData data = PlayerDataAccess.get(player);

        if (stand) {
            // The Stand's own pool, and only spendable once there is a Stand to spend it on.
            if (data.availableStandPoints <= 0
                    || !data.stand.isPresent() || data.stand.stats()[stat] >= MAX_STAT) {
                return false;
            }

            data.stand = data.stand.plusStat(stat);
            data.availableStandPoints--;
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            return true;
        }

        // A point spent on something already at the ceiling is a point thrown away, so the request
        // is refused rather than taken.
        if (data.availableStatPoints <= 0 || playerStat(data, stat) >= MAX_STAT) {
            return false;
        }

        switch (stat) {
            case STRENGTH -> data.strength++;
            case VITALITY -> data.vitality++;
            case AGILITY -> data.agility++;
            case ENDURANCE -> data.endurance++;
            case WORTHINESS -> data.worthiness++;
            default -> {
                return false;
            }
        }

        data.availableStatPoints--;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        return true;
    }
}
