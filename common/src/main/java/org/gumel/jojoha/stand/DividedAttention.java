package org.gumel.jojoha.stand;

import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;

public final class DividedAttention {
    public static final float SKILL_COST_PERCENT = 25F;

    public static final float PUNCH_SLOW = 1.5F;

    private DividedAttention() {
    }

    public static float skillCost(JojohaPlayerData data) {
        return data.standMode.isAutonomous()
                ? 1F + JojohaServerConfig.DEFENSE_SKILL_COST.value() / 100F : 1F;
    }

    public static int punchTicks(JojohaPlayerData data, int ticks) {
        return data.standMode.isAutonomous()
                ? Math.round(ticks * JojohaServerConfig.DEFENSE_PUNCH_SLOW.value()) : ticks;
    }
}
