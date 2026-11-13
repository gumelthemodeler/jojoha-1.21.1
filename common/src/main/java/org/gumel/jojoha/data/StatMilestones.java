package org.gumel.jojoha.data;

import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.JojohaAdvancements;

public final class StatMilestones {
    private static final int NOTABLE = 20;

    private static final int ROUNDED = 25;

    private StatMilestones() {
    }

    public static void reached(ServerPlayer player, JojohaPlayerData data, int stat, int level) {
        if (stat < 0 || stat >= StatPoints.COUNT) {
            return;
        }

        if (level >= NOTABLE) {
            JojohaAdvancements.award(player,
                    "part2/stat_" + StatPoints.PLAYER_NAMES[stat]);
        }

        if (level >= StatPoints.MAX_STAT) {
            JojohaAdvancements.award(player, "part2/beyond_measure");
        }

        if (allAtLeast(data, ROUNDED)) {
            JojohaAdvancements.award(player, "part2/a_life_well_lived");
        }
    }

    private static boolean allAtLeast(JojohaPlayerData data, int level) {
        for (int stat = 0; stat < StatPoints.COUNT; stat++) {
            if (StatPoints.playerStat(data, stat) < level) {
                return false;
            }
        }
        return true;
    }
}
