package org.gumel.jojoha.combat;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class BarrageBlow {
    private static Entity throwing;

    private BarrageBlow() {
    }

    public static boolean hurt(LivingEntity victim, DamageSource source, float amount) {
        if (BarrageBank.slipping(victim)) {
            return false;
        }
        Entity previous = throwing;
        throwing = source.getEntity();
        try {
            return StandPierce.hurt(victim, source, amount);
        } finally {
            throwing = previous;
        }
    }

    public static void within(Entity attacker, Runnable blow) {
        Entity previous = throwing;
        throwing = attacker;
        try {
            blow.run();
        } finally {
            throwing = previous;
        }
    }

    public static boolean is(DamageSource source) {
        return throwing != null && source.getEntity() == throwing;
    }
}
