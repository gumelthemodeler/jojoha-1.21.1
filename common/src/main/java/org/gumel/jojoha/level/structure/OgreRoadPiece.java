package org.gumel.jojoha.level.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import org.gumel.jojoha.registry.ModBlocks;
import org.gumel.jojoha.registry.ModRegistries;

public class OgreRoadPiece extends StructurePiece {
    public static final int ROAD = 0;
    public static final int EDGE = 1;
    public static final int FRINGE = 2;
    public static final int CROP = 3;
    public static final int CROPS = 4;
    public static final int WATER = CROP + CROPS;
    private static final int CLEAR = 4;
    private static final int FILL = 8;
    private static final int EDGE_DIRT = 30;
    private static final int FRINGE_STONE = 25;
    private static final int[] PAVING_SHARE = {40, 20, 20, 20};
    private static final int FRINGE_DIRT = 55;
    private static final int FRINGE_REACH = 1;

    private final int[] xs;
    private final int[] ys;
    private final int[] zs;
    private final int[] kinds;

    public OgreRoadPiece(int[] xs, int[] ys, int[] zs, int[] kinds) {
        super(ModRegistries.OGRE_ROAD_PIECE.get(), 0, box(xs, ys, zs));
        this.xs = xs;
        this.ys = ys;
        this.zs = zs;
        this.kinds = kinds;
    }

    public OgreRoadPiece(CompoundTag tag) {
        super(ModRegistries.OGRE_ROAD_PIECE.get(), tag);
        this.xs = tag.getIntArray("X");
        this.ys = tag.getIntArray("Y");
        this.zs = tag.getIntArray("Z");
        int[] stored = tag.getIntArray("K");
        this.kinds = stored.length == xs.length ? stored : new int[xs.length];
    }

    private static BoundingBox box(int[] xs, int[] ys, int[] zs) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int i = 0; i < xs.length; i++) {
            minX = Math.min(minX, xs[i]);
            minY = Math.min(minY, ys[i]);
            minZ = Math.min(minZ, zs[i]);
            maxX = Math.max(maxX, xs[i]);
            maxY = Math.max(maxY, ys[i]);
            maxZ = Math.max(maxZ, zs[i]);
        }
        return new BoundingBox(minX, minY - FILL, minZ, maxX, maxY + CLEAR, maxZ);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putIntArray("X", xs);
        tag.putIntArray("Y", ys);
        tag.putIntArray("Z", zs);
        tag.putIntArray("K", kinds);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
                            RandomSource random, BoundingBox chunk, ChunkPos chunkPos, BlockPos pivot) {
        BlockState[] paving = {Blocks.SMOOTH_BASALT.defaultBlockState(), Blocks.ANDESITE.defaultBlockState(),
                Blocks.TUFF.defaultBlockState(), Blocks.STONE.defaultBlockState()};
        BlockState dirt = ModBlocks.PHANTOM_COARSE_DIRT.get().defaultBlockState();
        BlockState soil = ModBlocks.PHANTOM_DIRT.get().defaultBlockState();
        BlockState farmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
        BlockState water = Blocks.WATER.defaultBlockState();
        CropBlock[] crops = {(CropBlock) Blocks.WHEAT, (CropBlock) Blocks.CARROTS, (CropBlock) Blocks.POTATOES,
                (CropBlock) Blocks.BEETROOTS};
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int i = 0; i < xs.length; i++) {
            if (!chunk.isInside(xs[i], chunk.minY(), zs[i])) {
                continue;
            }
            long seed = Mth.getSeed(xs[i], 0, zs[i]);
            int roll = Math.floorMod((int) (seed >> 16), 100);
            BlockState stone = paving[pave(Math.floorMod((int) (seed >> 40), 100))];
            if (kinds[i] == FRINGE) {
                wear(level, at, xs[i], ys[i], zs[i], roll < FRINGE_STONE ? stone : roll < FRINGE_STONE + FRINGE_DIRT ? dirt : null);
                continue;
            }
            boolean planted = kinds[i] >= CROP && kinds[i] < WATER;
            BlockState road = planted ? farmland : kinds[i] == WATER ? water
                    : kinds[i] == EDGE && roll < EDGE_DIRT ? dirt : stone;
            BlockState fill = planted ? soil : kinds[i] == WATER ? stone : road;
            for (int dy = 1; dy <= CLEAR; dy++) {
                at.set(xs[i], ys[i] + dy, zs[i]);
                if (!level.getBlockState(at).isAir()) {
                    level.setBlock(at, air, 2);
                }
            }
            at.set(xs[i], ys[i], zs[i]);
            level.setBlock(at, road, 2);
            if (planted) {
                CropBlock crop = crops[kinds[i] - CROP];
                level.setBlock(at.set(xs[i], ys[i] + 1, zs[i]), crop.getStateForAge(roll % (crop.getMaxAge() + 1)), 2);
            }
            for (int dy = 1; dy <= FILL; dy++) {
                at.set(xs[i], ys[i] - dy, zs[i]);
                BlockState state = level.getBlockState(at);
                if (!state.isAir() && !state.canBeReplaced() && state.getFluidState().isEmpty()) {
                    break;
                }
                level.setBlock(at, fill, 2);
            }
        }
    }

    private static int pave(int pick) {
        for (int i = 0; i < PAVING_SHARE.length; i++) {
            pick -= PAVING_SHARE[i];
            if (pick < 0) {
                return i;
            }
        }
        return 0;
    }

    private static void wear(WorldGenLevel level, BlockPos.MutableBlockPos at, int x, int y, int z, BlockState worn) {
        if (worn == null) {
            return;
        }
        for (int top = y + FRINGE_REACH; top >= y - FRINGE_REACH; top--) {
            BlockState state = level.getBlockState(at.set(x, top, z));
            if (state.isAir()) {
                continue;
            }
            if (GroundFit.natural(state) && level.getBlockState(at.set(x, top + 1, z)).canBeReplaced()) {
                if (!level.getBlockState(at).isAir()) {
                    level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
                }
                level.setBlock(at.set(x, top, z), worn, 2);
            }
            return;
        }
    }
}
