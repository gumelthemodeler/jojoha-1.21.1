package org.gumel.jojoha.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.gumel.jojoha.client.LevelPostPasses;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererPostMixin {
    @Inject(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel("
                            + "Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;"
                            + "Lnet/minecraft/client/renderer/GameRenderer;"
                            + "Lnet/minecraft/client/renderer/LightTexture;"
                            + "Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
                    shift = At.Shift.AFTER))
    private void jojoha$postPassesAfterLevel(DeltaTracker deltaTracker, CallbackInfo ci) {
        LevelPostPasses.run();
    }
}
