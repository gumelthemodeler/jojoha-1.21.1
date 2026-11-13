package org.gumel.jojoha.mixin;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.gumel.jojoha.combat.ArmourCurve;
import org.gumel.jojoha.combat.StandPierce;
import org.gumel.jojoha.combat.StatPerkEffects;
import org.gumel.jojoha.combat.WeaponClasses;
import org.gumel.jojoha.stand.passive.PiercingThrustPassive;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ArmourPierceMixin {

    @Shadow
    protected abstract void hurtArmor(DamageSource source, float damage);

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("HEAD"), cancellable = true)
    private void jojoha$curvedProtection(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (ArmourCurve.applies(self, source)) {
            cir.setReturnValue(ArmourCurve.afterResistance(self, source, amount));
        }
    }

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
    private void jojoha$pierceArmour(DamageSource source, float amount,
                                     CallbackInfoReturnable<Float> cir) {
        float pierce = Math.max(StandPierce.armour(source),
                Math.max(WeaponClasses.armourPierce(source),
                        StatPerkEffects.armourPierce((LivingEntity) (Object) this, source)));
        pierce = Math.min(1F, pierce + PiercingThrustPassive.armour(
                (LivingEntity) (Object) this, source));
        if (ArmourCurve.applies((LivingEntity) (Object) this, source)) {
            hurtArmor(source, amount);
            cir.setReturnValue(ArmourCurve.afterArmour(
                    (LivingEntity) (Object) this, source, amount, pierce));
            return;
        }
        if (pierce <= 0F) {
            return;
        }

        if (source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return;
        }

        LivingEntity self = (LivingEntity) (Object) this;

        hurtArmor(source, amount);

        float armour = self.getArmorValue() * (1F - pierce);
        float toughness = (float) self.getAttributeValue(Attributes.ARMOR_TOUGHNESS);

        cir.setReturnValue(
                CombatRules.getDamageAfterAbsorb(self, amount, source, armour, toughness));
    }
}
