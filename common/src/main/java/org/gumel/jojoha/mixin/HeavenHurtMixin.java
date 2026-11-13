package org.gumel.jojoha.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.heaven.RealityBend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class HeavenHurtMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void jojoha$heavenRefuses(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() && Heaven.refuses(self, source)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, index = 2)
    private float jojoha$heavenIncoming(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return amount;
        }
        return Heaven.incoming(self, source, amount);
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void jojoha$realityBend(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() && RealityBend.save(self, source)) {
            cir.setReturnValue(true);
        }
    }
}
