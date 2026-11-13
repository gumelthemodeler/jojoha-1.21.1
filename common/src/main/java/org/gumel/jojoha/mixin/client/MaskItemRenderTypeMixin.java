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
