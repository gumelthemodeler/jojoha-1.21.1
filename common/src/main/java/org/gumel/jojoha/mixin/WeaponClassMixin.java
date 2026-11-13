package org.gumel.jojoha.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.combat.WeaponClasses;
import org.gumel.jojoha.stand.anubis.Anubis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class WeaponClassMixin {

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, index = 2)
    private float jojoha$weaponClassStrike(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return amount;
        }
        return Anubis.outgoing(source, self,
                amount * WeaponClasses.strikeMultiplier(source, self));
    }
}
