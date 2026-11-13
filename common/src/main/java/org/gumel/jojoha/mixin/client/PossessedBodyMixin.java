package org.gumel.jojoha.mixin.client;

import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.client.PossessionClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PossessedBodyMixin {
    @Inject(method = "tick()V", at = @At("HEAD"))
    private void jojoha$walkedByItsPossessor(CallbackInfo ci) {
        PossessionClient.tickBody((LivingEntity) (Object) this);
    }

    @Inject(method = "lerpTo(DDDFFI)V", at = @At("HEAD"), cancellable = true)
    private void jojoha$keepsItsOwnPlace(double x, double y, double z, float yRot, float xRot, int steps,
                                         CallbackInfo ci) {
        if (PossessionClient.drives((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }

    @Inject(method = "lerpHeadTo(FI)V", at = @At("HEAD"), cancellable = true)
    private void jojoha$keepsItsOwnHeading(float yHeadRot, int steps, CallbackInfo ci) {
        if (PossessionClient.drives((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
