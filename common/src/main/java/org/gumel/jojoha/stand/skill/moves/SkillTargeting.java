package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.stand.StandAim;

/**
 * Server-side aiming for the moves that need a target.
 *
 * <p>Defers to {@link StandAim} rather than casting its own ray - a move must never take the
 * client's word for what it hit, but it should reach the same conclusion the client's crosshair
 * did, and sharing one implementation is what makes both true at once.
 */
final class SkillTargeting {
    /** Matches the Stand's reach, so a move can never acquire something the Stand cannot get to. */
    static final double REACH = StandAim.DEFAULT_REACH;

    private SkillTargeting() {
    }

    static LivingEntity lookTarget(ServerPlayer player) {
        return StandAim.lookTarget(player, REACH);
    }

    static LivingEntity lookTarget(ServerPlayer player, double reach) {
        return StandAim.lookTarget(player, reach);
    }
}
