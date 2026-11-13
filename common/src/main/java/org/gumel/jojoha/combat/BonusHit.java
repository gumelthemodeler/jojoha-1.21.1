package org.gumel.jojoha.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class BonusHit {

    private BonusHit() {
    }

    public static void land(LivingEntity victim, DamageSource source, float amount) {
        int window = victim.invulnerableTime;
        HitWindow.open(victim);
        try {
            victim.hurt(source, amount);
        } finally {
            victim.invulnerableTime = window;
        }
    }
}
