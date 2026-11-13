package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.Entity;
import org.gumel.jojoha.stand.anubis.Anubis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class PossessedTurnMixin {

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void jojoha$possessedTurn(double yRot, double xRot, CallbackInfo ci) {
        if (Anubis.clientPossessed.test((Entity) (Object) this)) {
            ci.cancel();
        }
    }
}
