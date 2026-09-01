package org.gumel.jojoha.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.gumel.jojoha.block.PhantomVineBlock;

import java.util.ArrayList;
import java.util.List;

import static org.gumel.jojoha.registry.ModRegistries.BLOCKS;
import static org.gumel.jojoha.registry.ModRegistries.ITEMS;

/**
 * The blocks the Phantom Highlands are built from.
 *
 * <h2>Properties borrowed from their vanilla counterparts</h2>
 *
 * <p>Each of these is copied from the block it stands in for - stone from stone, planks from planks -
 * rather than given numbers of its own. Hardness, tool, sound and push behaviour are things players
 * already know for these materials, and a stone that takes a different length of time to mine than
 * every other stone is a surprise with nothing to gain from it.
 */
public final class ModBlocks {
    /**
     * Which shelf a block belongs on.
     *
     * <p>Stated at the point of registration rather than worked out later, because any list kept
     * somewhere else is a list that falls behind the moment somebody adds a block and forgets it.
     * NATURAL is the default, so the worst a forgotten classification can do is put a block on the
     * wrong shelf - never make it unobtainable, which is the failure that actually matters.
     */
    public enum Tab {
        /** Terrain, ores and things that grow: what the biome generates. */
        NATURAL,
        /** Timber, stone and everything shaped from them: what a player builds with. */
        BUILDING,
    }

    /** Every block registered here, so the creative tabs do not need a second list to fall behind. */
    private static final List<RegistrySupplier<Block>> ALL = new ArrayList<>();
    private static final List<RegistrySupplier<Block>> NATURAL = new ArrayList<>();
    private static final List<RegistrySupplier<Block>> BUILDING = new ArrayList<>();

    public static final RegistrySupplier<Block> PHANTOM_STONE = stone("phantom_stone");
    public static final RegistrySupplier<Block> PHANTOM_ANDESITE = stone("phantom_andesite");

    public static final RegistrySupplier<Block> PHANTOM_LOG = log("phantom_log");
    public static final RegistrySupplier<Block> STRIPPED_PHANTOM_LOG = log("stripped_phantom_log");

    /**
     * Bark on all six faces, and the four shapes cut from it.
     *
     * <p>The plank family and this one are the same five shapes twice over, which is the point: they
     * are two finishes of one wood, and a build wants both. Everything here reads exactly like its
     * plank equivalent above and differs only in the sheet it wears - so the numbers are copied
     * rather than chosen, because a bark staircase that mines at a different speed to a plank one is
     * a difference nobody asked for.
     *
     * <p>The block itself is a pillar like the log, not a plain cube. Vanilla's wood blocks rotate,
     * and a wall of these wants to be able to run either way.
     */
    public static final RegistrySupplier<Block> PHANTOM_WOOD = log("phantom_wood");

    public static final RegistrySupplier<Block> PHANTOM_PLANKS = register("phantom_planks", Tab.BUILDING,
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    /**
     * Leaves, and deliberately not tinted.
     *
     * <p>The sheet is already coloured - measured at 0.58 saturation, a clear teal - so it is
     * painted art rather than the greyscale vanilla leaves are, which exist to be multiplied by a
     * biome's foliage colour. Registering a tint here would multiply teal by the biome's violet and
     * produce mud. See the client registration, which pointedly does not add a colour handler.
     */
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

    /**
     * The turf of the Highlands.
     *
     * <p>Grass on every face rather than a cap of grass over dirt sides, which is the whole point of
     * it. Vanilla's grass block shows dirt down the sides, and on a slope every step of the terrain
     * puts one of those sides in view - a hillside reads as a stack of dirt blocks with green lids.
     * Wrapping the turf around the sides is what makes a hill look like one surface.
     *
     * <p>Greyscale art, tinted at render time from the biome, exactly like the moss.
     */
    public static final RegistrySupplier<Block> PHANTOM_GRASS_BLOCK = register("phantom_grass_block",
            () -> new org.gumel.jojoha.block.PhantomGrassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(0.6F)
                    .sound(SoundType.GRASS)
                    .randomTicks()));

    /**
     * The mossy ground cover of the Highlands, tinted rather than painted.
     *
     * <p>The one sheet in this set that is greyscale - measured at 0.000 saturation, a flat #666666 -
     * so unlike the leaves and the logs it is not finished art, it is a value map waiting for a
     * colour. That is what the biome's own grass colour is for, and it is applied at render time
     * rather than baked in, so retuning the biome's violet moves the moss with it.
     */
    /**
     * Fallen leaves, in the ones and twos that gather under a canopy.
     *
     * <p>Soil only, the same ground any bush asks for. Litter that clung to a cliff face would read
     * as a mistake rather than as weather.
     */
    /**
     * The soil under the turf.
     *
     * <p>Vanilla dirt showing through a cut bank was the one place the biome's own palette stopped,
     * so the surface rule lays this instead. It is in #minecraft:dirt like everything else that
     * counts as soil, which is what lets plants stand on it and trees take root in it.
     */
    /**
     * The biome's ores.
     *
     * <p>The stone column here is phantom stone all the way down, so vanilla's ores would have looked
     * like grey stone erupting through it. These carry the same drops, the same tool requirements and
     * the same experience as their vanilla counterparts - the loot tables are vanilla's own, with
     * only the silk-touch drop repointed - so nothing about mining them is a surprise.
     *
     * <p>Experience follows vanilla exactly: none from the three that drop raw metal, since the
     * experience for those comes out of the furnace instead.
     */
    public static final RegistrySupplier<Block> PHANTOM_COAL_ORE = ore("phantom_coal_ore", UniformInt.of(0, 2));
    public static final RegistrySupplier<Block> PHANTOM_IRON_ORE = ore("phantom_iron_ore", ConstantInt.of(0));
    public static final RegistrySupplier<Block> PHANTOM_COPPER_ORE = ore("phantom_copper_ore", ConstantInt.of(0));
    public static final RegistrySupplier<Block> PHANTOM_GOLD_ORE = ore("phantom_gold_ore", ConstantInt.of(0));
    public static final RegistrySupplier<Block> PHANTOM_DIAMOND_ORE = ore("phantom_diamond_ore", UniformInt.of(3, 7));
    public static final RegistrySupplier<Block> PHANTOM_EMERALD_ORE = ore("phantom_emerald_ore", UniformInt.of(3, 7));
    public static final RegistrySupplier<Block> PHANTOM_LAPIS_ORE = ore("phantom_lapis_ore", UniformInt.of(2, 5));

    /**
     * What phantom stone breaks into.
     *
     * <p>Softer than the stone it comes from and with no tool requirement of its own beyond a
     * pickaxe, which is cobblestone's arrangement everywhere else.
     */
    /**
     * The meteorite itself, and the two ways of stacking what comes out of it.
     *
     * <p>The rock is as tough as ancient debris and, like it, resistant enough to survive a blast -
     * a meteorite that a creeper could scatter would be a poor thing. Both storage blocks follow
     * vanilla's convention for raw and refined metal.
     */
    public static final RegistrySupplier<Block> METEORITE = register("meteorite",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(30.0F, 1200.0F)
                    .sound(SoundType.ANCIENT_DEBRIS)));

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

    /**
     * Short grass, and the tall two-block form of it.
     *
     * <p>Both sheets are greyscale - measured at 0.017 saturation against 0.55 for the grass block's
     * own art - so unlike the block these are value maps and take the biome's grass colour at render
     * time. That is the same arrangement the moss has, and it means the plants follow the biome if
     * its colour is ever retuned.
     */
    public static final RegistrySupplier<Block> PHANTOM_SHORT_GRASS = register("phantom_short_grass",
            () -> new org.gumel.jojoha.block.PhantomShortGrassBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .replaceable()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<Block> PHANTOM_TALL_GRASS = register("phantom_tall_grass",
            () -> new net.minecraft.world.level.block.DoublePlantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .replaceable()
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    /**
     * Three flowers, all coloured art rather than value maps, so none of them is tinted.
     *
     * <p>The stew effect is the one piece of behaviour a flower carries, and each is picked to match
     * what the thing is called rather than left at a default.
     */
    public static final RegistrySupplier<Block> DUSKWEED =
            flower("duskweed", net.minecraft.world.effect.MobEffects.BLINDNESS, 8.0F);
    public static final RegistrySupplier<Block> BLOODVINE =
            flower("bloodvine", net.minecraft.world.effect.MobEffects.REGENERATION, 6.0F);
    public static final RegistrySupplier<Block> SUNLEAF =
            flower("sunleaf", net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 4.0F);

    public static final RegistrySupplier<Block> PHANTOM_LEAF_PILE = register("phantom_leaf_pile",
            () -> new org.gumel.jojoha.block.PhantomPileBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .noCollission()
                    .instabreak()
                    .sound(SoundType.PINK_PETALS)
                    .pushReaction(PushReaction.DESTROY)
                    .ignitedByLava(),
                    state -> false));

    /**
     * Loose stone, scattered wherever the ground will hold it.
     *
     * <p>Unlike the litter this also settles on the biome's own stone, because the hillsides are
     * bare rock and stones that refuse to lie on rock would be a peculiar thing to have built.
     */
    public static final RegistrySupplier<Block> PHANTOM_ROCK_PILE = register("phantom_rock_pile",
            () -> new org.gumel.jojoha.block.PhantomPileBlock(BlockBehaviour.Properties.of()
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

    /** Hanging strands. Climbable through the tag - see PhantomVineBlock. */
    /**
     * The building set: everything you can make out of the wood and the stone.
     *
     * <p>Every one of these is a vanilla block class with vanilla numbers, and that is deliberate -
     * a staircase that mines at a different speed to every other wooden staircase is a surprise with
     * nothing to gain from it. Five of the shapes have protected constructors and are reached
     * through the thin subclasses in {@code PhantomBuilding}.
     */
    public static final RegistrySupplier<Block> PHANTOM_STAIRS = register("phantom_stairs", Tab.BUILDING,
            () -> new org.gumel.jojoha.block.PhantomBuilding.Stairs(PHANTOM_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_SLAB = register("phantom_slab", Tab.BUILDING,
            () -> new net.minecraft.world.level.block.SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_FENCE = register("phantom_fence", Tab.BUILDING,
            () -> new net.minecraft.world.level.block.FenceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_WOOD_STAIRS = register("phantom_wood_stairs", Tab.BUILDING,
            () -> new org.gumel.jojoha.block.PhantomBuilding.Stairs(PHANTOM_WOOD.get().defaultBlockState(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_WOOD_SLAB = register("phantom_wood_slab", Tab.BUILDING,
            () -> new net.minecraft.world.level.block.SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_WOOD_FENCE = register("phantom_wood_fence", Tab.BUILDING,
            () -> new net.minecraft.world.level.block.FenceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_WOOD_TRAPDOOR = register("phantom_wood_trapdoor", Tab.BUILDING,
            () -> new org.gumel.jojoha.block.PhantomBuilding.Trapdoor(net.minecraft.world.level.block.state.properties.BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(3.0F)
                    .noOcclusion()
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_FENCE_GATE = register("phantom_fence_gate", Tab.BUILDING,
            () -> new net.minecraft.world.level.block.FenceGateBlock(net.minecraft.world.level.block.state.properties.WoodType.OAK, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_DOOR = register("phantom_door", Tab.BUILDING,
            () -> new org.gumel.jojoha.block.PhantomBuilding.Door(net.minecraft.world.level.block.state.properties.BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(3.0F)
                    .noOcclusion()
                    .sound(SoundType.WOOD)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<Block> PHANTOM_TRAPDOOR = register("phantom_trapdoor", Tab.BUILDING,
            () -> new org.gumel.jojoha.block.PhantomBuilding.Trapdoor(net.minecraft.world.level.block.state.properties.BlockSetType.OAK, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(3.0F)
                    .noOcclusion()
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final RegistrySupplier<Block> PHANTOM_BUTTON = register("phantom_button", Tab.BUILDING,
            () -> new org.gumel.jojoha.block.PhantomBuilding.Button(net.minecraft.world.level.block.state.properties.BlockSetType.OAK, 30, BlockBehaviour.Properties.of()
                    .noCollission()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<Block> PHANTOM_PRESSURE_PLATE =
            register("phantom_pressure_plate", Tab.BUILDING, () -> new org.gumel.jojoha.block.PhantomBuilding.Plate(net.minecraft.world.level.block.state.properties.BlockSetType.OAK,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .noCollission()
                            .strength(0.5F)
                            .sound(SoundType.WOOD)
                            .ignitedByLava()
                            .pushReaction(PushReaction.DESTROY)));

    public static final RegistrySupplier<Block> PHANTOM_STONE_STAIRS = register("phantom_stone_stairs", Tab.BUILDING,
            () -> new org.gumel.jojoha.block.PhantomBuilding.Stairs(PHANTOM_STONE.get().defaultBlockState(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> PHANTOM_STONE_SLAB = register("phantom_stone_slab", Tab.BUILDING,
            () -> new net.minecraft.world.level.block.SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> PHANTOM_STONE_WALL = register("phantom_stone_wall", Tab.BUILDING,
            () -> new net.minecraft.world.level.block.WallBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> PHANTOM_ANDESITE_STAIRS =
            register("phantom_andesite_stairs", Tab.BUILDING,
                    () -> new org.gumel.jojoha.block.PhantomBuilding.Stairs(PHANTOM_ANDESITE.get().defaultBlockState(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> PHANTOM_ANDESITE_SLAB =
            register("phantom_andesite_slab", Tab.BUILDING,
                    () -> new net.minecraft.world.level.block.SlabBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F)));

    public static final RegistrySupplier<Block> PHANTOM_ANDESITE_WALL =
            register("phantom_andesite_wall", Tab.BUILDING,
                    () -> new net.minecraft.world.level.block.WallBlock(BlockBehaviour.Properties.of()
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

    private ModBlocks() {
    }

    /** Every block of the set. */
    public static List<RegistrySupplier<Block>> all() {
        return ALL;
    }

    /** Terrain, ores and growing things. */
    public static List<RegistrySupplier<Block>> natural() {
        return NATURAL;
    }

    /** Timber, stone and the shapes cut from them. */
    public static List<RegistrySupplier<Block>> building() {
        return BUILDING;
    }

    /** No-op call site so this class's registrations happen before BLOCKS is frozen. */
    public static void bootstrap() {
    }

    private static RegistrySupplier<Block> stone(String name) {
        return register(name, () -> new Block(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .instrument(NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(1.5F, 6.0F)));
    }

    private static RegistrySupplier<Block> ore(
            String name, net.minecraft.util.valueproviders.IntProvider experience) {
        return register(name, () -> new net.minecraft.world.level.block.DropExperienceBlock(
                experience, BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_PURPLE)
                        .instrument(NoteBlockInstrument.BASEDRUM)
                        .requiresCorrectToolForDrops()
                        .strength(3.0F, 3.0F)));
    }

    private static RegistrySupplier<Block> flower(
            String name,
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> stewEffect,
            float stewSeconds) {
        return register(name, () -> new net.minecraft.world.level.block.FlowerBlock(
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

    /**
     * One block and the item that places it, kept together.
     *
     * <p>Registering the pair in one call is what stops a block existing with no way to hold it,
     * which is the sort of omission nothing complains about until somebody opens the creative tab.
     */
    private static RegistrySupplier<Block> register(String name,
                                                    java.util.function.Supplier<Block> block) {
        return register(name, Tab.NATURAL, block);
    }

    private static RegistrySupplier<Block> register(String name, Tab tab,
                                                    java.util.function.Supplier<Block> block) {
        RegistrySupplier<Block> registered = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        ALL.add(registered);
        (tab == Tab.BUILDING ? BUILDING : NATURAL).add(registered);
        return registered;
    }
}
