package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.gumel.jojoha.client.TimeStopMask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderTarget.class)
public abstract class RenderTargetCaptureMixin {
    @Inject(method = "bindWrite", at = @At("HEAD"), cancellable = true)
    private void jojoha$keepCaptureInMask(boolean setViewport, CallbackInfo ci) {
        if (TimeStopMask.redirects((RenderTarget) (Object) this)) {
            TimeStopMask.bindMask();
            ci.cancel();
        }
    }

    @Inject(method = "unbindWrite", at = @At("HEAD"), cancellable = true)
    private void jojoha$keepCaptureBound(CallbackInfo ci) {
        if (TimeStopMask.capturing()) {
            TimeStopMask.bindMask();
            ci.cancel();
        }
    }
}
