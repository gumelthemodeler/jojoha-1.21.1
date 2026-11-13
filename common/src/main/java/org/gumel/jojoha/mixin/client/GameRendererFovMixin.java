package org.gumel.jojoha.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.gumel.jojoha.client.BarrageFx;
import org.gumel.jojoha.client.PreciseEyesView;
import org.gumel.jojoha.client.fx.ImpactDirector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererFovMixin {
    @Inject(method = "getFov(Lnet/minecraft/client/Camera;FZ)D", at = @At("RETURN"), cancellable = true)
    private void jojoha$impactPunch(Camera camera, float partialTick, boolean useFovSetting,
                                    CallbackInfoReturnable<Double> cir) {
        float scale = ImpactDirector.fovScale()
                * BarrageFx.fovScale()
                * (useFovSetting ? PreciseEyesView.fovScale(partialTick) : 1F);
        if (scale < 1F) {
            cir.setReturnValue(cir.getReturnValueD() * scale);
        }
    }
}
