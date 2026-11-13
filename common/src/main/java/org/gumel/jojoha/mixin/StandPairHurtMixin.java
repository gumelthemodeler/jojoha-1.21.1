package org.gumel.jojoha.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.combat.StandHits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class StandPairHurtMixin {
    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void jojoha$oneBlowForThePair(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (StandHits.refuseDirect((LivingEntity) (Object) this, source)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("RETURN"))
    private void jojoha$noteTheBlow(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            StandHits.noteDirect((LivingEntity) (Object) this, source);
        }
    }
}
