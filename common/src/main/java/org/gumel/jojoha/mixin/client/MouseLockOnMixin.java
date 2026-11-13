package org.gumel.jojoha.mixin.client;

import net.minecraft.client.MouseHandler;
import org.gumel.jojoha.client.LockOn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseLockOnMixin {
    @Inject(method = "handleAccumulatedMovement", at = @At("TAIL"))
    private void jojoha$followLock(CallbackInfo ci) {
        LockOn.frame();
    }
}
