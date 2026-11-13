package org.gumel.jojoha.combat;

import net.minecraft.world.effect.MobEffectInstance;
import org.gumel.jojoha.registry.ModEffects;

public final class PlayerStun {
    public static final int MAX_TICKS = 30;

    private PlayerStun() {
    }

    public static MobEffectInstance cap(MobEffectInstance incoming) {
        if (incoming.isInfiniteDuration() || incoming.getDuration() <= MAX_TICKS
                || incoming.getEffect().value() != ModEffects.STUN.get()) {
            return incoming;
        }
        return new MobEffectInstance(incoming.getEffect(), MAX_TICKS, incoming.getAmplifier(),
                incoming.isAmbient(), incoming.isVisible(), incoming.showIcon());
    }
}
