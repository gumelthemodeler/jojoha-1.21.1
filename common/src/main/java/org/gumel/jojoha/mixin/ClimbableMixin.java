package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.combat.Climbing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class ClimbableMixin {
    @Inject(method = "onClimbable", at = @At("RETURN"), cancellable = true)
    private void jojoha$clingToWalls(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && Climbing.canClimb((LivingEntity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
