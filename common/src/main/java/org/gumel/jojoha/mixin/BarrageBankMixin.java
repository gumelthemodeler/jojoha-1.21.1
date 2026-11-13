package org.gumel.jojoha.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.combat.BarrageBank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 900)
public abstract class BarrageBankMixin {
    @Inject(method = "hurt", at = @At("HEAD"))
    private void jojoha$bankBefore(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        BarrageBank.beforeHurt((LivingEntity) (Object) this, source);
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void jojoha$bankAfter(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        BarrageBank.afterHurt((LivingEntity) (Object) this, source);
    }

    @Inject(method = "knockback(DDD)V", at = @At("HEAD"), cancellable = true)
    private void jojoha$heldInPlace(double strength, double x, double z, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() && BarrageBank.absorbing(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void jojoha$bankSpares(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() && BarrageBank.spares(self, source)) {
            cir.setReturnValue(true);
        }
    }
}
