package org.gumel.jojoha.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.skill.moves.CameraCrushSkill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ItemInHandRenderer.class)
public abstract class PhotographMapMixin {
    private static final RenderType JOJOHA$PHOTO_PAPER = RenderType.text(
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/map/photo_background.png"));

    private static final float JOJOHA$INSET = 0.78F;
    private static final float JOJOHA$OFF_X = 14F;
    private static final float JOJOHA$OFF_Y = 6F;

    @WrapOperation(method = "renderMap(Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I"
            + "Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/MapRenderer;render"
                            + "(Lcom/mojang/blaze3d/vertex/PoseStack;"
                            + "Lnet/minecraft/client/renderer/MultiBufferSource;"
                            + "Lnet/minecraft/world/level/saveddata/maps/MapId;"
                            + "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;ZI)V"))
    private void jojoha$insetPicture(MapRenderer renderer, PoseStack poseStack,
                                     MultiBufferSource buffers, MapId mapId,
                                     MapItemSavedData data, boolean active, int light,
                                     Operation<Void> original,
                                     @Local(argsOnly = true) ItemStack stack) {
        if (!jojoha$isPhotograph(stack)) {
            original.call(renderer, poseStack, buffers, mapId, data, active, light);
            return;
        }

        poseStack.pushPose();
        poseStack.translate(JOJOHA$OFF_X, JOJOHA$OFF_Y, 0F);
        poseStack.scale(JOJOHA$INSET, JOJOHA$INSET, 1F);
        original.call(renderer, poseStack, buffers, mapId, data, active, light);
        poseStack.popPose();
    }

    @ModifyArg(method = "renderMap(Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;I"
            + "Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MultiBufferSource;getBuffer"
                            + "(Lnet/minecraft/client/renderer/RenderType;)"
                            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private RenderType jojoha$photoPaper(RenderType original,
                                         @Local(argsOnly = true) ItemStack stack) {
        return jojoha$isPhotograph(stack) ? JOJOHA$PHOTO_PAPER : original;
    }

    private static boolean jojoha$isPhotograph(ItemStack stack) {
        CustomModelData model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return model != null && model.value() == CameraCrushSkill.PHOTOGRAPH_MODEL;
    }
}
