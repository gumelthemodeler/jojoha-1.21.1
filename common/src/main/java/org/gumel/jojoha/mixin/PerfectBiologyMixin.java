package org.gumel.jojoha.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.ultimate.PerfectBiology;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class PerfectBiologyMixin {
    @Inject(method = "canBeAffected(Lnet/minecraft/world/effect/MobEffectInstance;)Z", at = @At("HEAD"), cancellable = true)
    private void jojoha$perfectBiology(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if (PerfectBiology.shrugs((LivingEntity) (Object) this, effect.getEffect())) {
            cir.setReturnValue(false);
        }
    }
}
