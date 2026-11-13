package org.gumel.jojoha.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class StandPierce {
    public static final float ARMOUR = 0.5F;

    public static final float PROTECTION = 0.5F;

    private static final float PROTECTION_PER_POINT = 25F;
    private static final float PROTECTION_CAP = 20F;

    private static Entity dealing;

    private StandPierce() {
    }

    public static boolean hurt(LivingEntity victim, DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        Entity previous = dealing;
        dealing = attacker;
        try {
            return victim.hurt(source, throughProtection(victim, source, amount));
        } finally {
            dealing = previous;
        }
    }

    public static boolean piercing(DamageSource source) {
        return dealing != null && source.getEntity() == dealing;
    }

    public static float armour(DamageSource source) {
        return piercing(source) ? ARMOUR : 0F;
    }

    public static float asThrownAt(LivingEntity victim, DamageSource source, float amount) {
        return piercing(source) ? throughProtection(victim, source, amount) : amount;
    }

    private static float throughProtection(LivingEntity victim, DamageSource source, float amount) {
        if (amount <= 0F || !(victim.level() instanceof ServerLevel level) || ArmourCurve.applies(victim, source)) {
            return amount;
        }
        float points = Math.min(PROTECTION_CAP, EnchantmentHelper.getDamageProtection(level, victim, source));
        if (points <= 0F) {
            return amount;
        }
        float kept = 1F - points / PROTECTION_PER_POINT;
        float keptPierced = 1F - points * (1F - PROTECTION) / PROTECTION_PER_POINT;
        return kept <= 0F ? amount : amount * (keptPierced / kept);
    }
}
