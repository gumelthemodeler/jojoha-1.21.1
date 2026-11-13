package org.gumel.jojoha.level.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.gumel.jojoha.mixin.StructureTemplateAccessor;
import org.gumel.jojoha.registry.ModBlocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.WeakHashMap;

public final class GroundFit {
    public static final int GRID = 5;
    public static final int SPREAD_TRIM = 2;
    public static final int MARGIN = 4;
    public static final int FOUNDATION_DEPTH = 16;
    public static final int SCAN_ABOVE = 10;
    public static final int SCAN_BELOW = 12;

    public record Footprint(Set<Long> columns, Map<Long, Integer> apron) {
    }

    private static final Map<StructureTemplate, Map<Integer, Footprint>> CACHE = new WeakHashMap<>();

    private GroundFit() {
    }

    public static OptionalInt site(Structure.GenerationContext context, int x, int z, Vec3i turned, int flatness) {
        int halfX = turned.getX() / 2;
        int halfZ = turned.getZ() / 2;
        int[] heights = new int[GRID * GRID];
        int n = 0;
        for (int i = 0; i < GRID; i++) {
            for (int j = 0; j < GRID; j++) {
                int at = x - halfX + (2 * halfX) * i / (GRID - 1);
                int bt = z - halfZ + (2 * halfZ) * j / (GRID - 1);
                int surface = height(context, at, bt, Heightmap.Types.WORLD_SURFACE_WG);
                int floor = height(context, at, bt, Heightmap.Types.OCEAN_FLOOR_WG);
                if (surface != floor) {
                    return OptionalInt.empty();
                }
                heights[n++] = floor;
            }
        }
        Arrays.sort(heights);
        if (heights[n - 1 - SPREAD_TRIM] - heights[SPREAD_TRIM] > flatness) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(heights[n / 2]);
    }

    private static int height(Structure.GenerationContext context, int x, int z, Heightmap.Types type) {
        return context.chunkGenerator().getBaseHeight(x, z, type, context.heightAccessor(), context.randomState());
    }

    public static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    public static boolean natural(BlockState state) {
        return state.isAir() || state.is(BlockTags.DIRT) || state.is(ModBlocks.PHANTOM_GRASS_BLOCK.get())
                || state.is(ModBlocks.PHANTOM_DIRT.get());
    }

    public static Footprint footprint(StructureTemplate template, int sink) {
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(template, t -> new HashMap<>())
                    .computeIfAbsent(sink, s -> build(template, s));
        }
    }

    private static Footprint build(StructureTemplate template, int sink) {
        Set<Long> columns = new HashSet<>();
        List<StructureTemplate.Palette> palettes = ((StructureTemplateAccessor) template).jojoha$palettes();
        if (!palettes.isEmpty()) {
            for (StructureTemplate.StructureBlockInfo info : palettes.get(0).blocks()) {
                BlockPos at = info.pos();
                boolean standing = at.getY() >= sink && !info.state().isAir();
                boolean built = at.getY() < sink && !natural(info.state());
                if ((standing || built) && !info.state().is(Blocks.STRUCTURE_VOID)) {
                    columns.add(key(at.getX(), at.getZ()));
                }
            }
        }
        Map<Long, Integer> apron = new HashMap<>();
        List<Long> edge = new ArrayList<>(columns);
        for (int d = 1; d <= MARGIN && !edge.isEmpty(); d++) {
            List<Long> next = new ArrayList<>();
            for (long column : edge) {
                int cx = (int) (column >> 32);
                int cz = (int) column;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        long near = key(cx + dx, cz + dz);
                        if (!columns.contains(near) && !apron.containsKey(near)) {
                            apron.put(near, d);
                            next.add(near);
                        }
                    }
                }
            }
            edge = next;
        }
        return new Footprint(Set.copyOf(columns), Map.copyOf(apron));
    }

    public static void settle(WorldGenLevel level, BoundingBox chunk, StructureTemplate template,
                              StructurePlaceSettings settings, BlockPos origin, int sink, boolean apron) {
        Footprint footprint = footprint(template, sink);
        int surface = origin.getY() + sink;
        for (long column : footprint.columns()) {
            BlockPos at = origin.offset(StructureTemplate.calculateRelativePosition(settings,
                    new BlockPos((int) (column >> 32), 0, (int) column)));
            if (chunk.isInside(at.getX(), chunk.minY(), at.getZ())) {
                foundation(level, at.below());
            }
        }
        if (!apron) {
            return;
        }
        for (Map.Entry<Long, Integer> entry : footprint.apron().entrySet()) {
            long column = entry.getKey();
            BlockPos at = origin.offset(StructureTemplate.calculateRelativePosition(settings,
                    new BlockPos((int) (column >> 32), 0, (int) column)));
            if (chunk.isInside(at.getX(), chunk.minY(), at.getZ())) {
                slope(level, at.getX(), at.getZ(), surface, entry.getValue());
            }
        }
    }

    private static boolean open(BlockState state) {
        return state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty()
                || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS);
    }

    private static void foundation(WorldGenLevel level, BlockPos top) {
        List<BlockPos> gap = new ArrayList<>();
        BlockPos.MutableBlockPos at = top.mutable();
        for (int i = 0; i < FOUNDATION_DEPTH && at.getY() > level.getMinBuildHeight(); i++) {
            BlockState state = level.getBlockState(at);
            if (!open(state)) {
                BlockState fill = under(state);
                for (BlockPos pos : gap) {
                    level.setBlock(pos, fill, 2);
                }
                return;
            }
            gap.add(at.immutable());
            at.move(0, -1, 0);
        }
    }

    private static boolean terrain(BlockState state) {
        return natural(state) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.SAND)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.MOSS_BLOCK);
    }

    private static BlockState under(BlockState top) {
        if (top.is(ModBlocks.PHANTOM_GRASS_BLOCK.get())) {
            return ModBlocks.PHANTOM_DIRT.get().defaultBlockState();
        }
        if (top.is(Blocks.GRASS_BLOCK) || top.is(Blocks.PODZOL) || top.is(Blocks.MYCELIUM)) {
            return Blocks.DIRT.defaultBlockState();
        }
        return top;
    }

    private static void slope(WorldGenLevel level, int x, int z, int surface, int distance) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, surface + distance + SCAN_ABOVE, z);
        int bottom = surface - distance - SCAN_BELOW;
        while (at.getY() > bottom && open(level.getBlockState(at))) {
            if (!level.getBlockState(at).getFluidState().isEmpty()) {
                return;
            }
            at.move(0, -1, 0);
        }
        if (at.getY() <= bottom) {
            return;
        }
        int top = at.getY();
        BlockState skin = level.getBlockState(at);
        if (!terrain(skin)) {
            return;
        }
        BlockState body = under(skin);
        int high = surface + distance;
        int low = surface - distance;
        if (top > high) {
            for (int y = top; y > high; y--) {
                level.setBlock(at.setY(y), Blocks.AIR.defaultBlockState(), 2);
            }
            level.setBlock(at.setY(high), skin, 2);
        } else if (top < low) {
            level.setBlock(at.setY(top), body, 2);
            for (int y = top + 1; y < low; y++) {
                level.setBlock(at.setY(y), body, 2);
            }
            level.setBlock(at.setY(low), skin, 2);
        }
    }

    public static boolean trimmed(Footprint footprint, BlockPos local, BlockState state, int sink) {
        return local.getY() < sink && natural(state) && !footprint.columns().contains(key(local.getX(), local.getZ()));
    }
}
