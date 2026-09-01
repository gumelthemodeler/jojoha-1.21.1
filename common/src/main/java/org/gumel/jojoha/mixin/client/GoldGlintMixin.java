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
import org.gumel.jojoha.client.GoldGlint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives the items in {@code #jojoha:gold_glint} a golden enchantment glint instead of the purple one.
 *
 * <h2>How the item reaches the decision</h2>
 *
 * <p>The two methods that pick a glint buffer are handed a render type and two booleans and never
 * see the stack, so the stack has to be carried to them. It is recorded on the way into
 * {@code render} and cleared on the way out, which is safe here for one specific reason: this is all
 * on the render thread, one item at a time, and the buffer is chosen synchronously inside that same
 * call. Nothing is deferred, so nothing can read a stale value.
 *
 * <p>That the <em>drawing</em> happens later is fine and is the whole design - by then the vertices
 * are already in the gold type's bucket and will be drawn with its sheet whenever that bucket is
 * emptied. This is exactly why a render type is used rather than rebinding a texture around the call.
 *
 * <h2>Why a tag</h2>
 *
 * <p>Which items glint gold is a list that will change, and a list that changes belongs in data. The
 * tag also means someone can add to it from a datapack without touching this.
 */
@Mixin(ItemRenderer.class)
public abstract class GoldGlintMixin {

    private static final TagKey<Item> JOJOHA$GOLD_GLINT = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "gold_glint"));

    /** The stack currently being drawn, or null between items. Render thread only. */
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

    private static boolean jojoha$isGold() {
        return jojoha$current != null && jojoha$current.is(JOJOHA$GOLD_GLINT);
    }

    @Inject(method = "getFoilBuffer", at = @At("HEAD"), cancellable = true)
    private static void jojoha$goldFoil(MultiBufferSource source, RenderType type, boolean isItem,
                                        boolean glint, CallbackInfoReturnable<VertexConsumer> cir) {
        if (!glint || !jojoha$isGold()) return;
        // The same pairing vanilla makes: the glint pass over the item's own pass, drawn together.
        cir.setReturnValue(VertexMultiConsumer.create(
                source.getBuffer(GoldGlint.ITEM), source.getBuffer(type)));
    }

    @Inject(method = "getFoilBufferDirect", at = @At("HEAD"), cancellable = true)
    private static void jojoha$goldFoilDirect(MultiBufferSource source, RenderType type,
                                              boolean isItem, boolean glint,
                                              CallbackInfoReturnable<VertexConsumer> cir) {
        if (!glint || !jojoha$isGold()) return;
        cir.setReturnValue(VertexMultiConsumer.create(
                source.getBuffer(GoldGlint.DIRECT), source.getBuffer(type)));
    }
}
