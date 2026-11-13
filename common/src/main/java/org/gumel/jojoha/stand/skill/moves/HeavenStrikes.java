package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.GuardBreak;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.passive.CrushingForcePassive;
import org.gumel.jojoha.stand.passive.StandPassives;

final class HeavenStrikes {
    static final int GUARD_BREAK_TICKS = 40;

    private HeavenStrikes() {
    }

    static boolean strike(ServerPlayer player, LivingEntity target, String key, float base, Vec3 push,
                          ImpactSpec spec, boolean breaksGuard) {
        ServerLevel level = player.serverLevel();
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (breaksGuard) {
            if (CrushingForcePassive.isGuarding(target)) {
                GuardBreak.apply(target, GUARD_BREAK_TICKS);
            }
            if (target instanceof Player victim && victim.isBlocking()) {
                victim.disableShield();
            }
        }
        float raw = StandTuning.damage(key, base) * data.stand.powerScale();
        float dealt = StandPassives.scaleOutgoing(player, data, StandHits.bodyOf(target), raw);
        target.invulnerableTime = 0;
        boolean landed = target.hurt(player.damageSources().playerAttack(player), DamageScale.stand(player, target, dealt));
        target.setDeltaMovement(push);
        target.hurtMarked = true;
        target.fallDistance = 0F;
        data.combatTicks = StandCombatHandler.COMBAT_TIMER_TICKS;
        Vec3 way = push.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : push.normalize();
        Impacts.landed(level, target, way, spec, Impacts.colourOf(player));
        return landed;
    }
}
