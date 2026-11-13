package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.combat.VampireBlood;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class VampireBreathMixin {
    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void jojoha$noBreathToLose(int air, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (VampireBlood.breathless(self)) {
            cir.setReturnValue(self.getMaxAirSupply());
        }
    }
}
