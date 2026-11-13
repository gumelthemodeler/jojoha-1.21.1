package org.gumel.jojoha.level.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class PalmFeature extends Feature<NoneFeatureConfiguration> {
    public static final int TRUNK_MIN = 5;
    public static final int TRUNK_MAX = 8;
    public static final double LEAN_MAX = 2.5;
    public static final int FROND_MIN = 3;
    public static final int FROND_MAX = 4;

    private static final int[][] FRONDS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS;

    public PalmFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockState ground = level.getBlockState(origin.below());
        if (!(ground.is(BlockTags.SAND) || ground.is(BlockTags.DIRT)) || !level.isEmptyBlock(origin)) {
            return false;
        }
        int height = Mth.nextInt(random, TRUNK_MIN, TRUNK_MAX);
        Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        double reach = Mth.nextDouble(random, 0.8, LEAN_MAX);
        BlockState log = Blocks.JUNGLE_LOG.defaultBlockState();
        BlockPos top = origin;
        for (int y = 0; y < height; y++) {
            double t = y / (double) (height - 1);
            int shift = (int) Math.round(reach * t * t);
            BlockPos pos = origin.above(y).relative(lean, shift);
            if (!open(level, pos)) {
                return y > 0;
            }
            if (!pos.below().equals(top) && y > 0) {
                set(level, pos.below(), log);
            }
            set(level, pos, log);
            top = pos;
        }
        BlockState leaves = Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        set(level, top.above(), leaves);
        for (int[] frond : FRONDS) {
            int length = Mth.nextInt(random, FROND_MIN, FROND_MAX) - (frond[0] != 0 && frond[1] != 0 ? 1 : 0);
            for (int k = 1; k <= length; k++) {
                set(level, top.offset(frond[0] * k, k < length ? 1 : 0, frond[1] * k), leaves);
                if (k == length) {
                    set(level, top.offset(frond[0] * k, -1, frond[1] * k), leaves);
                }
            }
        }
        return true;
    }

    private static boolean open(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(BlockTags.REPLACEABLE) || state.is(BlockTags.LEAVES);
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (open(level, pos)) {
            level.setBlock(pos, state, PLACE_FLAGS);
        }
    }
}
