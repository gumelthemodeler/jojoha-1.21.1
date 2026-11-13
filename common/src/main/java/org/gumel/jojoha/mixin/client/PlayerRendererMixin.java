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
import net.minecraft.world.entity.HumanoidArm;
import org.gumel.jojoha.client.AccessoryLayer;
import org.gumel.jojoha.client.FleshBudFx;
import org.gumel.jojoha.client.FleshBudModel;
import org.gumel.jojoha.client.HamonHealHand;
import org.gumel.jojoha.client.HeadlessView;
import org.gumel.jojoha.client.PilotPose;
import org.gumel.jojoha.client.PlayerStabAnimation;
import org.gumel.jojoha.client.StandAfterimages;
import org.gumel.jojoha.client.StandAwakeningGlowLayer;
import org.gumel.jojoha.client.AwakeningLight;
import org.gumel.jojoha.client.StandBoundArmsLayer;
import org.gumel.jojoha.client.StandFirstPersonArms;
import org.gumel.jojoha.client.StoneMaskLayer;
import org.gumel.jojoha.client.StoneMaskModel;
import org.gumel.jojoha.client.SubzeroChillLayer;
import org.gumel.jojoha.client.LimbRegrowLayer;
import org.gumel.jojoha.client.UltimateWingsLayer;
import org.gumel.jojoha.client.ZoomPunchArm;
import org.gumel.jojoha.client.pillar.GoopBody;
import org.gumel.jojoha.client.pillar.PillarBodyLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin
        extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    @Unique
    private float[] jojoha$heldPose;

    @Unique
    private HumanoidArm jojoha$punchingArm;

    private PlayerRendererMixin(EntityRendererProvider.Context context,
                                PlayerModel<AbstractClientPlayer> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void jojoha$addAwakeningGlowLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        this.addLayer(new StandAwakeningGlowLayer(this));

        this.addLayer(new StandBoundArmsLayer(this));
        this.addLayer(new StoneMaskLayer(this, context.bakeLayer(StoneMaskModel.LAYER)));

        this.addLayer(new SubzeroChillLayer(this));
        this.addLayer(new PillarBodyLayer(this));
        this.addLayer(new UltimateWingsLayer(this));
        this.addLayer(new LimbRegrowLayer(this));
        this.addLayer(new AccessoryLayer(this, context.getModelSet()));
        this.addLayer(new HamonHealHand(this));

        FleshBudFx.bake(
                context.bakeLayer(FleshBudModel.LAYER));
    }
    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void jojoha$meltedIntoGoop(AbstractClientPlayer player, float entityYaw, float partialTick,
                                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                       CallbackInfo ci) {
        if (GoopBody.hides(player)) {
            ci.cancel();
            return;
        }
        if (HeadlessView.render(player, partialTick, poseStack, bufferSource, packedLight)) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float jojoha$holdPilotBody(float entityYaw, AbstractClientPlayer player) {
        float[] held = PilotPose.of(player);
        return held == null ? entityYaw : held[0];
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void jojoha$holdPilotHead(AbstractClientPlayer player, float entityYaw, float partialTick,
                                      PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, CallbackInfo ci) {
        float[] held = PilotPose.of(player);
        if (held == null) {
            return;
        }

        jojoha$heldPose = new float[]{player.yHeadRot, player.yHeadRotO,
                player.getXRot(), player.xRotO, player.yBodyRot, player.yBodyRotO};

        player.yHeadRot = held[1];
        player.yHeadRotO = held[1];
        player.setXRot(held[2]);
        player.xRotO = held[2];
        player.yBodyRot = held[0];
        player.yBodyRotO = held[0];
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("RETURN"))
    private void jojoha$releasePilotHead(AbstractClientPlayer player, float entityYaw, float partialTick,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, CallbackInfo ci) {
        if (jojoha$heldPose == null) {
            return;
        }

        player.yHeadRot = jojoha$heldPose[0];
        player.yHeadRotO = jojoha$heldPose[1];
        player.setXRot(jojoha$heldPose[2]);
        player.xRotO = jojoha$heldPose[3];
        player.yBodyRot = jojoha$heldPose[4];
        player.yBodyRotO = jojoha$heldPose[5];
        jojoha$heldPose = null;
    }

    @Inject(
            method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("TAIL"))
    private void jojoha$renderAwakeningRays(AbstractClientPlayer player, float entityYaw, float partialTick,
                                            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                            CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float clientTime = (float) minecraft.level.getGameTime() + partialTick;
        AwakeningLight.chest(player, poseStack, clientTime);
        StandAfterimages.renderTrail(player, getModel(), poseStack, bufferSource, partialTick,
                minecraft.level.getGameTime());
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/player/PlayerRenderer;"
                            + "setModelProperties(Lnet/minecraft/client/player/AbstractClientPlayer;)V",
                    shift = At.Shift.AFTER))
    private void jojoha$hideExtendedArm(AbstractClientPlayer player, float entityYaw,
                                        float partialTick, PoseStack poseStack,
                                        MultiBufferSource buffers, int packedLight,
                                        CallbackInfo ci) {
        if (!ZoomPunchArm.hidesArm(player, partialTick)) {
            return;
        }

        jojoha$punchingArm = player.getMainArm();
        PlayerModel<AbstractClientPlayer> model = getModel();
        boolean right = jojoha$punchingArm == HumanoidArm.RIGHT;
        (right ? model.rightArm : model.leftArm).visible = false;
        (right ? model.rightSleeve : model.leftSleeve).visible = false;
    }

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void jojoha$showExtendedArm(AbstractClientPlayer player, float entityYaw,
                                        float partialTick, PoseStack poseStack,
                                        MultiBufferSource buffers, int packedLight,
                                        CallbackInfo ci) {
        if (jojoha$punchingArm == null) {
            return;
        }

        PlayerModel<AbstractClientPlayer> model = getModel();
        boolean right = jojoha$punchingArm == HumanoidArm.RIGHT;
        (right ? model.rightArm : model.leftArm).visible = true;
        (right ? model.rightSleeve : model.leftSleeve).visible = true;
        jojoha$punchingArm = null;
    }

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void jojoha$enterHandPass(PoseStack poseStack, MultiBufferSource buffers, int light,
                                      AbstractClientPlayer player,
                                      ModelPart arm,
                                      ModelPart sleeve, CallbackInfo ci) {
        PlayerStabAnimation.handPass(true);
    }

    @Inject(method = "renderHand", at = @At("RETURN"))
    private void jojoha$leaveHandPass(PoseStack poseStack, MultiBufferSource buffers, int light,
                                      AbstractClientPlayer player,
                                      ModelPart arm,
                                      ModelPart sleeve, CallbackInfo ci) {
        PlayerStabAnimation.handPass(false);
    }

    @Inject(method = "renderRightHand", at = @At("TAIL"))
    private void jojoha$standOnRightArm(PoseStack poseStack, MultiBufferSource buffers, int light,
                                        AbstractClientPlayer player, CallbackInfo ci) {
        StandFirstPersonArms.render(poseStack, buffers, light, player, HumanoidArm.RIGHT,
                getModel().rightArm);
        AccessoryLayer.firstPerson(poseStack, buffers, light, player,
                getModel().rightArm, true);
        PillarBodyLayer.firstPerson(poseStack, buffers, player, getModel().rightArm, true);
        HamonHealHand.firstPerson(poseStack, buffers, player, getModel().rightArm, true);
    }

    @Inject(method = "renderLeftHand", at = @At("TAIL"))
    private void jojoha$standOnLeftArm(PoseStack poseStack, MultiBufferSource buffers, int light,
                                       AbstractClientPlayer player, CallbackInfo ci) {
        StandFirstPersonArms.render(poseStack, buffers, light, player, HumanoidArm.LEFT,
                getModel().leftArm);
        AccessoryLayer.firstPerson(poseStack, buffers, light, player,
                getModel().leftArm, false);
        PillarBodyLayer.firstPerson(poseStack, buffers, player, getModel().leftArm, false);
        HamonHealHand.firstPerson(poseStack, buffers, player, getModel().leftArm, false);
    }
}
