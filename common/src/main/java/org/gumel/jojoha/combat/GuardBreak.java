package org.gumel.jojoha.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.stand.StandCombatHandler;

public final class GuardBreak {
    public static final int TICKS = 60;

    private GuardBreak() {
    }

    public static boolean isBroken(LivingEntity entity) {
        return entity.hasEffect(ModEffects.guardBreak());
    }

    public static void apply(LivingEntity victim, int ticks) {
        victim.addEffect(new MobEffectInstance(ModEffects.guardBreak(), ticks, 0, false, true, true));

        if (victim instanceof ServerPlayer player) {
            StandCombatHandler.breakStandGuard(player);

            MovementFx.pose(player, MovementFx.GUARD_BREAK);

            ServerLevel level = player.serverLevel();
            level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BREAK,
                    SoundSource.PLAYERS, 1.0F, 0.9F);
        }
    }

    public static void apply(LivingEntity victim) {
        apply(victim, TICKS);
    }
}
