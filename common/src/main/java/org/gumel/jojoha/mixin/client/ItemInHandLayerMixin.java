package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.client.ClackerHand;
import org.gumel.jojoha.client.UltimateWingsLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void jojoha$hideUnderWings(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                       HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, int light,
                                       CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer player
                && UltimateWingsLayer.armless(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem("
                    + "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;"
                    + "Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;"
                    + "Lnet/minecraft/client/renderer/MultiBufferSource;I)V"))
    private void jojoha$gripClackers(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                     HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffer, int light,
                                     CallbackInfo ci) {
        ClackerHand.heldInThirdPerson(entity, stack, arm, poseStack);
    }
}
