package org.gumel.jojoha.level.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class BoneFragmentFeature extends Feature<NoneFeatureConfiguration> {
    private static final int RUBBLE_SPREAD = 2;
    private static final int MAX_STEP = 2;
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS;

    public BoneFragmentFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (!level.getBlockState(origin.below()).isSolidRender(level, origin.below())) {
            return false;
        }
        Direction.Axis axis = random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
        switch (random.nextInt(4)) {
            case 0 -> femur(level, random, origin, axis);
            case 1 -> shard(level, random, origin);
            case 2 -> rib(level, random, origin, axis);
            default -> rubble(level, random, origin);
        }
        return true;
    }

    private static void femur(WorldGenLevel level, RandomSource random, BlockPos origin, Direction.Axis axis) {
        int length = Mth.nextInt(random, 2, 4);
        Direction along = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        Direction side = along.getClockWise();
        for (int i = 0; i < length; i++) {
            ground(level, origin, origin.relative(along, i), RibArchFeature.bone(axis));
        }
        if (random.nextBoolean()) {
            BlockPos end = origin.relative(along, random.nextBoolean() ? -1 : length);
            ground(level, origin, end.relative(side), RibArchFeature.bone(side.getAxis()));
        }
    }

    private static void shard(WorldGenLevel level, RandomSource random, BlockPos origin) {
        int height = Mth.nextInt(random, 1, 3);
        for (int i = 0; i < height; i++) {
            set(level, origin.above(i), RibArchFeature.bone(Direction.Axis.Y));
        }
        if (height > 1 && random.nextBoolean()) {
            Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            set(level, origin.above(height).relative(lean), RibArchFeature.bone(lean.getAxis()));
        }
    }

    private static void rib(WorldGenLevel level, RandomSource random, BlockPos origin, Direction.Axis axis) {
        Direction out = Direction.fromAxisAndDirection(axis,
                random.nextBoolean() ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        int radius = Mth.nextInt(random, 2, 3);
        BlockPos last = null;
        for (int step = 0; step <= 8; step++) {
            double t = step / 8.0 * Mth.HALF_PI;
            BlockPos pos = origin.relative(out, (int) Math.round(radius * (1 - Math.cos(t))))
                    .above((int) Math.round(radius * Math.sin(t)));
            if (!pos.equals(last)) {
                set(level, pos, RibArchFeature.bone(t < Mth.PI / 4 ? Direction.Axis.Y : axis));
                last = pos;
            }
        }
    }

    private static void rubble(WorldGenLevel level, RandomSource random, BlockPos origin) {
        int pieces = Mth.nextInt(random, 2, 4);
        for (int i = 0; i < pieces; i++) {
            BlockPos column = origin.offset(Mth.nextInt(random, -RUBBLE_SPREAD, RUBBLE_SPREAD), 0,
                    Mth.nextInt(random, -RUBBLE_SPREAD, RUBBLE_SPREAD));
            Direction.Axis axis = Direction.Axis.VALUES[random.nextInt(3)];
            ground(level, origin, column, RibArchFeature.bone(axis));
        }
    }

    private static void ground(WorldGenLevel level, BlockPos origin, BlockPos column, BlockState state) {
        BlockPos top = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, column);
        if (Math.abs(top.getY() - origin.getY()) > MAX_STEP
                || !level.getBlockState(top.below()).isSolidRender(level, top.below())) {
            return;
        }
        set(level, top, state);
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (RibArchFeature.open(level.getBlockState(pos))) {
            level.setBlock(pos, state, PLACE_FLAGS);
        }
    }
}
