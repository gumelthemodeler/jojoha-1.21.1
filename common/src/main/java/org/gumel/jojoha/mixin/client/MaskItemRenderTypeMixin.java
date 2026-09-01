package org.gumel.jojoha.mixin.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.Jojoha;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws both sides of the items in {@code #jojoha:no_cull}, so you cannot see out through them.
 *
 * <h2>What goes wrong without it</h2>
 *
 * <p>Every sheet vanilla hands an item is a culling one - {@code itemEntityTranslucentCull} and
 * {@code entityTranslucentCull} - so only faces pointing at the camera are drawn. That is right for
 * a solid item and wrong for a hollow one. The Stone Mask is a shell whose sheet is nine tenths
 * clear, so looking into it you were looking through the gaps and straight out the far side into the
 * world, because the far side's inner faces had been culled away.
 *
 * <p>Swapping in a no-cull cutout draws those inner faces. Cutout rather than translucent because
 * the alpha was measured and it is hard - 372 opaque pixels of 4096 and nothing in between - so
 * there is nothing to blend and an alpha test does the job without paying for sorting.
 *
 * <p>Items and blocks share one atlas, which is why the block atlas is the texture named here.
 */
@Mixin(ItemBlockRenderTypes.class)
public abstract class MaskItemRenderTypeMixin {

    private static final TagKey<Item> JOJOHA$NO_CULL = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "no_cull"));

    private static final RenderType JOJOHA$SHEET =
            RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);

    @Inject(method = "getRenderType(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/client/renderer/RenderType;",
            at = @At("HEAD"), cancellable = true)
    private static void jojoha$noCull(ItemStack stack, boolean blend,
                                      CallbackInfoReturnable<RenderType> cir) {
        if (stack.is(JOJOHA$NO_CULL)) cir.setReturnValue(JOJOHA$SHEET);
    }
}
