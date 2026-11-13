package org.gumel.jojoha.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.block.BossSummoningTotemBlock;
import org.gumel.jojoha.block.PairedDoublePlantBlock;
import org.gumel.jojoha.block.PhantomBuilding;
import org.gumel.jojoha.block.PhantomGrassBlock;
import org.gumel.jojoha.block.PhantomPileBlock;
import org.gumel.jojoha.block.PhantomShortGrassBlock;
import org.gumel.jojoha.block.PhantomVineBlock;
import org.gumel.jojoha.block.RespawnTotemBlock;
import org.gumel.jojoha.labyrinth.DungeonSpawnerBlock;
import org.gumel.jojoha.labyrinth.LabyrinthDoorBlock;
import org.gumel.jojoha.labyrinth.LabyrinthKeySlotBlock;
import org.gumel.jojoha.labyrinth.TntBarrelBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.gumel.jojoha.registry.ModRegistries.BLOCKS;
import static org.gumel.jojoha.registry.ModRegistries.ITEMS;

public final class ModBlocks {
    public enum Tab {
        NATURAL,
        BUILDING,
    }

    public enum Part {
        ONE,
        TWO,
        THREE,
    }

    private static Part partFor(String name) {
        if (name.startsWith("skeleton_") || name.startsWith("ashy_rockland_")) {
            return Part.TWO;
        }
        if (name.contains("meteorite") || name.startsWith("tombstone_") || name.equals("dungeon_spawner")
                || name.equals("tnt_barrel") || name.equals("crusader_sand") || name.equals("flesh_block")
                || name.equals("boss_summoning_totem")) {
            return Part.THREE;
        }
        if (name.equals("labyrinth_door") || name.equals("labyrinth_door_key_slot")) {
            return Part.ONE;
        }
        if (name.startsWith("phantom_") || name.startsWith("stripped_phantom_")
                || name.equals("sunleaf") || name.equals("duskweed") || name.equals("bloodvine")) {
            return Part.ONE;
        }
        return null;
    }

    private static final Map<String, Part> PARTS = new HashMap<>();

    public static Part partOf(Block block) {
        ResourceLocation id =
                BuiltInRegistries.BLOCK.getKey(block);
        return id != null && id.getNamespace().equals(Jojoha.MOD_ID)
                ? PARTS.get(id.getPath())
                : null;
    }

    public static boolean hasBlocksIn(Part part) {
        return PARTS.containsValue(part);
    }

    private static final List<RegistrySupplier<Block>> ALL = new ArrayList<>();
    private static final List<RegistrySupplier<Block>> NATURAL = new ArrayList<>();
    private static final List<RegistrySupplier<Block>> BUILDING = new ArrayList<>();

    public static final RegistrySupplier<Block> PHANTOM_STONE = stone("phantom_stone");
    public static final RegistrySupplier<Block> PHANTOM_ANDESITE = stone("phantom_andesite");

    public static final RegistrySupplier<Block> PHANTOM_LOG = log("phantom_log");
    public static final RegistrySupplier<Block> STRIPPED_PHANTOM_LOG = log("stripped_phantom_log");

    public static final RegistrySupplier<Block> PHANTOM_WOOD = log("phantom_wood");

    public static final RegistrySupplier<Block> PHANTOM_PLANKS = register("phantom_planks", Tab.BUILDING,
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_PLANKS_DECORATIVE =
            register("phantom_planks_decorative", Tab.BUILDING,
                    () -> new Block(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .instrument(NoteBlockInstrument.BASS)
                            .strength(2.0F, 3.0F)
                            .sound(SoundType.WOOD)
                            .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_PLANKS_DECORATIVE_STAIRS =
            register("phantom_planks_decorative_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(PHANTOM_PLANKS_DECORATIVE.get().defaultBlockState(),
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .instrument(NoteBlockInstrument.BASS)
                            .strength(2.0F, 3.0F)
                            .sound(SoundType.WOOD)
                            .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_PLANKS_DECORATIVE_SLAB =
            register("phantom_planks_decorative_slab", Tab.BUILDING,
                    () -> new SlabBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .instrument(NoteBlockInstrument.BASS)
                            .strength(2.0F, 3.0F)
                            .sound(SoundType.WOOD)
                            .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_LEAVES = register("phantom_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .strength(0.2F)
                    .randomTicks()
                    .sound(SoundType.GRASS)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<Block> PHANTOM_GRASS_BLOCK = register("phantom_grass_block",
            () -> new PhantomGrassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.6F)
                    .sound(SoundType.GRASS)
                    .randomTicks()));

    public static final RegistrySupplier<Block> PHANTOM_COAL_ORE = ore("phantom_coal_ore", UniformInt.of(0, 2));
    public static final RegistrySupplier<Block> PHANTOM_IRON_ORE = ore("phantom_iron_ore", ConstantInt.of(0));
    public static final RegistrySupplier<Block> PHANTOM_COPPER_ORE = ore("phantom_copper_ore", ConstantInt.of(0));
    public static final RegistrySupplier<Block> PHANTOM_GOLD_ORE = ore("phantom_gold_ore", ConstantInt.of(0));
    public static final RegistrySupplier<Block> PHANTOM_DIAMOND_ORE = ore("phantom_diamond_ore", UniformInt.of(3, 7));
    public static final RegistrySupplier<Block> PHANTOM_EMERALD_ORE = ore("phantom_emerald_ore", UniformInt.of(3, 7));
    public static final RegistrySupplier<Block> PHANTOM_LAPIS_ORE = ore("phantom_lapis_ore", UniformInt.of(2, 5));

    public static final RegistrySupplier<Block> METEORITE = register("meteorite",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(30.0F, 1200.0F)
                    .sound(SoundType.ANCIENT_DEBRIS)));

    public static final RegistrySupplier<Block> METEORITE_ROCK = meteorRock("meteorite_rock");
    public static final RegistrySupplier<Block> PALE_METEORITE_ROCK = meteorRock("pale_meteorite_rock");

    private static final int SUNSCORCHED_DUST = 0xC7B48B;

    public static final RegistrySupplier<Block> SUNSCORCHED_SAND = register("sunscorched_sand", Tab.BUILDING,
            () -> new ColoredFallingBlock(
                    new ColorRGBA(SUNSCORCHED_DUST),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.SAND)
                            .strength(0.6F)
                            .sound(SoundType.SAND)));

    public static final RegistrySupplier<Block> RAW_METEORITE_BLOCK = register("raw_meteorite_block", Tab.BUILDING,
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 6.0F)));

    public static final RegistrySupplier<Block> METEORITE_BLOCK = register("meteorite_block", Tab.BUILDING,
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(50.0F, 1200.0F)
                    .sound(SoundType.NETHERITE_BLOCK)));

    public static final RegistrySupplier<Block> PHANTOM_COBBLESTONE = register("phantom_cobblestone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(2.0F, 6.0F)));

    public static final RegistrySupplier<Block> PHANTOM_DIRT = register("phantom_dirt",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.5F)
                    .sound(SoundType.GRAVEL)));

    public static final RegistrySupplier<Block> PHANTOM_COARSE_DIRT = register("phantom_coarse_dirt",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.5F)
                    .sound(SoundType.GRAVEL)));

    public static final RegistrySupplier<Block> PHANTOM_SHORT_GRASS = register("phantom_short_grass",
            () -> new PhantomShortGrassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .replaceable()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<Block> ASHY_ROCKLAND_STONE = register("ashy_rockland_stone",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> PHANTOM_STONE_BRICKS =
            worked("phantom_stone_bricks", MapColor.COLOR_PURPLE, SoundType.STONE, 1.5F, 6.0F);

    public static final RegistrySupplier<Block> PHANTOM_STONE_BRICKS_STAIRS =
            register("phantom_stone_bricks_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(PHANTOM_STONE_BRICKS.get().defaultBlockState(),
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> PHANTOM_STONE_BRICKS_SLAB =
            register("phantom_stone_bricks_slab", Tab.BUILDING,
                    () -> new SlabBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> PHANTOM_STONE_BRICKS_WALL =
            register("phantom_stone_bricks_wall", Tab.BUILDING,
                    () -> new WallBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> PHANTOM_STONE_CHISELED =
            worked("phantom_stone_chiseled", MapColor.COLOR_PURPLE, SoundType.STONE, 1.5F, 6.0F);
    public static final RegistrySupplier<Block> PHANTOM_STONE_SMOOTH =
            worked("phantom_stone_smooth", MapColor.COLOR_PURPLE, SoundType.STONE, 1.5F, 6.0F);

    public static final RegistrySupplier<Block> PHANTOM_STONE_SMOOTH_STAIRS =
            register("phantom_stone_smooth_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(PHANTOM_STONE_SMOOTH.get().defaultBlockState(),
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> PHANTOM_STONE_SMOOTH_SLAB =
            register("phantom_stone_smooth_slab", Tab.BUILDING,
                    () -> new SlabBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> PHANTOM_STONE_BRICKS_CRACKED =
            worked("phantom_stone_bricks_cracked", MapColor.COLOR_PURPLE, SoundType.STONE, 1.5F, 6.0F);

    public static final RegistrySupplier<Block> PHANTOM_STONE_BRICKS_CRACKED_STAIRS =
            register("phantom_stone_bricks_cracked_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(PHANTOM_STONE_BRICKS_CRACKED.get().defaultBlockState(),
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> PHANTOM_STONE_BRICKS_CRACKED_SLAB =
            register("phantom_stone_bricks_cracked_slab", Tab.BUILDING,
                    () -> new SlabBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> PHANTOM_STONE_BRICKS_CRACKED_WALL =
            register("phantom_stone_bricks_cracked_wall", Tab.BUILDING,
                    () -> new WallBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> PHANTOM_STONE_BRICKS_MOSSY =
            worked("phantom_stone_bricks_mossy", MapColor.COLOR_PURPLE, SoundType.STONE, 1.5F, 6.0F);

    public static final RegistrySupplier<Block> PHANTOM_STONE_BRICKS_MOSSY_STAIRS =
            register("phantom_stone_bricks_mossy_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(PHANTOM_STONE_BRICKS_MOSSY.get().defaultBlockState(),
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> PHANTOM_STONE_BRICKS_MOSSY_SLAB =
            register("phantom_stone_bricks_mossy_slab", Tab.BUILDING,
                    () -> new SlabBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> PHANTOM_STONE_BRICKS_MOSSY_WALL =
            register("phantom_stone_bricks_mossy_wall", Tab.BUILDING,
                    () -> new WallBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> ASHY_ROCKLAND_STONE_BRICKS =
            worked("ashy_rockland_stone_bricks", MapColor.STONE, SoundType.STONE, 1.5F, 6.0F);

    public static final RegistrySupplier<Block> ASHY_ROCKLAND_STONE_BRICKS_STAIRS =
            register("ashy_rockland_stone_bricks_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(ASHY_ROCKLAND_STONE_BRICKS.get().defaultBlockState(),
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> ASHY_ROCKLAND_STONE_BRICKS_SLAB =
            register("ashy_rockland_stone_bricks_slab", Tab.BUILDING,
                    () -> new SlabBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> ASHY_ROCKLAND_STONE_BRICKS_WALL =
            register("ashy_rockland_stone_bricks_wall", Tab.BUILDING,
                    () -> new WallBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> ASHY_ROCKLAND_STONE_CHISELED =
            worked("ashy_rockland_stone_chiseled", MapColor.STONE, SoundType.STONE, 1.5F, 6.0F);
    public static final RegistrySupplier<Block> ASHY_ROCKLAND_STONE_BRICKS_CRACKED =
            worked("ashy_rockland_stone_bricks_cracked", MapColor.STONE, SoundType.STONE, 1.5F, 6.0F);

    public static final RegistrySupplier<Block> ASHY_ROCKLAND_STONE_BRICKS_CRACKED_STAIRS =
            register("ashy_rockland_stone_bricks_cracked_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(ASHY_ROCKLAND_STONE_BRICKS_CRACKED.get().defaultBlockState(),
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> ASHY_ROCKLAND_STONE_BRICKS_CRACKED_SLAB =
            register("ashy_rockland_stone_bricks_cracked_slab", Tab.BUILDING,
                    () -> new SlabBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> ASHY_ROCKLAND_STONE_BRICKS_CRACKED_WALL =
            register("ashy_rockland_stone_bricks_cracked_wall", Tab.BUILDING,
                    () -> new WallBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));
    public static final RegistrySupplier<Block> ASHY_ROCKLAND_STONE_BRICKS_MOSSY =
            worked("ashy_rockland_stone_bricks_mossy", MapColor.STONE, SoundType.STONE, 1.5F, 6.0F);

    public static final RegistrySupplier<Block> ASHY_ROCKLAND_STONE_BRICKS_MOSSY_STAIRS =
            register("ashy_rockland_stone_bricks_mossy_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(ASHY_ROCKLAND_STONE_BRICKS_MOSSY.get().defaultBlockState(),
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> ASHY_ROCKLAND_STONE_BRICKS_MOSSY_SLAB =
            register("ashy_rockland_stone_bricks_mossy_slab", Tab.BUILDING,
                    () -> new SlabBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> ASHY_ROCKLAND_STONE_BRICKS_MOSSY_WALL =
            register("ashy_rockland_stone_bricks_mossy_wall", Tab.BUILDING,
                    () -> new WallBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> ASHY_ROCKLAND_STONE_PILLARS =
            register("ashy_rockland_stone_pillars", Tab.BUILDING,
                    () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> BONE_BUNDLE = register("bone_bundle", Tab.BUILDING,
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .instrument(NoteBlockInstrument.XYLOPHONE)
                    .requiresCorrectToolForDrops()
                    .strength(2.0F)
                    .sound(SoundType.BONE_BLOCK)));

    public static final RegistrySupplier<Block> SKELETON_ROCK = register("skeleton_rock",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(2.0F, 8.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE)));

    public static final RegistrySupplier<Block> SKELETON_ROCK_INVINCIBLE = register("skeleton_rock_invincible",
            Tab.BUILDING, () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .strength(-1.0F, 3600000.0F)
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.DEEPSLATE)));

    public static final RegistrySupplier<Block> SKELETON_ROCK_BRICKS =
            worked("skeleton_rock_bricks", MapColor.TERRACOTTA_WHITE, SoundType.DEEPSLATE, 2.0F, 8.0F);

    public static final RegistrySupplier<Block> SKELETON_ROCK_BRICKS_STAIRS =
            register("skeleton_rock_bricks_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(SKELETON_ROCK_BRICKS.get().defaultBlockState(),
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(2.0F, 8.0F)
                            .sound(SoundType.DEEPSLATE)));

    public static final RegistrySupplier<Block> SKELETON_ROCK_BRICKS_SLAB =
            register("skeleton_rock_bricks_slab", Tab.BUILDING,
                    () -> new SlabBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(2.0F, 8.0F)
                            .sound(SoundType.DEEPSLATE)));

    public static final RegistrySupplier<Block> SKELETON_ROCK_BRICKS_WALL =
            register("skeleton_rock_bricks_wall", Tab.BUILDING,
                    () -> new WallBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(2.0F, 8.0F)
                            .sound(SoundType.DEEPSLATE)));
    public static final RegistrySupplier<Block> SKELETON_ROCK_BRICKS_CRACKED =
            worked("skeleton_rock_bricks_cracked", MapColor.TERRACOTTA_WHITE, SoundType.DEEPSLATE, 2.0F, 8.0F);

    public static final RegistrySupplier<Block> SKELETON_ROCK_BRICKS_CRACKED_STAIRS =
            register("skeleton_rock_bricks_cracked_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(SKELETON_ROCK_BRICKS_CRACKED.get().defaultBlockState(),
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(2.0F, 8.0F)
                            .sound(SoundType.DEEPSLATE)));

    public static final RegistrySupplier<Block> SKELETON_ROCK_BRICKS_CRACKED_SLAB =
            register("skeleton_rock_bricks_cracked_slab", Tab.BUILDING,
                    () -> new SlabBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(2.0F, 8.0F)
                            .sound(SoundType.DEEPSLATE)));

    public static final RegistrySupplier<Block> SKELETON_ROCK_BRICKS_CRACKED_WALL =
            register("skeleton_rock_bricks_cracked_wall", Tab.BUILDING,
                    () -> new WallBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(2.0F, 8.0F)
                            .sound(SoundType.DEEPSLATE)));
    public static final RegistrySupplier<Block> SKELETON_ROCK_BRICKS_MOSSY =
            worked("skeleton_rock_bricks_mossy", MapColor.TERRACOTTA_WHITE, SoundType.DEEPSLATE, 2.0F, 8.0F);

    public static final RegistrySupplier<Block> SKELETON_ROCK_BRICKS_MOSSY_STAIRS =
            register("skeleton_rock_bricks_mossy_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(SKELETON_ROCK_BRICKS_MOSSY.get().defaultBlockState(),
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(2.0F, 8.0F)
                            .sound(SoundType.DEEPSLATE)));

    public static final RegistrySupplier<Block> SKELETON_ROCK_BRICKS_MOSSY_SLAB =
            register("skeleton_rock_bricks_mossy_slab", Tab.BUILDING,
                    () -> new SlabBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(2.0F, 8.0F)
                            .sound(SoundType.DEEPSLATE)));

    public static final RegistrySupplier<Block> SKELETON_ROCK_BRICKS_MOSSY_WALL =
            register("skeleton_rock_bricks_mossy_wall", Tab.BUILDING,
                    () -> new WallBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(2.0F, 8.0F)
                            .sound(SoundType.DEEPSLATE)));

    public static final RegistrySupplier<Block> SKELETON_ROCK_ORNATE_BRICKS =
            worked("skeleton_rock_ornate_bricks", MapColor.TERRACOTTA_WHITE, SoundType.DEEPSLATE, 2.0F, 8.0F);

    public static final RegistrySupplier<Block> SKELETON_ROCK_ORNATE_BRICKS_STAIRS =
            register("skeleton_rock_ornate_bricks_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(SKELETON_ROCK_ORNATE_BRICKS.get().defaultBlockState(),
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(2.0F, 8.0F)
                            .sound(SoundType.DEEPSLATE)));

    public static final RegistrySupplier<Block> SKELETON_ROCK_ORNATE_BRICKS_SLAB =
            register("skeleton_rock_ornate_bricks_slab", Tab.BUILDING,
                    () -> new SlabBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(2.0F, 8.0F)
                            .sound(SoundType.DEEPSLATE)));

    public static final RegistrySupplier<Block> SKELETON_ROCK_ORNATE_BRICKS_CRACKED =
            worked("skeleton_rock_ornate_bricks_cracked", MapColor.TERRACOTTA_WHITE, SoundType.DEEPSLATE, 2.0F, 8.0F);

    public static final RegistrySupplier<Block> SKELETON_ROCK_ORNATE_BRICKS_CRACKED_STAIRS =
            register("skeleton_rock_ornate_bricks_cracked_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(SKELETON_ROCK_ORNATE_BRICKS_CRACKED.get().defaultBlockState(),
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(2.0F, 8.0F)
                            .sound(SoundType.DEEPSLATE)));

    public static final RegistrySupplier<Block> SKELETON_ROCK_ORNATE_BRICKS_CRACKED_SLAB =
            register("skeleton_rock_ornate_bricks_cracked_slab", Tab.BUILDING,
                    () -> new SlabBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(2.0F, 8.0F)
                            .sound(SoundType.DEEPSLATE)));

    public static final RegistrySupplier<Block> SKELETON_ROCK_ORNATE_PILLAR =
            register("skeleton_rock_ornate_pillar", Tab.BUILDING,
                    () -> new RotatedPillarBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.TERRACOTTA_WHITE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(2.0F, 8.0F)
                            .sound(SoundType.DEEPSLATE)));

    public static final RegistrySupplier<Block> SKELETON_SAND = register("skeleton_sand",
            () -> new ColoredFallingBlock(
                    new ColorRGBA(0xFFD9D2C0),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.SAND)
                            .strength(0.5F)
                            .sound(SoundType.SAND)));

    public static final RegistrySupplier<Block> CRUSADER_SAND = register("crusader_sand", Tab.BUILDING,
            () -> new ColoredFallingBlock(
                    new ColorRGBA(0xFFE0BA8E),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.SAND)
                            .instrument(NoteBlockInstrument.SNARE)
                            .strength(0.5F)
                            .sound(SoundType.SAND)));

    public static final RegistrySupplier<Block> PHANTOM_TALL_GRASS = register("phantom_tall_grass",
            () -> new PairedDoublePlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .replaceable()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<Block> DUSKWEED =
            flower("duskweed", MobEffects.BLINDNESS, 8.0F);
    public static final RegistrySupplier<Block> BLOODVINE =
            flower("bloodvine", MobEffects.REGENERATION, 6.0F);
    public static final RegistrySupplier<Block> SUNLEAF =
            flower("sunleaf", MobEffects.FIRE_RESISTANCE, 4.0F);

    public static final RegistrySupplier<Block> PHANTOM_LEAF_PILE = register("phantom_leaf_pile",
            () -> new PhantomPileBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.PINK_PETALS)
                    .pushReaction(PushReaction.DESTROY)
                    .ignitedByLava(),
                    state -> false));

    public static final RegistrySupplier<Block> PHANTOM_ROCK_PILE = register("phantom_rock_pile",
            () -> new PhantomPileBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.DESTROY),
                    state -> state.is(PHANTOM_STONE.get()) || state.is(PHANTOM_ANDESITE.get())));

    public static final RegistrySupplier<Block> PHANTOM_MOSS = register("phantom_moss",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.1F)
                    .sound(SoundType.MOSS)));

    public static final RegistrySupplier<Block> PHANTOM_STAIRS = register("phantom_stairs", Tab.BUILDING,
            () -> new PhantomBuilding.Stairs(PHANTOM_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_SLAB = register("phantom_slab", Tab.BUILDING,
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_FENCE = register("phantom_fence", Tab.BUILDING,
            () -> new FenceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_WOOD_STAIRS = register("phantom_wood_stairs", Tab.BUILDING,
            () -> new PhantomBuilding.Stairs(PHANTOM_WOOD.get().defaultBlockState(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_WOOD_SLAB = register("phantom_wood_slab", Tab.BUILDING,
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_WOOD_FENCE = register("phantom_wood_fence", Tab.BUILDING,
            () -> new FenceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_WOOD_TRAPDOOR = register("phantom_wood_trapdoor", Tab.BUILDING,
            () -> new PhantomBuilding.Trapdoor(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(3.0F)
                    .noOcclusion()
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_FENCE_GATE = register("phantom_fence_gate", Tab.BUILDING,
            () -> new FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_DOOR = register("phantom_door", Tab.BUILDING,
            () -> new PhantomBuilding.Door(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(3.0F)
                    .noOcclusion()
                    .sound(SoundType.WOOD)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<Block> PHANTOM_TRAPDOOR = register("phantom_trapdoor", Tab.BUILDING,
            () -> new PhantomBuilding.Trapdoor(BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(3.0F)
                    .noOcclusion()
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> DEEPSLATE_TRAPDOOR = register("deepslate_trapdoor", Tab.BUILDING,
            () -> new PhantomBuilding.Trapdoor(BlockSetType.STONE, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(3.5F, 6.0F)
                    .noOcclusion()
                    .sound(SoundType.DEEPSLATE_TILES)
                    .isValidSpawn((state, level, pos, type) -> false)));

    public static final RegistrySupplier<Block> PHANTOM_BUTTON = register("phantom_button", Tab.BUILDING,
            () -> new PhantomBuilding.Button(BlockSetType.OAK, 30, BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<Block> PHANTOM_PRESSURE_PLATE =
            register("phantom_pressure_plate", Tab.BUILDING, () -> new PhantomBuilding.Plate(BlockSetType.OAK,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .noCollission()
                            .strength(0.5F)
                            .sound(SoundType.WOOD)
                            .ignitedByLava()
                            .pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<Block> PHANTOM_STONE_STAIRS = register("phantom_stone_stairs", Tab.BUILDING,
            () -> new PhantomBuilding.Stairs(PHANTOM_STONE.get().defaultBlockState(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> PHANTOM_STONE_SLAB = register("phantom_stone_slab", Tab.BUILDING,
            () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> PHANTOM_STONE_WALL = register("phantom_stone_wall", Tab.BUILDING,
            () -> new WallBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> PHANTOM_ANDESITE_STAIRS =
            register("phantom_andesite_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(PHANTOM_ANDESITE.get().defaultBlockState(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> PHANTOM_ANDESITE_SLAB =
            register("phantom_andesite_slab", Tab.BUILDING,
                    () -> new SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> PHANTOM_ANDESITE_WALL =
            register("phantom_andesite_wall", Tab.BUILDING,
                    () -> new WallBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> PHANTOM_VINES = register("phantom_vines",
            () -> new PhantomVineBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_CYAN)
                    .replaceable()
                    .noCollission()
                    .randomTicks()
                    .instabreak()
                    .sound(SoundType.VINE)
                    .noOcclusion()
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<Block> PHANTOM_DUNGEON_BRICKS =
            worked("phantom_dungeon_bricks", MapColor.COLOR_GREEN, SoundType.STONE, 1.5F, 6.0F);

    public static final RegistrySupplier<Block> PHANTOM_VILLAGE_BRICKS =
            worked("phantom_village_bricks", MapColor.COLOR_GREEN, SoundType.STONE, 1.5F, 6.0F);

    public static final RegistrySupplier<Block> PHANTOM_VILLAGE_BRICKS_STAIRS =
            register("phantom_village_bricks_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(PHANTOM_VILLAGE_BRICKS.get().defaultBlockState(),
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GREEN)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> PHANTOM_VILLAGE_BRICKS_SLAB =
            register("phantom_village_bricks_slab", Tab.BUILDING,
                    () -> new SlabBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GREEN)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> PHANTOM_VILLAGE_BRICKS_WALL =
            register("phantom_village_bricks_wall", Tab.BUILDING,
                    () -> new WallBlock(
                            BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_GREEN)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.STONE)));

    public static final RegistrySupplier<Block> WAX_BLOCK = register("wax_block", Tab.BUILDING,
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(0.6F)
                    .sound(SoundType.CANDLE)));

    public static final RegistrySupplier<Block> FLESH_BLOCK = register("flesh_block", Tab.BUILDING,
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.8F)
                    .sound(SoundType.MUD)));

    public static final RegistrySupplier<Block> SACK = sack("sack");
    public static final RegistrySupplier<Block> SACK_OF_APPLES = sack("sack_of_apples");
    public static final RegistrySupplier<Block> SACK_OF_GOLDEN_APPLES = sack("sack_of_golden_apples");
    public static final RegistrySupplier<Block> SACK_OF_BONES = sack("sack_of_bones");
    public static final RegistrySupplier<Block> SACK_OF_ROTTEN_FLESH = sack("sack_of_rotten_flesh");

    public static final RegistrySupplier<Block> TOMBSTONE_BRICKS = register("tombstone_bricks", Tab.BUILDING,
            () -> new Block(sealed()));
    public static final RegistrySupplier<Block> TOMBSTONE_BRICKS_STAIRS = register("tombstone_bricks_stairs",
            Tab.BUILDING, () -> new PhantomBuilding.Stairs(
                    TOMBSTONE_BRICKS.get().defaultBlockState(), sealed()));
    public static final RegistrySupplier<Block> TOMBSTONE_BRICKS_SLAB = register("tombstone_bricks_slab",
            Tab.BUILDING, () -> new SlabBlock(sealed()));
    public static final RegistrySupplier<Block> TOMBSTONE_BRICKS_WALL = register("tombstone_bricks_wall",
            Tab.BUILDING, () -> new WallBlock(sealed()));
    public static final RegistrySupplier<Block> TOMBSTONE_BRICKS_CRACKED = register("tombstone_bricks_cracked",
            Tab.BUILDING, () -> new Block(sealed()));
    public static final RegistrySupplier<Block> TOMBSTONE_BRICKS_CRACKED_STAIRS =
            register("tombstone_bricks_cracked_stairs", Tab.BUILDING,
                    () -> new PhantomBuilding.Stairs(
                            TOMBSTONE_BRICKS_CRACKED.get().defaultBlockState(), sealed()));
    public static final RegistrySupplier<Block> TOMBSTONE_BRICKS_CRACKED_SLAB =
            register("tombstone_bricks_cracked_slab", Tab.BUILDING,
                    () -> new SlabBlock(sealed()));
    public static final RegistrySupplier<Block> TOMBSTONE_BRICKS_CRACKED_WALL =
            register("tombstone_bricks_cracked_wall", Tab.BUILDING,
                    () -> new WallBlock(sealed()));

    public static final RegistrySupplier<Block> LABYRINTH_DOOR = register("labyrinth_door", Tab.BUILDING,
            () -> new LabyrinthDoorBlock(sealed()
                    .mapColor(MapColor.COLOR_BLACK)
                    .sound(SoundType.WOOD)));

    public static final RegistrySupplier<Block> LABYRINTH_DOOR_KEY_SLOT = register("labyrinth_door_key_slot", Tab.BUILDING,
            () -> new LabyrinthKeySlotBlock(sealed()
                    .mapColor(MapColor.COLOR_BLACK)
                    .sound(SoundType.WOOD)));

    public static final RegistrySupplier<Block> DUNGEON_SPAWNER = register("dungeon_spawner", Tab.BUILDING,
            () -> new DungeonSpawnerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> state.getValue(DungeonSpawnerBlock.ACTIVE)
                            && !state.getValue(DungeonSpawnerBlock.SPENT) ? 8 : 0)
                    .pushReaction(PushReaction.BLOCK)));

    public static final RegistrySupplier<Block> TNT_BARREL = register("tnt_barrel", Tab.BUILDING,
            () -> new TntBarrelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(-1.0F, 0.0F)
                    .noLootTable()
                    .sound(SoundType.WOOD)
                    .pushReaction(PushReaction.BLOCK)));

    public static final RegistrySupplier<Block> RESPAWN_TOTEM = register("respawn_totem", Tab.BUILDING,
            () -> new RespawnTotemBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 1200.0F)
                    .sound(SoundType.DEEPSLATE_BRICKS)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    public static final RegistrySupplier<Block> BOSS_SUMMONING_TOTEM = register("boss_summoning_totem", Tab.BUILDING,
            () -> new BossSummoningTotemBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F, 1200.0F)
                    .sound(SoundType.DEEPSLATE_BRICKS)
                    .lightLevel(state -> state.getValue(BossSummoningTotemBlock.LIT) ? 7 : 0)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    private ModBlocks() {
    }

    public static List<RegistrySupplier<Block>> all() {
        return ALL;
    }

    public static List<RegistrySupplier<Block>> natural() {
        return NATURAL;
    }

    public static List<RegistrySupplier<Block>> building() {
        return BUILDING;
    }

    public static void bootstrap() {
    }

    private static BlockBehaviour.Properties sealed() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BROWN)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .strength(-1.0F, 3600000.0F)
                .noLootTable()
                .sound(SoundType.DEEPSLATE_BRICKS)
                .pushReaction(PushReaction.BLOCK);
    }

    private static RegistrySupplier<Block> sack(String name) {
        return register(name, Tab.BUILDING, () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .instrument(NoteBlockInstrument.GUITAR)
                .strength(0.8F)
                .sound(SoundType.WOOL)
                .ignitedByLava()));
    }

    private static RegistrySupplier<Block> meteorRock(String name) {
        return register(name, Tab.BUILDING, () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_GRAY)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(4.0F, 12.0F)
                .sound(SoundType.ANCIENT_DEBRIS)));
    }

    private static RegistrySupplier<Block> worked(String name, MapColor colour, SoundType sound,
                                                  float hardness, float resistance) {
        return register(name, Tab.BUILDING, () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(colour)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(hardness, resistance)
                .sound(sound)));
    }

    private static RegistrySupplier<Block> stone(String name) {
        return register(name, () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(1.5F, 6.0F)));
    }

    private static RegistrySupplier<Block> ore(
            String name, IntProvider experience) {
        return register(name, () -> new DropExperienceBlock(
                experience, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_PURPLE)
                        .instrument(NoteBlockInstrument.BASEDRUM)
                        .requiresCorrectToolForDrops()
                        .strength(3.0F, 3.0F)));
    }

    private static RegistrySupplier<Block> flower(
            String name,
            Holder<MobEffect> stewEffect,
            float stewSeconds) {
        return register(name, () -> new FlowerBlock(
                stewEffect, stewSeconds, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.PLANT)
                        .noCollission()
                        .instabreak()
                        .sound(SoundType.GRASS)
                        .offsetType(BlockBehaviour.OffsetType.XZ)
                        .ignitedByLava()
                        .pushReaction(PushReaction.DESTROY)));
    }

    private static RegistrySupplier<Block> log(String name) {
        return register(name, Tab.BUILDING, () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .instrument(NoteBlockInstrument.BASS)
                .strength(2.0F)
                .sound(SoundType.WOOD)
                .ignitedByLava()));
    }

    private static RegistrySupplier<Block> register(String name,
                                                    Supplier<Block> block) {
        return register(name, Tab.NATURAL, block);
    }

    private static RegistrySupplier<Block> register(String name, Tab tab,
                                                    Supplier<Block> block) {
        RegistrySupplier<Block> registered = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        ALL.add(registered);
        Part part = partFor(name);
        if (part != null) {
            PARTS.put(name, part);
        }
        (tab == Tab.BUILDING ? BUILDING : NATURAL).add(registered);
        return registered;
    }
}
