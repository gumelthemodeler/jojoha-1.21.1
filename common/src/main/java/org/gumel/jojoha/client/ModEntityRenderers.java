package org.gumel.jojoha.client;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.level.block.Block;
import org.gumel.jojoha.pillar.PillarBirth;
import org.gumel.jojoha.registry.ModBlocks;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.supporter.SupporterChoice;

import java.util.List;

public final class ModEntityRenderers {
    private static final int PHANTOM_MOSS_ITEM_TINT = 0xFF893F7A;

    private static boolean modelsRegistered;

    private ModEntityRenderers() {
    }

    public static void registerModels() {
        if (modelsRegistered) {
            return;
        }
        modelsRegistered = true;

        EntityModelLayerRegistry.register(
                StoneMaskModel.LAYER, StoneMaskModel::createBodyLayer);
        EntityModelLayerRegistry.register(
                FleshBudModel.LAYER, FleshBudModel::createBodyLayer);
        EntityModelLayerRegistry.register(
                BubbleGlovesModel.LAYER, BubbleGlovesModel::createBodyLayer);
        EntityModelLayerRegistry.register(
                ScarfModels.RING, ScarfModels::createRingLayer);

        for (ArmorOutfits.Outfit outfit : ArmorOutfits.all()) {
            EntityModelLayerRegistry.register(
                    outfit.layer(), outfit.mesh()::get);
        }

        EntityRendererRegistry.register(ModRegistries.STAND, StandRenderer::new);
        EntityRendererRegistry.register(ModRegistries.WILLOW, WillowRenderer::new);
        EntityRendererRegistry.register(ModRegistries.RISEN_ZOMBIE, RisenZombieRenderer::new);
        EntityRendererRegistry.register(ModRegistries.AMALGAMATION, AmalgamationRenderer::new);
        EntityRendererRegistry.register(ModRegistries.GHOST, GhostRenderer::new);
        EntityRendererRegistry.register(ModRegistries.VAMPIRE_RAT, VampireRatRenderer::new);
        EntityRendererRegistry.register(ModRegistries.VAMPIRE, VampireRenderer::new);
        EntityRendererRegistry.register(ModRegistries.HAMON_STUDENT, NpcGeoRenderer::new);
        EntityRendererRegistry.register(ModRegistries.JOTARO, NpcGeoRenderer::new);
        EntityRendererRegistry.register(ModRegistries.JOSEPH, NpcGeoRenderer::new);
        EntityRendererRegistry.register(ModRegistries.AVDOL, NpcGeoRenderer::new);
        EntityRendererRegistry.register(ModRegistries.KAKYOIN, NpcGeoRenderer::new);
        EntityRendererRegistry.register(ModRegistries.POLNAREFF, NpcGeoRenderer::new);
        EntityRendererRegistry.register(ModRegistries.STAND_USER, NpcRenderer::new);
        EntityRendererRegistry.register(ModRegistries.FALLING_MASK, FallingMaskRenderer::new);
        EntityRendererRegistry.register(ModRegistries.SEVERED_LIMB, SeveredLimbRenderer::new);

        EntityRendererRegistry.register(ModRegistries.THROWN_DAGGER, ThrownDaggerRenderer::new);
        EntityRendererRegistry.register(ModRegistries.THROWN_TRINKET, ThrownItemRenderer::new);
        EntityRendererRegistry.register(ModRegistries.STAND_KNIFE, ThrownDaggerRenderer::new);
        EntityRendererRegistry.register(ModRegistries.EMERALD_SPLASH, EmeraldSplashRenderer::new);
        EntityRendererRegistry.register(ModRegistries.CHARIOT_BLADE, ChariotBladeRenderer::new);
        EntityRendererRegistry.register(ModRegistries.STAND_ARROW_PROJECTILE, StandArrowRenderer::new);

        EntityModelLayerRegistry.register(
                HermitGrappleModels.ROPE, HermitGrappleModels::createRopeLayer);
        EntityModelLayerRegistry.register(
                HermitGrappleModels.HOOK, HermitGrappleModels::createHookLayer);
        EntityRendererRegistry.register(ModRegistries.GRAPPLE_HOOK, HermitGrappleRenderer::new);

        EntityModelLayerRegistry.register(
                RespawnTotemModel.LAYER, RespawnTotemModel::createBodyLayer);

        EntityModelLayerRegistry.register(
                ClackerModels.HEAD, ClackerModels::createHeadLayer);
        EntityModelLayerRegistry.register(
                ClackerModels.STRING, ClackerModels::createStringLayer);
    }

    public static void init() {
        AccessoryPanel.init();
        SupporterChoice.init();
        SupporterLockScreen.register();
        AnubisPossession.init();
        HeadlessView.init();
        PillarBirth.clientView = Gore::reforming;
        RenderTypeRegistry.register(
                RenderType.cutout(), ModRegistries.HEAD_JAR_BLOCK.get());
        RenderTypeRegistry.register(
                RenderType.cutout(), ModRegistries.MAGICIAN_FLAME.get());
        RenderTypeRegistry.register(
                RenderType.translucent(), ModRegistries.CATACOMB_FOG.get());
        RenderTypeRegistry.register(
                RenderType.cutout(),
                ModBlocks.BOSS_SUMMONING_TOTEM.get());
        RenderTypeRegistry.register(
                RenderType.cutoutMipped(),
                ModBlocks.PHANTOM_LEAVES.get());
        RenderTypeRegistry.register(
                RenderType.cutout(),
                ModBlocks.PHANTOM_VINES.get());

        for (RegistrySupplier<Block> plant :
                List.of(ModBlocks.PHANTOM_SHORT_GRASS,
                        ModBlocks.PHANTOM_TALL_GRASS,
                        ModBlocks.DUSKWEED,
                        ModBlocks.BLOODVINE,
                        ModBlocks.SUNLEAF)) {
            RenderTypeRegistry.register(
                    RenderType.cutout(), plant.get());
        }

        RenderTypeRegistry.register(
                RenderType.cutout(),
                ModBlocks.PHANTOM_LEAF_PILE.get());
        RenderTypeRegistry.register(
                RenderType.cutout(),
                ModBlocks.PHANTOM_ROCK_PILE.get());
        RenderTypeRegistry.register(
                RenderType.cutout(),
                ModBlocks.DUNGEON_SPAWNER.get());
        RenderTypeRegistry.register(
                RenderType.cutout(),
                ModBlocks.DEEPSLATE_TRAPDOOR.get());

        ColorHandlerRegistry.registerBlockColors(
                (state, view, pos, tint) -> view != null && pos != null
                        ? BiomeColors.getAverageGrassColor(view, pos)
                        : PHANTOM_MOSS_ITEM_TINT,
                ModBlocks.PHANTOM_MOSS.get());

        for (RegistrySupplier<Block> grass :
                List.of(ModBlocks.PHANTOM_SHORT_GRASS,
                        ModBlocks.PHANTOM_TALL_GRASS)) {
            ColorHandlerRegistry.registerBlockColors(
                    (state, view, pos, tint) -> view != null && pos != null
                            ? BiomeColors.getAverageGrassColor(view, pos)
                            : PHANTOM_MOSS_ITEM_TINT,
                    grass.get());
            ColorHandlerRegistry.registerItemColors(
                    (stack, tint) -> PHANTOM_MOSS_ITEM_TINT, grass.get().asItem());
        }

        ColorHandlerRegistry.registerItemColors(
                (stack, tint) -> PHANTOM_MOSS_ITEM_TINT,
                ModBlocks.PHANTOM_MOSS.get().asItem());

        SoulCardColors.register();
        SandShade.register();

        registerModels();

        BlockEntityRendererRegistry.register(
                ModRegistries.CAMERA_BLOCK_ENTITY.get(), context -> new CameraBlockRenderer());

        BlockEntityRendererRegistry.register(
                ModRegistries.RESPAWN_TOTEM_BLOCK_ENTITY.get(), RespawnTotemRenderer::new);

        BlockEntityRendererRegistry.register(
                ModRegistries.HEAD_JAR_BLOCK_ENTITY.get(), HeadJarRenderer::new);
    }
}
