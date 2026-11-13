package org.gumel.jojoha.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.stand.mob.MobStands;

public final class ArmourCurve {
    public static final float FLOOR = 35F;

    private static final float PROTECTION_CAP = 20F;

    private ArmourCurve() {
    }

    public static boolean applies(LivingEntity victim, DamageSource source) {
        if (!(victim instanceof ServerPlayer) || !JojohaServerConfig.PVP_ARMOUR_CURVE.on()
                || source.is(DamageTypeTags.BYPASSES_ARMOR) || !DamageScale.modHit(victim)) {
            return false;
        }
        Entity attacker = source.getEntity();
        if (attacker == null || attacker == victim) {
            return false;
        }
        return attacker instanceof Player
                || attacker instanceof Mob mob && MobStands.has(mob);
    }

    public static float afterArmour(LivingEntity victim, DamageSource source, float amount, float pierce) {
        float armour = victim.getArmorValue() * (1F - pierce);
        float toughness = (float) victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float dealt = CombatRules.getDamageAfterAbsorb(victim, amount, source, armour, toughness);
        if (victim.level() instanceof ServerLevel level && !source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            float protection = Math.min(PROTECTION_CAP, EnchantmentHelper.getDamageProtection(level, victim, source));
            float protectionPierce = StandPierce.piercing(source) ? StandPierce.PROTECTION : 0F;
            dealt = CombatRules.getDamageAfterMagicAbsorb(dealt, protection * (1F - protectionPierce));
        }
        float floor = JojohaServerConfig.PVP_ARMOUR_FLOOR.value() / 100F;
        return Math.max(dealt, amount * floor);
    }

    public static float afterResistance(LivingEntity victim, DamageSource source, float amount) {
        if (amount <= 0F || source.is(DamageTypeTags.BYPASSES_RESISTANCE)
                || !victim.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
            return Math.max(0F, amount);
        }
        int level = (victim.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
        return Math.max(0F, amount * (25 - level) / 25F);
    }
}
