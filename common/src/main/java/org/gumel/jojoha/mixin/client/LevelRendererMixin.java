package org.gumel.jojoha.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.gumel.jojoha.client.LevelPostPasses;
import org.gumel.jojoha.client.PilotBody;
import org.gumel.jojoha.client.TimeStopView;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Inject(method = "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;"
            + "Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;"
            + "Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", at = @At("HEAD"))
    private void jojoha$capturePostView(DeltaTracker deltaTracker, boolean renderBlockOutline,
                                        Camera camera,
                                        GameRenderer gameRenderer,
                                        LightTexture lightTexture,
                                        Matrix4f frustumMatrix, Matrix4f projectionMatrix,
                                        CallbackInfo ci) {
        LevelPostPasses.capture(frustumMatrix, projectionMatrix, camera,
                deltaTracker.getGameTimeDeltaPartialTick(false));
    }

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

    @ModifyArg(method = RENDER_ENTITY, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Mth;lerp(FFF)F"), index = 0)
    private float jojoha$holdFrozenFacing(float partialTick, @Local(argsOnly = true) Entity entity) {
        return TimeStopView.clientHolds(entity) ? 1.0F : partialTick;
    }

    @ModifyArg(method = RENDER_ENTITY, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;render("
                    + "Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V"), index = 5)
    private float jojoha$holdFrozenFrame(float partialTick, @Local(argsOnly = true) Entity entity) {
        return TimeStopView.clientHolds(entity) ? 1.0F : partialTick;
    }

    @ModifyArg(method = RENDER_ENTITY, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;getPackedLightCoords("
                    + "Lnet/minecraft/world/entity/Entity;F)I"), index = 1)
    private float jojoha$holdFrozenLight(float partialTick, @Local(argsOnly = true) Entity entity) {
        return TimeStopView.clientHolds(entity) ? 1.0F : partialTick;
    }

    private static final String RENDER_ENTITY = "renderEntity(Lnet/minecraft/world/entity/Entity;DDDF"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V";
}
