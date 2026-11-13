package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.client.ClackerHand;
import org.gumel.jojoha.client.HeadlessView;
import org.gumel.jojoha.client.ManifestCamera;
import org.gumel.jojoha.client.PilotView;
import org.gumel.jojoha.client.PossessionClient;
import org.gumel.jojoha.client.RapierBrace;
import org.gumel.jojoha.client.StandSkillInput;
import org.gumel.jojoha.client.UltimateWingsLayer;
import org.gumel.jojoha.client.fp.FirstPersonArms;
import org.gumel.jojoha.client.pillar.GoopBody;
import org.gumel.jojoha.item.ClackersItem;
import org.gumel.jojoha.vampire.Headless;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow
    private void renderPlayerArm(PoseStack poseStack, MultiBufferSource buffer, int combinedLight,
                                 float equippedProgress, float swingProgress,
                                 HumanoidArm arm) {
        throw new AssertionError();
    }

    @Inject(method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;"
            + "Lnet/minecraft/client/player/LocalPlayer;I)V",
            at = @At("HEAD"), cancellable = true)
    private void jojoha$poseOrHideHands(float partialTicks, PoseStack poseStack,
                                        MultiBufferSource.BufferSource buffer,
                                        LocalPlayer player, int combinedLight,
                                        CallbackInfo ci) {
        ClackerHand.handsBegin(poseStack);
        if (ManifestCamera.detaches(partialTicks)) {
            ci.cancel();
            return;
        }
        if (GoopBody.hides(player)
                || UltimateWingsLayer.armless(player)) {
            ci.cancel();
            return;
        }
        if (HeadlessView.state(player) != Headless.FREE) {
            ci.cancel();
            return;
        }
        if (StandSkillInput.isPiloting() || PossessionClient.possessing()) {
            if (PilotView.render(poseStack, buffer, partialTicks)) {
                buffer.endBatch();
            }
            ci.cancel();
            return;
        }

        if (FirstPersonArms.render(poseStack, buffer, combinedLight, player,
                (ItemInHandRenderer) (Object) this)) {
            buffer.endBatch();
            ci.cancel();
        }
    }

    @Inject(method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FF"
            + "Lnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;F"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void jojoha$braceRapier(AbstractClientPlayer player,
                                    float partialTicks, float pitch,
                                    InteractionHand hand, float swingProgress,
                                    ItemStack stack,
                                    float equippedProgress, PoseStack poseStack,
                                    MultiBufferSource buffer, int combinedLight,
                                    CallbackInfo ci) {
        if (RapierBrace.renderFirstPerson(
                (ItemInHandRenderer) (Object) this, player, hand,
                stack, equippedProgress, poseStack, buffer, combinedLight)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FF"
            + "Lnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;F"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void jojoha$holdClackers(AbstractClientPlayer player,
                                     float partialTicks, float pitch,
                                     InteractionHand hand, float swingProgress,
                                     ItemStack stack,
                                     float equippedProgress, PoseStack poseStack,
                                     MultiBufferSource buffer, int combinedLight,
                                     CallbackInfo ci) {
        if (!(stack.getItem() instanceof ClackersItem)) {
            return;
        }
        HumanoidArm arm = ClackerHand.arm(player, hand);
        poseStack.pushPose();
        if (!player.isInvisible()) {
            this.renderPlayerArm(poseStack, buffer, combinedLight, equippedProgress, swingProgress, arm);
        }
        ClackerHand.heldInFirstPerson(poseStack, arm);
        poseStack.popPose();
        ci.cancel();
    }
}
