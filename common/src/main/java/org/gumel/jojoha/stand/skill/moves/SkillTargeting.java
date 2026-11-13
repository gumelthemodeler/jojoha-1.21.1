package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.stand.StandAim;

final class SkillTargeting {
    static final double REACH = StandAim.DEFAULT_REACH;

    private SkillTargeting() {
    }

    static final double CATCH_SLACK = 1.0;

    static boolean stillInReach(ServerPlayer player, LivingEntity target, double reach) {
        return target.isAlive() && player.distanceTo(target) <= reach + CATCH_SLACK;
    }

    static LivingEntity lookTarget(ServerPlayer player) {
        return StandAim.lookTarget(player, REACH);
    }

    static LivingEntity lookTarget(ServerPlayer player, double reach) {
        return StandAim.lookTarget(player, reach);
    }
}
