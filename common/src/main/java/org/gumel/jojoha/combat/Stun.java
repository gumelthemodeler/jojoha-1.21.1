package org.gumel.jojoha.combat;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.registry.ModEffects;

public final class Stun {
    public static final int TIER_ONE = 0;

    public static final int TIER_TWO = 1;

    public static final int STAGGER_TICKS = 18;

    public static void stagger(LivingEntity target) {
        if (target.isAlive()) {
            target.addEffect(new MobEffectInstance(ModEffects.stun(),
                    JojohaServerConfig.STAGGER_TICKS.whole(), TIER_ONE, false, false, true));
        }
    }

    public static void lock(LivingEntity target, int ticks) {
        if (target.isAlive()) {
            target.addEffect(new MobEffectInstance(ModEffects.stun(), ticks, TIER_TWO, false, false, true));
        }
    }

    private Stun() {
    }

    public static int tierOf(LivingEntity entity) {
        MobEffectInstance stun = entity.getEffect(ModEffects.stun());
        return stun == null ? -1 : stun.getAmplifier();
    }

    public static boolean isStunned(LivingEntity entity) {
        return tierOf(entity) >= TIER_ONE;
    }

    public static boolean freezesMovement(LivingEntity entity) {
        return tierOf(entity) >= TIER_TWO;
    }

    public static boolean blocks(LivingEntity entity, boolean moveIsAnAnswer) {
        int tier = tierOf(entity);
        if (tier < TIER_ONE) {
            return false;
        }
        return tier >= TIER_TWO || !moveIsAnAnswer;
    }
}
