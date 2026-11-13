package org.gumel.jojoha.registry;

import org.gumel.jojoha.entity.npc.CrusaderNpc;
import com.mojang.serialization.MapCodec;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.block.CameraBlock;
import org.gumel.jojoha.block.CameraBlockEntity;
import org.gumel.jojoha.block.HeadJarBlock;
import org.gumel.jojoha.block.HeadJarBlockEntity;
import org.gumel.jojoha.block.RespawnTotemBlockEntity;
import org.gumel.jojoha.body.SeveredLimb;
import org.gumel.jojoha.entity.CatacombGhost;
import org.gumel.jojoha.entity.Amalgamation;
import org.gumel.jojoha.entity.RisenZombie;
import org.gumel.jojoha.entity.StandKnife;
import org.gumel.jojoha.entity.VampireEntity;
import org.gumel.jojoha.entity.VampireRat;
import org.gumel.jojoha.entity.WillowEntity;
import org.gumel.jojoha.entity.npc.HamonStudent;
import org.gumel.jojoha.entity.npc.StandUserNpc;
import org.gumel.jojoha.item.FallingMask;
import org.gumel.jojoha.item.OutfitSmithingRecipe;
import org.gumel.jojoha.item.StandArrowProjectile;
import org.gumel.jojoha.item.ThrownDagger;
import org.gumel.jojoha.item.ThrownTrinket;
import org.gumel.jojoha.labyrinth.CatacombFogBlock;
import org.gumel.jojoha.labyrinth.DungeonSpawnerBlockEntity;
import org.gumel.jojoha.level.feature.BoneFragmentFeature;
import org.gumel.jojoha.level.feature.BoneOutcropFeature;
import org.gumel.jojoha.level.feature.DuneFeature;
import org.gumel.jojoha.level.feature.MeteorCrashFeature;
import org.gumel.jojoha.level.feature.PalmFeature;
import org.gumel.jojoha.level.feature.PhantomGrassConnectFeature;
import org.gumel.jojoha.level.feature.PillarShrineFeature;
import org.gumel.jojoha.level.feature.RibArchFeature;
import org.gumel.jojoha.level.feature.TemplateScatterFeature;
import org.gumel.jojoha.level.feature.TemplateTreeFeature;
import org.gumel.jojoha.level.placement.ClearOfCanopyPlacement;
import org.gumel.jojoha.level.placement.SpacedGridPlacement;
import org.gumel.jojoha.level.structure.BlockSwapProcessor;
import org.gumel.jojoha.level.structure.CatacombsStructure;
import org.gumel.jojoha.level.structure.ContainerLootProcessor;
import org.gumel.jojoha.level.structure.GroundTrimProcessor;
import org.gumel.jojoha.level.structure.OgreRoadPiece;
import org.gumel.jojoha.level.structure.OgreStreetStructure;
import org.gumel.jojoha.level.structure.SpawnerMobsProcessor;
import org.gumel.jojoha.level.structure.SurfaceTemplatePiece;
import org.gumel.jojoha.level.structure.SurfaceTemplateStructure;
import org.gumel.jojoha.level.structure.WillowHousePiece;
import org.gumel.jojoha.level.structure.WillowHouseStructure;
import org.gumel.jojoha.loot.BizarreFind;
import org.gumel.jojoha.loot.ConfigChance;
import org.gumel.jojoha.loot.StampPathFunction;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.chariot.ChariotBlade;
import org.gumel.jojoha.stand.grapple.HermitGrappleHook;
import org.gumel.jojoha.stand.hierophant.EmeraldChip;
import org.gumel.jojoha.stand.hierophant.EmeraldSparkle;
import org.gumel.jojoha.stand.hierophant.EmeraldSplash;
import org.gumel.jojoha.stand.magician.MagicianFlameBlock;

import java.util.List;
import java.util.Map;

public final class ModRegistries {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.ITEM);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.CREATIVE_MODE_TAB);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.ENTITY_TYPE);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.PARTICLE_TYPE);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.SOUND_EVENT);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.MOB_EFFECT);
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.BLOCK);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final DeferredRegister<RecipeSerializer<?>>
            RECIPE_SERIALIZERS = DeferredRegister.create(Jojoha.MOD_ID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeSerializer<
            OutfitSmithingRecipe>> OUTFIT_SMITHING =
            RECIPE_SERIALIZERS.register("apply_outfit",
                    OutfitSmithingRecipe.Serializer::new);

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.ARMOR_MATERIAL);

    public static final RegistrySupplier<ArmorMaterial> METEORITE_ARMOR =
            ARMOR_MATERIALS.register("meteorite", () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.HELMET, 4,
                            ArmorItem.Type.CHESTPLATE, 9,
                            ArmorItem.Type.LEGGINGS, 7,
                            ArmorItem.Type.BOOTS, 4,
                            ArmorItem.Type.BODY, 11),
                    18,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(
                            ModItems.METEORITE_INGOT.get()),
                    List.of(),
                    4.0F,
                    0.15F));

    public static final DeferredRegister<LootItemFunctionType<?>>
            LOOT_FUNCTIONS = DeferredRegister.create(Jojoha.MOD_ID, Registries.LOOT_FUNCTION_TYPE);

    public static final RegistrySupplier<LootItemFunctionType<
            StampPathFunction>> STAMP_PATH =
            LOOT_FUNCTIONS.register("stamp_path", () ->
                    new LootItemFunctionType<>(
                            StampPathFunction.CODEC));

    public static final DeferredRegister<LootItemConditionType>
            LOOT_CONDITIONS = DeferredRegister.create(Jojoha.MOD_ID, Registries.LOOT_CONDITION_TYPE);

    public static final RegistrySupplier<LootItemConditionType>
            CONFIG_CHANCE = LOOT_CONDITIONS.register("config_chance", () ->
                    new LootItemConditionType(
                            ConfigChance.CODEC));

    public static final RegistrySupplier<LootItemConditionType>
            BIZARRE_FIND = LOOT_CONDITIONS.register("bizarre_find", () ->
                    new LootItemConditionType(
                            BizarreFind.CODEC));

    public static final DeferredRegister<StructureType<?>>
            STRUCTURE_TYPES = DeferredRegister.create(Jojoha.MOD_ID, Registries.STRUCTURE_TYPE);
    public static final DeferredRegister<StructurePieceType>
            STRUCTURE_PIECES = DeferredRegister.create(Jojoha.MOD_ID, Registries.STRUCTURE_PIECE);

    public static final RegistrySupplier<StructureType<
            WillowHouseStructure>> WILLOW_HOUSE_STRUCTURE =
            STRUCTURE_TYPES.register("willow_house",
                    () -> () -> WillowHouseStructure.CODEC);
    public static final RegistrySupplier<StructureType<
            CatacombsStructure>> CATACOMBS_STRUCTURE =
            STRUCTURE_TYPES.register("catacombs",
                    () -> () -> CatacombsStructure.CODEC);
    public static final RegistrySupplier<StructureType<
            SurfaceTemplateStructure>> SURFACE_TEMPLATE_STRUCTURE =
            STRUCTURE_TYPES.register("surface_template",
                    () -> () -> SurfaceTemplateStructure.CODEC);
    public static final RegistrySupplier<StructurePieceType>
            SURFACE_TEMPLATE_PIECE = STRUCTURE_PIECES.register("surface_template",
                    () -> (StructurePieceType.StructureTemplateType)
                            SurfaceTemplatePiece::new);
    public static final RegistrySupplier<StructurePieceType>
            WILLOW_HOUSE_PIECE = STRUCTURE_PIECES.register("willow_house",
                    () -> (StructurePieceType.StructureTemplateType)
                            WillowHousePiece::new);
    public static final RegistrySupplier<StructureType<
            OgreStreetStructure>> OGRE_STREET_STRUCTURE =
            STRUCTURE_TYPES.register("ogre_street",
                    () -> () -> OgreStreetStructure.CODEC);
    public static final RegistrySupplier<StructurePieceType>
            OGRE_ROAD_PIECE = STRUCTURE_PIECES.register("ogre_road",
                    () -> (StructurePieceType.ContextlessType)
                            OgreRoadPiece::new);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.templatesystem
            .StructureProcessorType<?>> STRUCTURE_PROCESSORS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.STRUCTURE_PROCESSOR);
    public static final RegistrySupplier<net.minecraft.world.level.levelgen.structure.templatesystem
            .StructureProcessorType<ContainerLootProcessor>>
            CONTAINER_LOOT_PROCESSOR = STRUCTURE_PROCESSORS.register("container_loot",
                    () -> () -> ContainerLootProcessor.CODEC);
    public static final RegistrySupplier<net.minecraft.world.level.levelgen.structure.templatesystem
            .StructureProcessorType<SpawnerMobsProcessor>>
            SPAWNER_MOBS_PROCESSOR = STRUCTURE_PROCESSORS.register("spawner_mobs",
                    () -> () -> SpawnerMobsProcessor.CODEC);
    public static final RegistrySupplier<net.minecraft.world.level.levelgen.structure.templatesystem
            .StructureProcessorType<GroundTrimProcessor>>
            GROUND_TRIM_PROCESSOR = STRUCTURE_PROCESSORS.register("ground_trim",
                    () -> () -> GroundTrimProcessor.CODEC);
    public static final RegistrySupplier<StructureProcessorType<BlockSwapProcessor>>
            BLOCK_SWAP_PROCESSOR = STRUCTURE_PROCESSORS.register("block_swap",
                    () -> () -> BlockSwapProcessor.CODEC);

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.FEATURE);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.placement
            .PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.PLACEMENT_MODIFIER_TYPE);

    public static final RegistrySupplier<net.minecraft.world.level.levelgen.placement
            .PlacementModifierType<SpacedGridPlacement>>
            SPACED_GRID = PLACEMENT_MODIFIERS.register("spaced_grid",
                    () -> () -> SpacedGridPlacement.CODEC);

    public static final RegistrySupplier<net.minecraft.world.level.levelgen.placement
            .PlacementModifierType<ClearOfCanopyPlacement>>
            CLEAR_OF_CANOPY = PLACEMENT_MODIFIERS.register("clear_of_canopy",
                    () -> () -> ClearOfCanopyPlacement.CODEC);

    public static final RegistrySupplier<Feature<
            NoneFeatureConfiguration>>
            PHANTOM_GRASS_CONNECT = FEATURES.register("phantom_grass_connect", () ->
                    new PhantomGrassConnectFeature(
                            net.minecraft.world.level.levelgen.feature.configurations
                                    .NoneFeatureConfiguration.CODEC));

    public static final RegistrySupplier<Feature<
            NoneFeatureConfiguration>>
            METEOR_CRASH = FEATURES.register("meteor_crash", () ->
                    new MeteorCrashFeature(
                            net.minecraft.world.level.levelgen.feature.configurations
                                    .NoneFeatureConfiguration.CODEC));

    public static final RegistrySupplier<Feature<
            NoneFeatureConfiguration>>
            PILLAR_SHRINE = FEATURES.register("pillar_shrine", () ->
                    new PillarShrineFeature(
                            net.minecraft.world.level.levelgen.feature.configurations
                                    .NoneFeatureConfiguration.CODEC));

    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>>
            DUNE = FEATURES.register("dune", () -> new DuneFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>>
            RIB_ARCH = FEATURES.register("rib_arch", () -> new RibArchFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>>
            PALM = FEATURES.register("palm", () -> new PalmFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>>
            BONE_FRAGMENT = FEATURES.register("bone_fragment", () -> new BoneFragmentFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>>
            BONE_OUTCROP = FEATURES.register("bone_outcrop", () -> new BoneOutcropFeature(NoneFeatureConfiguration.CODEC));

    public static final RegistrySupplier<Feature<?>>
            TEMPLATE_TREE = FEATURES.register("template_tree", () ->
                    new TemplateTreeFeature());

    public static final RegistrySupplier<Feature<?>>
            TEMPLATE_SCATTER = FEATURES.register("template_scatter", () ->
                    new TemplateScatterFeature());

    public static final RegistrySupplier<Block> CAMERA_BLOCK =
            BLOCKS.register("camera", () -> new CameraBlock(
                    BlockBehaviour.Properties.of()
                            .strength(0.4F)
                            .sound(SoundType.GLASS)
                            .noOcclusion()));

    public static final RegistrySupplier<Block> CATACOMB_FOG =
            BLOCKS.register("catacomb_fog", () -> new CatacombFogBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.SNOW)
                            .strength(-1.0F, 3600000.0F)
                            .noOcclusion()
                            .forceSolidOn()
                            .isValidSpawn((state, level, pos, type) -> false)
                            .isRedstoneConductor((state, level, pos) -> false)
                            .isSuffocating((state, level, pos) -> false)
                            .isViewBlocking((state, level, pos) -> false)
                            .lightLevel(state -> 7)
                            .sound(SoundType.WOOL)
                            .pushReaction(PushReaction.BLOCK)
                            .noLootTable()));

    public static final RegistrySupplier<Block> MAGICIAN_FLAME =
            BLOCKS.register("magician_flame", () -> new MagicianFlameBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.FIRE)
                            .replaceable()
                            .noCollission()
                            .instabreak()
                            .lightLevel(state -> 15)
                            .sound(SoundType.WOOL)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()));

    public static final RegistrySupplier<Block> HEAD_JAR_BLOCK =
            BLOCKS.register("head_jar", () -> new HeadJarBlock(
                    BlockBehaviour.Properties.of()
                            .strength(0.3F)
                            .sound(SoundType.GLASS)
                            .noOcclusion()
                            .isSuffocating((state, level, pos) -> false)
                            .isViewBlocking((state, level, pos) -> false)));

    public static final RegistrySupplier<BlockEntityType<HeadJarBlockEntity>> HEAD_JAR_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("head_jar", () ->
                    BlockEntityType.Builder.of(
                            HeadJarBlockEntity::new,
                            HEAD_JAR_BLOCK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<CameraBlockEntity>> CAMERA_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("camera", () ->
                    BlockEntityType.Builder.of(
                            CameraBlockEntity::new,
                            CAMERA_BLOCK.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<RespawnTotemBlockEntity>> RESPAWN_TOTEM_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("respawn_totem", () ->
                    BlockEntityType.Builder.of(
                            RespawnTotemBlockEntity::new,
                            ModBlocks.RESPAWN_TOTEM.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<DungeonSpawnerBlockEntity>> DUNGEON_SPAWNER_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("dungeon_spawner", () ->
                    BlockEntityType.Builder.of(
                            DungeonSpawnerBlockEntity::new,
                            ModBlocks.DUNGEON_SPAWNER.get()).build(null));

    public static final RegistrySupplier<EntityType<FallingMask>> FALLING_MASK =
            ENTITY_TYPES.register("falling_mask", () ->
                    EntityType.Builder.<FallingMask>of(
                                    FallingMask::new, MobCategory.MISC)
                            .sized(0.6F, 0.5F)
                            .noSave()
                            .clientTrackingRange(6)
                            .updateInterval(1)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "falling_mask").toString()));

    public static final RegistrySupplier<EntityType<SeveredLimb>> SEVERED_LIMB =
            ENTITY_TYPES.register("severed_limb", () ->
                    EntityType.Builder.<SeveredLimb>of(
                                    SeveredLimb::new, MobCategory.MISC)
                            .sized(0.5F, 0.25F)
                            .clientTrackingRange(8)
                            .updateInterval(2)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "severed_limb").toString()));

    public static final RegistrySupplier<EntityType<ThrownDagger>> THROWN_DAGGER =
            ENTITY_TYPES.register("thrown_dagger", () ->
                    EntityType.Builder.<ThrownDagger>of(
                                    ThrownDagger::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "thrown_dagger").toString()));

    public static final RegistrySupplier<EntityType<ThrownTrinket>> THROWN_TRINKET =
            ENTITY_TYPES.register("thrown_trinket", () ->
                    EntityType.Builder.<ThrownTrinket>of(
                                    ThrownTrinket::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "thrown_trinket").toString()));

    public static final RegistrySupplier<EntityType<HermitGrappleHook>>
            GRAPPLE_HOOK = ENTITY_TYPES.register("hermit_grapple_hook", () ->
                    EntityType.Builder.<HermitGrappleHook>of(
                                    HermitGrappleHook::new,
                                    MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build(ResourceLocation.fromNamespaceAndPath(
                                    Jojoha.MOD_ID, "hermit_grapple_hook").toString()));

    public static final RegistrySupplier<EntityType<WillowEntity>> WILLOW =
            ENTITY_TYPES.register("willow", () ->
                    EntityType.Builder.of(WillowEntity::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.95F)
                            .clientTrackingRange(10)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "willow").toString()));

    public static final RegistrySupplier<EntityType<StandArrowProjectile>> STAND_ARROW_PROJECTILE =
            ENTITY_TYPES.register("stand_arrow", () ->
                    EntityType.Builder.<StandArrowProjectile>of(
                                    StandArrowProjectile::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .eyeHeight(0.13F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_arrow").toString()));

    public static final RegistrySupplier<EntityType<EmeraldSplash>> EMERALD_SPLASH =
            ENTITY_TYPES.register("emerald_splash", () ->
                    EntityType.Builder.<EmeraldSplash>of(
                                    EmeraldSplash::new, MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .clientTrackingRange(6)
                            .updateInterval(10)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "emerald_splash").toString()));

    public static final RegistrySupplier<EntityType<ChariotBlade>> CHARIOT_BLADE =
            ENTITY_TYPES.register("chariot_blade", () ->
                    EntityType.Builder.<ChariotBlade>of(
                                    ChariotBlade::new, MobCategory.MISC)
                            .sized(0.35F, 0.35F)
                            .clientTrackingRange(6)
                            .updateInterval(2)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "chariot_blade").toString()));

    public static final RegistrySupplier<EntityType<StandKnife>> STAND_KNIFE =
            ENTITY_TYPES.register("stand_knife", () ->
                    EntityType.Builder.<StandKnife>of(
                                    StandKnife::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_knife").toString()));

    public static final RegistrySupplier<EntityType<RisenZombie>> RISEN_ZOMBIE =
            ENTITY_TYPES.register("zombie", () ->
                    EntityType.Builder.<RisenZombie>of(
                                    RisenZombie::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .eyeHeight(1.74F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "zombie").toString()));

    public static final RegistrySupplier<EntityType<Amalgamation>> AMALGAMATION =
            ENTITY_TYPES.register("amalgamation", () ->
                    EntityType.Builder.<Amalgamation>of(
                                    Amalgamation::new, MobCategory.MONSTER)
                            .sized(1.4F, 2.6F)
                            .eyeHeight(2.25F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "amalgamation").toString()));

    public static final RegistrySupplier<EntityType<HamonStudent>> HAMON_STUDENT =
            ENTITY_TYPES.register("hamon_student", () ->
                    EntityType.Builder.<HamonStudent>of(HamonStudent::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .eyeHeight(1.62F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "hamon_student").toString()));

    public static final RegistrySupplier<EntityType<CrusaderNpc>> JOTARO =
            ENTITY_TYPES.register("jotaro", () ->
                    EntityType.Builder.<CrusaderNpc>of((type, level) -> new CrusaderNpc(type, level, CrusaderNpc.Crusader.JOTARO), MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .eyeHeight(1.62F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "jotaro").toString()));

    public static final RegistrySupplier<EntityType<CrusaderNpc>> JOSEPH =
            ENTITY_TYPES.register("joseph", () ->
                    EntityType.Builder.<CrusaderNpc>of((type, level) -> new CrusaderNpc(type, level, CrusaderNpc.Crusader.JOSEPH), MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .eyeHeight(1.62F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "joseph").toString()));

    public static final RegistrySupplier<EntityType<CrusaderNpc>> AVDOL =
            ENTITY_TYPES.register("avdol", () ->
                    EntityType.Builder.<CrusaderNpc>of((type, level) -> new CrusaderNpc(type, level, CrusaderNpc.Crusader.AVDOL), MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .eyeHeight(1.62F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "avdol").toString()));

    public static final RegistrySupplier<EntityType<CrusaderNpc>> KAKYOIN =
            ENTITY_TYPES.register("kakyoin", () ->
                    EntityType.Builder.<CrusaderNpc>of((type, level) -> new CrusaderNpc(type, level, CrusaderNpc.Crusader.KAKYOIN), MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .eyeHeight(1.62F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "kakyoin").toString()));

    public static final RegistrySupplier<EntityType<CrusaderNpc>> POLNAREFF =
            ENTITY_TYPES.register("polnareff", () ->
                    EntityType.Builder.<CrusaderNpc>of((type, level) -> new CrusaderNpc(type, level, CrusaderNpc.Crusader.POLNAREFF), MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .eyeHeight(1.62F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "polnareff").toString()));

    public static final RegistrySupplier<EntityType<StandUserNpc>> STAND_USER =
            ENTITY_TYPES.register("stand_user", () ->
                    EntityType.Builder.<StandUserNpc>of(StandUserNpc::new, MobCategory.CREATURE)
                            .sized(0.6F, 1.8F)
                            .eyeHeight(1.62F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_user").toString()));

    public static final RegistrySupplier<EntityType<VampireEntity>> VAMPIRE =
            ENTITY_TYPES.register("vampire", () ->
                    EntityType.Builder.<VampireEntity>of(
                                    VampireEntity::new, MobCategory.MONSTER)
                            .sized(0.6F, 1.95F)
                            .eyeHeight(1.74F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "vampire").toString()));

    public static final RegistrySupplier<EntityType<CatacombGhost>> GHOST =
            ENTITY_TYPES.register("ghost", () ->
                    EntityType.Builder.<CatacombGhost>of(
                                    CatacombGhost::new, MobCategory.MONSTER)
                            .sized(0.8F, 1.9F)
                            .eyeHeight(1.6F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ghost").toString()));

    public static final RegistrySupplier<EntityType<VampireRat>> VAMPIRE_RAT =
            ENTITY_TYPES.register("vampire_rat", () ->
                    EntityType.Builder.<VampireRat>of(
                                    VampireRat::new, MobCategory.MONSTER)
                            .sized(0.5F, 0.4F)
                            .eyeHeight(0.3F)
                            .clientTrackingRange(8)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "vampire_rat").toString()));

    public static final RegistrySupplier<EntityType<StandEntity>> STAND = ENTITY_TYPES.register("stand", () ->
            EntityType.Builder.of(StandEntity::new, MobCategory.MISC)
                    .sized(0.8F, 1.9F)
                    .noSave()
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand").toString()));

    public static final RegistrySupplier<SimpleParticleType> STAND_AURA =
            PARTICLE_TYPES.register("stand_aura", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> TIMESTOP_MOTE =
            PARTICLE_TYPES.register("timestop_mote", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> STAND_TRANSFORM =
            PARTICLE_TYPES.register("stand_transform", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> STUN =
            PARTICLE_TYPES.register("stun", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> ANUBIS_SLICE =
            PARTICLE_TYPES.register("anubis_slice", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> IMPACT_RING =
            PARTICLE_TYPES.register("impact_ring", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> HAMON_RIPPLE =
            PARTICLE_TYPES.register("hamon_ripple", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> BUBBLE_DRIFT =
            PARTICLE_TYPES.register("bubble_drift", () -> new SimpleParticleType(true) {
            });
    public static final RegistrySupplier<SimpleParticleType> BUBBLE_SHOT =
            PARTICLE_TYPES.register("bubble_shot", () -> new SimpleParticleType(true) {
            });
    public static final RegistrySupplier<SimpleParticleType> BUBBLE_SHEET =
            PARTICLE_TYPES.register("bubble_sheet", () -> new SimpleParticleType(true) {
            });
    public static final RegistrySupplier<SimpleParticleType> BUBBLE_CUT =
            PARTICLE_TYPES.register("bubble_cut", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> HAMON_SPARK =
            PARTICLE_TYPES.register("hamon_spark", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> HAMON_MOTE =
            PARTICLE_TYPES.register("hamon_mote", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> HAMON_SPARK_FINE =
            PARTICLE_TYPES.register("hamon_spark_fine", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<ParticleType<EmeraldSparkle>> EMERALD_SPARKLE =
            PARTICLE_TYPES.register("emerald_sparkle",
                    () -> new ParticleType<EmeraldSparkle>(false) {
                        @Override
                        public MapCodec<EmeraldSparkle> codec() {
                            return EmeraldSparkle.CODEC;
                        }

                        @Override
                        public StreamCodec<? super RegistryFriendlyByteBuf,
                                EmeraldSparkle> streamCodec() {
                            return EmeraldSparkle.STREAM_CODEC;
                        }
                    });

    public static final RegistrySupplier<ParticleType<EmeraldChip>> EMERALD_CHIP =
            PARTICLE_TYPES.register("emerald_chip",
                    () -> new ParticleType<EmeraldChip>(false) {
                        @Override
                        public MapCodec<EmeraldChip> codec() {
                            return EmeraldChip.CODEC;
                        }

                        @Override
                        public StreamCodec<? super RegistryFriendlyByteBuf,
                                EmeraldChip> streamCodec() {
                            return EmeraldChip.STREAM_CODEC;
                        }
                    });

    public static final RegistrySupplier<SimpleParticleType> HAMON_MOTE_FINE =
            PARTICLE_TYPES.register("hamon_mote_fine", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> VAMPIRE_RING =
            PARTICLE_TYPES.register("vampire_ring", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> RIPPER_RING =
            PARTICLE_TYPES.register("ripper_ring", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> RIPPER_MIST =
            PARTICLE_TYPES.register("ripper_mist", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> HAMON_AURA =
            PARTICLE_TYPES.register("hamon_aura", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> HAMON_RING =
            PARTICLE_TYPES.register("hamon_ring", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> HAMON_RIPPLE_RAINBOW =
            PARTICLE_TYPES.register("hamon_ripple_rainbow", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> HAMON_SPARK_RAINBOW =
            PARTICLE_TYPES.register("hamon_spark_rainbow", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> HAMON_MOTE_RAINBOW =
            PARTICLE_TYPES.register("hamon_mote_rainbow", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> HAMON_SPARK_FINE_RAINBOW =
            PARTICLE_TYPES.register("hamon_spark_fine_rainbow", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> HAMON_MOTE_FINE_RAINBOW =
            PARTICLE_TYPES.register("hamon_mote_fine_rainbow", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> HAMON_AURA_RAINBOW =
            PARTICLE_TYPES.register("hamon_aura_rainbow", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> HAMON_RING_RAINBOW =
            PARTICLE_TYPES.register("hamon_ring_rainbow", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> INHALE_WIND =
            PARTICLE_TYPES.register("inhale_wind", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> INHALE_SMOKE =
            PARTICLE_TYPES.register("inhale_smoke", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> GUARD_BREAK =
            PARTICLE_TYPES.register("guard_break", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> PILLAR_FLESH =
            PARTICLE_TYPES.register("pillar_flesh", () -> new SimpleParticleType(false) {
            });

    public static final RegistrySupplier<SimpleParticleType> PILLAR_BONE =
            PARTICLE_TYPES.register("pillar_bone", () -> new SimpleParticleType(false) {
            });

    public static final RegistrySupplier<SimpleParticleType> PILLAR_GUST =
            PARTICLE_TYPES.register("pillar_gust", () -> new SimpleParticleType(false) {
            });

    public static final RegistrySupplier<SimpleParticleType> PILLAR_SAND =
            PARTICLE_TYPES.register("pillar_sand", () -> new SimpleParticleType(false) {
            });

    public static final RegistrySupplier<SimpleParticleType> PILLAR_STEAM =
            PARTICLE_TYPES.register("pillar_steam", () -> new SimpleParticleType(false) {
            });

    public static final RegistrySupplier<SimpleParticleType> PILLAR_BUBBLE =
            PARTICLE_TYPES.register("pillar_bubble", () -> new SimpleParticleType(false) {
            });

    public static final RegistrySupplier<SimpleParticleType> PILLAR_GLINT =
            PARTICLE_TYPES.register("pillar_glint", () -> new SimpleParticleType(false) {
            });

    public static final RegistrySupplier<SimpleParticleType> PILLAR_STONE =
            PARTICLE_TYPES.register("pillar_stone", () -> new SimpleParticleType(false) {
            });

    public static final RegistrySupplier<SimpleParticleType> HEAT_SHIMMER =
            PARTICLE_TYPES.register("heat_shimmer", () -> new SimpleParticleType(false) {
            });

    public static final RegistrySupplier<SimpleParticleType> BLOOD_MOTE =
            PARTICLE_TYPES.register("blood_mote", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> STAND_AWAKEN_RED =
            PARTICLE_TYPES.register("stand_awaken_red", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> STAND_AWAKEN_BLUE =
            PARTICLE_TYPES.register("stand_awaken_blue", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> STAND_AWAKEN_PINK =
            PARTICLE_TYPES.register("stand_awaken_pink", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> STAND_AWAKEN =
            PARTICLE_TYPES.register("stand_awaken", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<CreativeModeTab> ITEMS_TAB =
            CREATIVE_TABS.register("items", () -> CreativeTabRegistry.create(builder -> {
                builder.title(Component.translatable("itemGroup.jojoha.items"));
                builder.icon(() -> new ItemStack(ModItems.STAND_ARROW.get()));
                builder.displayItems((params, output) -> {
                    for (ItemShelves shelf : ItemShelves.values()) {
                        shelf.stacks().forEach(output::accept);
                    }
                });
            }));

    public static final RegistrySupplier<CreativeModeTab> BLOCKS_TAB =
            CREATIVE_TABS.register("blocks", () -> CreativeTabRegistry.create(builder -> {
                builder.title(Component.translatable("itemGroup.jojoha.blocks"));
                builder.icon(() -> new ItemStack(ModBlocks.PHANTOM_GRASS_BLOCK.get()));
                builder.displayItems((params, output) -> {
                    for (var block : ModBlocks.natural()) {
                        output.accept(block.get());
                    }
                    for (var block : ModBlocks.building()) {
                        output.accept(block.get());
                    }
                });
            }));

    private ModRegistries() {
    }

    public static void init() {
        ModBlocks.bootstrap();
        ModComponents.bootstrap();
        ModItems.bootstrap();
        ModTraits.bootstrap();
        ModSounds.bootstrap();
        ModEffects.bootstrap();
        BLOCKS.register();
        BLOCK_ENTITY_TYPES.register();
        ARMOR_MATERIALS.register();
        RECIPE_SERIALIZERS.register();
        ModComponents.COMPONENTS.register();
        FEATURES.register();
        STRUCTURE_TYPES.register();
        STRUCTURE_PIECES.register();
        STRUCTURE_PROCESSORS.register();
        PLACEMENT_MODIFIERS.register();
        LOOT_FUNCTIONS.register();
        LOOT_CONDITIONS.register();
        ENTITY_TYPES.register();
        ITEMS.register();
        CREATIVE_TABS.register();
        PARTICLE_TYPES.register();
        SOUND_EVENTS.register();
        MOB_EFFECTS.register();
        CreativeTabRegistry.append(
                ResourceKey.create(
                        Registries.CREATIVE_MODE_TAB,
                        ResourceLocation.withDefaultNamespace("spawn_eggs")),
                ModItems.WILLOW_SPAWN_EGG,
                ModItems.RISEN_ZOMBIE_SPAWN_EGG,
                ModItems.AMALGAMATION_SPAWN_EGG,
                ModItems.VAMPIRE_SPAWN_EGG,
                ModItems.GHOST_SPAWN_EGG,
                ModItems.VAMPIRE_RAT_SPAWN_EGG,
                ModItems.HAMON_STUDENT_SPAWN_EGG,
                ModItems.STAND_USER_SPAWN_EGG,
                ModItems.JOTARO_SPAWN_EGG,
                ModItems.JOSEPH_SPAWN_EGG,
                ModItems.AVDOL_SPAWN_EGG,
                ModItems.KAKYOIN_SPAWN_EGG,
                ModItems.POLNAREFF_SPAWN_EGG);

        EntityAttributeRegistry.register(STAND, StandEntity::createAttributes);
        EntityAttributeRegistry.register(WILLOW, WillowEntity::createAttributes);
        EntityAttributeRegistry.register(RISEN_ZOMBIE,
                Zombie::createAttributes);
        EntityAttributeRegistry.register(AMALGAMATION, Amalgamation::createAttributes);
        EntityAttributeRegistry.register(VAMPIRE, VampireEntity::createAttributes);
        EntityAttributeRegistry.register(HAMON_STUDENT, HamonStudent::createAttributes);
        EntityAttributeRegistry.register(STAND_USER, StandUserNpc::createAttributes);
        EntityAttributeRegistry.register(JOTARO, CrusaderNpc::createAttributes);
        EntityAttributeRegistry.register(JOSEPH, CrusaderNpc::createAttributes);
        EntityAttributeRegistry.register(AVDOL, CrusaderNpc::createAttributes);
        EntityAttributeRegistry.register(KAKYOIN, CrusaderNpc::createAttributes);
        EntityAttributeRegistry.register(POLNAREFF, CrusaderNpc::createAttributes);
        EntityAttributeRegistry.register(GHOST, CatacombGhost::createAttributes);
        EntityAttributeRegistry.register(VAMPIRE_RAT, VampireRat::createAttributes);
    }
}
