package org.gumel.jojoha.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.registry.ModEffects;

public final class Burning {
    public static final int TICKS = 100;

    public static final int MAX_AMPLIFIER = 2;

    public static final float DAMAGE = 1.0F;

    public static final int EVERY = 20;

    private static final float PER_STACK = 0.5F;

    private Burning() {
    }

    public static boolean immune(LivingEntity victim) {
        return victim.fireImmune() || victim.hasEffect(MobEffects.FIRE_RESISTANCE);
    }

    public static int apply(LivingEntity victim) {
        if (immune(victim) || victim.isInWaterOrBubble()) {
            return -1;
        }

        MobEffectInstance open = victim.getEffect(ModEffects.burning());
        int stack = open == null ? 0 : Math.min(MAX_AMPLIFIER, open.getAmplifier() + 1);
        victim.addEffect(new MobEffectInstance(ModEffects.burning(),
                JojohaServerConfig.BURN_TICKS.whole(), stack, false, true, true));
        return stack;
    }

    public static float perSecond(int amplifier) {
        return JojohaServerConfig.BURN_DAMAGE.value() * (1F + Math.min(amplifier, MAX_AMPLIFIER) * PER_STACK);
    }

    public static boolean burn(LivingEntity victim, int amplifier) {
        if (!(victim.level() instanceof ServerLevel level)) {
            return true;
        }

        if (immune(victim) || victim.isInWaterOrBubble()) {
            level.playSound(null, victim.blockPosition(), SoundEvents.GENERIC_EXTINGUISH_FIRE,
                    SoundSource.NEUTRAL, 0.7F, 1.6F);
            level.sendParticles(ParticleTypes.SMOKE, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6,
                    victim.getZ(), 8, 0.25, 0.3, 0.25, 0.02);
            return false;
        }

        victim.hurt(level.damageSources().onFire(),
                DamageScale.combat(null, victim, perSecond(amplifier)));
        level.sendParticles(ParticleTypes.FLAME, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5,
                victim.getZ(), 3 + amplifier * 3, 0.25, 0.4, 0.25, 0.01);
        return true;
    }
}
