package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.RedGlint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public abstract class RedGlintMixin {

    private static final TagKey<Item> JOJOHA$RED_GLINT = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "red_glint"));

    private static ItemStack jojoha$current;

    @Inject(method = "render", at = @At("HEAD"))
    private void jojoha$rememberStack(ItemStack stack, ItemDisplayContext context, boolean leftHand,
                                      PoseStack poseStack, MultiBufferSource buffers, int light,
                                      int overlay, BakedModel model, CallbackInfo ci) {
        jojoha$current = stack;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void jojoha$forgetStack(ItemStack stack, ItemDisplayContext context, boolean leftHand,
                                    PoseStack poseStack, MultiBufferSource buffers, int light,
                                    int overlay, BakedModel model, CallbackInfo ci) {
        jojoha$current = null;
    }

    private static boolean jojoha$isRed() {
        return jojoha$current != null && jojoha$current.is(JOJOHA$RED_GLINT);
    }

    @Inject(method = "getFoilBuffer", at = @At("HEAD"), cancellable = true)
    private static void jojoha$redFoil(MultiBufferSource source, RenderType type, boolean isItem,
                                        boolean glint, CallbackInfoReturnable<VertexConsumer> cir) {
        if (!glint || !jojoha$isRed()) return;
        cir.setReturnValue(VertexMultiConsumer.create(
                source.getBuffer(RedGlint.ITEM), source.getBuffer(type)));
    }

    @Inject(method = "getFoilBufferDirect", at = @At("HEAD"), cancellable = true)
    private static void jojoha$redFoilDirect(MultiBufferSource source, RenderType type,
                                              boolean isItem, boolean glint,
                                              CallbackInfoReturnable<VertexConsumer> cir) {
        if (!glint || !jojoha$isRed()) return;
        cir.setReturnValue(VertexMultiConsumer.create(
                source.getBuffer(RedGlint.DIRECT), source.getBuffer(type)));
    }
}
