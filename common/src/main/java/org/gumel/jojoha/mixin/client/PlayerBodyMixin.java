package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.gumel.jojoha.body.Body;
import org.gumel.jojoha.body.Limbs;
import org.gumel.jojoha.client.LimbRegrowLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerBodyMixin
        extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private PlayerBodyMixin(EntityRendererProvider.Context context,
                            PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void jojoha$unseenSpirit(AbstractClientPlayer player, float entityYaw, float partialTick,
                                     PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                                     CallbackInfo ci) {
        if (Body.hidden(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;"
                            + "setModelProperties(Lnet/minecraft/client/player/AbstractClientPlayer;)V",
                    shift = At.Shift.AFTER))
    private void jojoha$lostLimbs(AbstractClientPlayer player, float entityYaw, float partialTick,
                                  PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                                  CallbackInfo ci) {
        PlayerModel<AbstractClientPlayer> model = getModel();
        if (Body.missing(player, Limbs.RIGHT_ARM)) {
            model.rightArm.visible = false;
            model.rightSleeve.visible = false;
        }
        if (Body.missing(player, Limbs.LEFT_ARM)) {
            model.leftArm.visible = false;
            model.leftSleeve.visible = false;
        }
        if (Body.missing(player, Limbs.RIGHT_LEG)) {
            model.rightLeg.visible = false;
            model.rightPants.visible = false;
        }
        if (Body.missing(player, Limbs.LEFT_LEG)) {
            model.leftLeg.visible = false;
            model.leftPants.visible = false;
        }
    }

    @Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true)
    private void jojoha$noRightHand(PoseStack poseStack, MultiBufferSource buffers, int light,
                                    AbstractClientPlayer player, CallbackInfo ci) {
        if (Body.missing(player, Limbs.RIGHT_ARM)) {
            jojoha$regrowing(poseStack, buffers, light, player, Limbs.RIGHT_ARM);
            ci.cancel();
        }
    }

    @Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true)
    private void jojoha$noLeftHand(PoseStack poseStack, MultiBufferSource buffers, int light,
                                   AbstractClientPlayer player, CallbackInfo ci) {
        if (Body.missing(player, Limbs.LEFT_ARM)) {
            jojoha$regrowing(poseStack, buffers, light, player, Limbs.LEFT_ARM);
            ci.cancel();
        }
    }

    @Unique
    private void jojoha$regrowing(PoseStack poseStack, MultiBufferSource buffers, int light,
                                   AbstractClientPlayer player, int limb) {
        if (!LimbRegrowLayer.growing(player, limb)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getModel();
        boolean right = limb == Limbs.RIGHT_ARM;
        ModelPart arm = right ? model.rightArm : model.leftArm;
        model.attackTime = 0F;
        model.crouching = false;
        model.swimAmount = 0F;
        model.setupAnim(player, 0F, 0F, 0F, 0F, 0F);
        arm.xRot = 0F;
        LimbRegrowLayer.grow(poseStack, buffers, light, OverlayTexture.NO_OVERLAY, player, arm,
                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
    }
}
