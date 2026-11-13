package org.gumel.jojoha.mixin.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.client.KeenEyes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class KeenEyesMixin {
    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void jojoha$keenEyes(Player viewer, CallbackInfoReturnable<Boolean> cir) {
        if (KeenEyes.sees((Entity) (Object) this, viewer)) {
            cir.setReturnValue(false);
        }
    }
}
