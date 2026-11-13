package org.gumel.jojoha.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.client.SkullFlashFx;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class SkullCrushRendererMixin {
    private static float jojoha$alpha = -1F;

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FF"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void jojoha$skullBeforeBody(LivingEntity entity, float entityYaw, float partialTick,
                                        PoseStack poseStack, MultiBufferSource buffers, int light,
                                        CallbackInfo ci) {
        jojoha$alpha = SkullFlashFx.bodyAlpha(entity);
        if (jojoha$alpha < 0F) {
            return;
        }

        SkullFlashFx.renderInside(entity, poseStack, buffers, light, partialTick);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FF"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;setupAnim"
                            + "(Lnet/minecraft/world/entity/Entity;FFFFF)V",
                    shift = At.Shift.AFTER))
    private void jojoha$skeletonInPose(LivingEntity entity, float entityYaw, float partialTick,
                                       PoseStack poseStack, MultiBufferSource buffers, int light,
                                       CallbackInfo ci) {
        if (jojoha$alpha < 0F) {
            return;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        EntityModel<?> model = ((LivingEntityRenderer) (Object) this).getModel();
        if (model instanceof HumanoidModel<?> humanoid) {
            SkullFlashFx.renderSkeleton(humanoid, poseStack, buffers, light,
                    OverlayTexture.NO_OVERLAY);
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FF"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void jojoha$done(LivingEntity entity, float entityYaw, float partialTick,
                             PoseStack poseStack, MultiBufferSource buffers, int light,
                             CallbackInfo ci) {
        jojoha$alpha = -1F;
    }

    @ModifyReturnValue(method = "getRenderType", at = @At("RETURN"))
    private RenderType jojoha$seeThrough(RenderType original, LivingEntity entity) {
        if (jojoha$alpha < 0F) {
            return original;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        ResourceLocation texture = ((LivingEntityRenderer) (Object) this).getTextureLocation(entity);
        return RenderType.itemEntityTranslucentCull(texture);
    }

    @WrapOperation(method = "render(Lnet/minecraft/world/entity/LivingEntity;FF"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer"
                            + "(Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private void jojoha$fadeBody(EntityModel<?> model, PoseStack poseStack, VertexConsumer consumer,
                                 int light, int overlay, int colour, Operation<Void> original) {
        if (jojoha$alpha >= 0F) {
            int alpha = Math.max(0, Math.min(255, Math.round(jojoha$alpha * 255F)));
            colour = (alpha << 24) | (colour & 0xFFFFFF);
        }
        original.call(model, poseStack, consumer, light, overlay, colour);
    }
}
