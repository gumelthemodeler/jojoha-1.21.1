package org.gumel.jojoha.config;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;

public enum BlockBreaking {
    ALWAYS,
    MOB_GRIEFING,
    NEVER;

    public boolean allows(ServerLevel level) {
        return switch (this) {
            case ALWAYS -> true;
            case MOB_GRIEFING -> level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            case NEVER -> false;
        };
    }
}
