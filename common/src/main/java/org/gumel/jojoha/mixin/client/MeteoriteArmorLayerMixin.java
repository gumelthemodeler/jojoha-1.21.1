package org.gumel.jojoha.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.ArmorOutfits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class MeteoriteArmorLayerMixin {

    private static final TagKey<Item> JOJOHA$METEORITE_ARMOR = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "meteorite_armor"));

    @Unique
    private int jojoha$depth;

    @Inject(method = "renderArmorPiece*", at = @At("HEAD"), cancellable = true)
    private void jojoha$renderOutfit(CallbackInfo ci,
                                     @Local(argsOnly = true) PoseStack poseStack,
                                     @Local(argsOnly = true) MultiBufferSource buffers,
                                     @Local(argsOnly = true) LivingEntity entity,
                                     @Local(argsOnly = true) EquipmentSlot slot,
                                     @Local(argsOnly = true) int light) {
        if (jojoha$depth == 0 && outfit(poseStack, buffers, entity, slot, light, jojoha$body())) {
            ci.cancel();
            return;
        }
        jojoha$depth++;
    }

    @Inject(method = "renderArmorPiece*", at = @At("RETURN"))
    private void jojoha$renderMeteorite(CallbackInfo ci,
                                        @Local(argsOnly = true) PoseStack poseStack,
                                        @Local(argsOnly = true) MultiBufferSource buffers,
                                        @Local(argsOnly = true) LivingEntity entity,
                                        @Local(argsOnly = true) EquipmentSlot slot,
                                        @Local(argsOnly = true) int light) {
        jojoha$depth = Math.max(0, jojoha$depth - 1);
        if (jojoha$depth == 0) {
            meteorite(poseStack, buffers, entity, slot, light, jojoha$body());
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private HumanoidModel<LivingEntity> jojoha$body() {
        return ((RenderLayer<LivingEntity, HumanoidModel<LivingEntity>>) (Object) this).getParentModel();
    }

    private static boolean outfit(PoseStack poseStack, MultiBufferSource buffers, LivingEntity entity,
                                  EquipmentSlot slot, int light, HumanoidModel<LivingEntity> posed) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (!ArmorOutfits.dressed(stack) || !worn(stack, slot)) {
            return false;
        }
        draw(poseStack, buffers, stack, slot, light, posed);
        return true;
    }

    private static void meteorite(PoseStack poseStack, MultiBufferSource buffers, LivingEntity entity,
                                  EquipmentSlot slot, int light, HumanoidModel<LivingEntity> posed) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.is(JOJOHA$METEORITE_ARMOR) && worn(stack, slot)) {
            draw(poseStack, buffers, stack, slot, light, posed);
        }
    }

    private static boolean worn(ItemStack stack, EquipmentSlot slot) {
        return stack.getItem() instanceof ArmorItem piece
                && piece.getEquipmentSlot() == slot;
    }

    private static void draw(PoseStack poseStack, MultiBufferSource buffers, ItemStack stack,
                             EquipmentSlot slot, int light, HumanoidModel<LivingEntity> posed) {
        ArmorOutfits.Outfit outfit = ArmorOutfits.forStack(stack);
        HumanoidModel<LivingEntity> model = ArmorOutfits.model(outfit);

        posed.copyPropertiesTo(model);
        ArmorOutfits.showOnly(model, slot);

        VertexConsumer buffer = ItemRenderer.getArmorFoilBuffer(buffers,
                RenderType.armorCutoutNoCull(outfit.texture()), stack.hasFoil());
        model.renderToBuffer(poseStack, buffer, light, OverlayTexture.NO_OVERLAY, -1);
    }
}
