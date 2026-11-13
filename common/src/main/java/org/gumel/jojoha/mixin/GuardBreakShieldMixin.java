package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.combat.GuardBreak;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class GuardBreakShieldMixin {

    @Inject(method = "isBlocking", at = @At("HEAD"), cancellable = true)
    private void jojoha$noShieldWhileBroken(CallbackInfoReturnable<Boolean> cir) {
        if (GuardBreak.isBroken((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
