package org.gumel.jojoha.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import org.gumel.jojoha.registry.ModRegistries;

/** Client-only entity renderer registration - called from each platform's client entrypoint. */
public final class ModEntityRenderers {
    /**
     * What the moss is drawn as when there is no world to ask.
     *
     * <p>The biome's own grass colour, repeated here because an item in a slot has no position and
     * so no biome. Kept in step with the value in the biome JSON by hand, which is the one place two
     * numbers have to agree in this set.
     */
    private static final int PHANTOM_MOSS_ITEM_TINT = 0x893F7A;

    private ModEntityRenderers() {
    }

    public static void init() {
        // Both sheets have hard alpha and no partial pixels - measured, 51 clear in the leaves and
        // 76 in the vines with nothing in between - so a cutout is exactly right and translucency
        // would cost sorting for nothing. Mipped for the leaves because they are seen at distance in
        // bulk; the vines are single strands close up and gain nothing from it.
        dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                net.minecraft.client.renderer.RenderType.cutoutMipped(),
                org.gumel.jojoha.registry.ModBlocks.PHANTOM_LEAVES.get());
        dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                net.minecraft.client.renderer.RenderType.cutout(),
                org.gumel.jojoha.registry.ModBlocks.PHANTOM_VINES.get());

        // The plants are all crossed quads with clear space around them.
        for (dev.architectury.registry.registries.RegistrySupplier<net.minecraft.world.level.block.Block> plant :
                java.util.List.of(org.gumel.jojoha.registry.ModBlocks.PHANTOM_SHORT_GRASS,
                        org.gumel.jojoha.registry.ModBlocks.PHANTOM_TALL_GRASS,
                        org.gumel.jojoha.registry.ModBlocks.DUSKWEED,
                        org.gumel.jojoha.registry.ModBlocks.BLOODVINE,
                        org.gumel.jojoha.registry.ModBlocks.SUNLEAF)) {
            dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                    net.minecraft.client.renderer.RenderType.cutout(), plant.get());
        }

        // Both piles are flat quads with clear space around the shapes, so without a cutout the
        // whole 16 by 16 tile draws as an opaque square. Unmipped: they are small things seen from
        // a few blocks away, and mipping a sparse sheet eats the shapes at distance.
        dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                net.minecraft.client.renderer.RenderType.cutout(),
                org.gumel.jojoha.registry.ModBlocks.PHANTOM_LEAF_PILE.get());
        dev.architectury.registry.client.rendering.RenderTypeRegistry.register(
                net.minecraft.client.renderer.RenderType.cutout(),
                org.gumel.jojoha.registry.ModBlocks.PHANTOM_ROCK_PILE.get());

        // The moss sheet is the one greyscale texture in the set - a flat value map, measured at
        // zero saturation - so it is coloured here rather than in the art. Asking the biome for its
        // grass colour means the moss and the ground it sits on can never disagree: retune the
        // biome's violet and both move together.
        //
        // The fallback is the biome's colour taken from a position rather than a constant, and the
        // null case is an item in an inventory, which has no position to ask about.
        dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerBlockColors(
                (state, view, pos, tint) -> view != null && pos != null
                        ? net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(view, pos)
                        : PHANTOM_MOSS_ITEM_TINT,
                org.gumel.jojoha.registry.ModBlocks.PHANTOM_MOSS.get());

        // The grasses are the greyscale sheets in this set - measured at 0.017 saturation against
        // 0.55 for the grass block's own art - so they are the ones the biome has to colour. The
        // turf itself no longer appears here: it is painted now, and multiplying a tint into art
        // that already has its colour would only darken it.
        for (dev.architectury.registry.registries.RegistrySupplier<net.minecraft.world.level.block.Block> grass :
                java.util.List.of(org.gumel.jojoha.registry.ModBlocks.PHANTOM_SHORT_GRASS,
                        org.gumel.jojoha.registry.ModBlocks.PHANTOM_TALL_GRASS)) {
            dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerBlockColors(
                    (state, view, pos, tint) -> view != null && pos != null
                            ? net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(view, pos)
                            : PHANTOM_MOSS_ITEM_TINT,
                    grass.get());
            dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerItemColors(
                    (stack, tint) -> PHANTOM_MOSS_ITEM_TINT, grass.get().asItem());
        }

        dev.architectury.registry.client.rendering.ColorHandlerRegistry.registerItemColors(
                (stack, tint) -> PHANTOM_MOSS_ITEM_TINT,
                org.gumel.jojoha.registry.ModBlocks.PHANTOM_MOSS.get().asItem());

        // Registered before the renderers that bake from it.
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                StoneMaskModel.LAYER, StoneMaskModel::createBodyLayer);

        // The armour is a model rather than a painted sheet, so its layers have to be baked like any
        // other - see MeteoriteArmorLayerMixin, which draws whichever one the piece is wearing.
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                MeteoriteArmorModel.LAYER, MeteoriteArmorModel::createBodyLayer);
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                JotaroOutfitModel.LAYER, JotaroOutfitModel::createBodyLayer);

        EntityRendererRegistry.register(ModRegistries.STAND, StandRenderer::new);
        EntityRendererRegistry.register(ModRegistries.FALLING_MASK, FallingMaskRenderer::new);

        // Drawn as the item it is, rather than through a model of its own. The daggers are flat
        // sprites and a thrown one is the same object mid-air, so the sprite is the honest picture
        // of it - and it means a dagger added later needs a texture and nothing else.
        EntityRendererRegistry.register(ModRegistries.THROWN_DAGGER, ThrownDaggerRenderer::new);

        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                HermitGrappleModels.ROPE, HermitGrappleModels::createRopeLayer);
        dev.architectury.registry.client.level.entity.EntityModelLayerRegistry.register(
                HermitGrappleModels.HOOK, HermitGrappleModels::createHookLayer);
        EntityRendererRegistry.register(ModRegistries.GRAPPLE_HOOK, HermitGrappleRenderer::new);

        // The placed camera, drawn from its own animated model - see CameraBlockRenderer.
        dev.architectury.registry.client.rendering.BlockEntityRendererRegistry.register(
                ModRegistries.CAMERA_BLOCK_ENTITY.get(), context -> new CameraBlockRenderer());
    }
}
