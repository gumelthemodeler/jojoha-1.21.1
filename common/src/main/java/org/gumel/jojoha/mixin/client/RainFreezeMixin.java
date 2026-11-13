package org.gumel.jojoha.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.client.TimeStopView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class RainFreezeMixin {

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void jojoha$noFallingRainInStoppedTime(LightTexture lightTexture, float partialTick,
                                                   double camX, double camY, double camZ,
                                                   CallbackInfo ci) {
        if (TimeStopView.holdsPosition(camX, camY, camZ)) {
            ci.cancel();
        }
    }

    @Inject(method = "tickRain", at = @At("HEAD"), cancellable = true)
    private void jojoha$noSplashesInStoppedTime(Camera camera, CallbackInfo ci) {
        Vec3 at = camera.getPosition();
        if (TimeStopView.holdsPosition(at.x, at.y, at.z)) {
            ci.cancel();
        }
    }
}
