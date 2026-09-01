package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.gumel.jojoha.client.PilotBody;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Notes whether the level renderer drew the local player itself this frame.
 *
 * <p>Purely an observation - nothing is changed here. {@link PilotBody} draws the pilot's body only
 * when the game did not, and this is how it finds out. Vanilla skips the local player whenever the
 * camera is on something else; NeoForge patches that. Asking rather than assuming keeps both right.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderEntity(Lnet/minecraft/world/entity/Entity;DDDF"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At("HEAD"))
    private void jojoha$notePlayerDrawn(Entity entity, double camX, double camY, double camZ,
                                        float partialTick, PoseStack poseStack,
                                        MultiBufferSource buffers, CallbackInfo ci) {
        if (entity == Minecraft.getInstance().player) {
            PilotBody.markDrawnByLevel();
        }
    }
}
