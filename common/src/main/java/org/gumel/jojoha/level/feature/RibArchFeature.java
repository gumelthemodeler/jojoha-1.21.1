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
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class RibArchFeature extends Feature<NoneFeatureConfiguration> {
    public static final int RIBS_MIN = 3;
    public static final int RIBS_MAX = 6;
    public static final int SPAN_MIN = 3;
    public static final int SPAN_MAX = 7;
    public static final int RISE_MIN = 5;
    public static final int RISE_MAX = 11;

    private static final double TAPER = 0.15;
    private static final float BROKEN_CHANCE = 0.25F;
    private static final int FOOT_DEPTH = 5;
    private static final int SPINE_OVERHANG = 2;
    private static final int SPINE_MIN = 9;
    private static final int SPINE_MAX = 16;
    private static final int STEPS = 40;
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS;

    private enum Shape {
        RIBCAGE(40), SUNKEN(25), CLAWS(20), SPINE(15);

        final int weight;

        Shape(int weight) {
            this.weight = weight;
        }

        static Shape pick(RandomSource random) {
            int roll = random.nextInt(100);
            for (Shape shape : values()) {
                roll -= shape.weight;
                if (roll < 0) {
                    return shape;
                }
            }
            return RIBCAGE;
        }
    }

    public RibArchFeature(Codec<NoneFeatureConfiguration> codec) {
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
        switch (Shape.pick(random)) {
            case RIBCAGE -> ribcage(level, random, origin, axis, 0, true);
            case SUNKEN -> ribcage(level, random, origin, axis, Mth.nextInt(random, 2, 4), random.nextBoolean());
            case CLAWS -> claws(level, random, origin, axis);
            case SPINE -> spine(level, random, origin, axis);
        }
        return true;
    }

    private static void ribcage(WorldGenLevel level, RandomSource random, BlockPos origin, Direction.Axis axis,
                                int sink, boolean spine) {
        Direction.Axis across = across(axis);
        int ribs = Mth.nextInt(random, RIBS_MIN, RIBS_MAX);
        int gap = Mth.nextInt(random, 2, 3);
        int span = Mth.nextInt(random, SPAN_MIN, SPAN_MAX);
        int rise = Mth.nextInt(random, RISE_MIN, RISE_MAX);
        double curl = Mth.nextDouble(random, 0.15, 0.5);
        BlockPos base = origin.below(sink);
        double middle = (ribs - 1) / 2.0;
        int first = (int) Math.round(-middle * gap);
        for (int i = 0; i < ribs; i++) {
            double shrink = 1.0 - TAPER * Math.abs(i - middle);
            float left = random.nextFloat() < BROKEN_CHANCE ? Mth.randomBetween(random, 0.35F, 0.8F) : 1F;
            float right = random.nextFloat() < BROKEN_CHANCE ? Mth.randomBetween(random, 0.35F, 0.8F) : 1F;
            rib(level, base, axis, first + i * gap, span * shrink, rise * shrink, curl, left, right);
        }
        if (spine) {
            for (int along = first - SPINE_OVERHANG; along <= -first + SPINE_OVERHANG; along++) {
                set(level, offset(base, axis, along, across, 0, rise), bone(axis));
            }
        }
    }

    private static void claws(WorldGenLevel level, RandomSource random, BlockPos origin, Direction.Axis axis) {
        int claws = Mth.nextInt(random, 2, 4);
        int gap = Mth.nextInt(random, 2, 3);
        int span = Mth.nextInt(random, SPAN_MIN, SPAN_MAX);
        int rise = Mth.nextInt(random, RISE_MIN + 2, RISE_MAX + 2);
        double curl = Mth.nextDouble(random, 0.5, 0.9);
        boolean left = random.nextBoolean();
        int first = -(claws - 1) * gap / 2;
        for (int i = 0; i < claws; i++) {
            double shrink = 1.0 - 0.12 * i;
            float reach = Mth.randomBetween(random, 0.85F, 1F);
            rib(level, origin, axis, first + i * gap, span * shrink, rise * shrink, curl,
                    left ? reach : 0F, left ? 0F : reach);
        }
    }

    private static void spine(WorldGenLevel level, RandomSource random, BlockPos origin, Direction.Axis axis) {
        Direction.Axis across = across(axis);
        int length = Mth.nextInt(random, SPINE_MIN, SPINE_MAX);
        double wave = Mth.nextDouble(random, 1.0, 2.5);
        double phase = random.nextDouble() * Mth.TWO_PI;
        for (int along = -length / 2; along <= length / 2; along++) {
            int out = (int) Math.round(wave * Math.sin(along * 0.35 + phase));
            BlockPos column = offset(origin, axis, along, across, out, 0);
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, column);
            if (Math.abs(ground.getY() - origin.getY()) > 4) {
                continue;
            }
            set(level, ground, bone(axis));
            if ((along & 1) == 0) {
                set(level, ground.above(), bone(Direction.Axis.Y));
                if (random.nextFloat() < 0.4F) {
                    set(level, ground.above(2), bone(Direction.Axis.Y));
                }
            } else if (random.nextFloat() < 0.5F) {
                int side = random.nextBoolean() ? 1 : -1;
                set(level, offset(ground, axis, 0, across, side, 0), bone(across));
            }
        }
    }

    private static void rib(WorldGenLevel level, BlockPos origin, Direction.Axis axis, int along,
                            double span, double rise, double curl, float left, float right) {
        Direction.Axis across = across(axis);
        for (int side = -1; side <= 1; side += 2) {
            BlockPos last = null;
            int limit = Math.round(STEPS * (side < 0 ? left : right));
            if (limit <= 0) {
                continue;
            }
            for (int step = 0; step <= limit; step++) {
                double t = step / (double) STEPS * Mth.HALF_PI;
                double out = span * Math.cos(t) * (1.0 + curl * Math.sin(t) * Math.cos(t));
                double up = rise * Math.sin(t);
                BlockPos pos = offset(origin, axis, along, across, (int) Math.round(side * out), (int) Math.round(up));
                if (pos.equals(last)) {
                    continue;
                }
                set(level, pos, bone(t < Mth.PI / 3.5 ? Direction.Axis.Y : across));
                if (step == 0) {
                    foot(level, pos);
                }
                last = pos;
            }
        }
    }

    private static void foot(WorldGenLevel level, BlockPos base) {
        BlockPos.MutableBlockPos cursor = base.mutable();
        for (int i = 0; i < FOOT_DEPTH; i++) {
            cursor.move(Direction.DOWN);
            if (!open(level.getBlockState(cursor))) {
                return;
            }
            level.setBlock(cursor, bone(Direction.Axis.Y), PLACE_FLAGS);
        }
    }

    private static Direction.Axis across(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    private static BlockPos offset(BlockPos origin, Direction.Axis axis, int along, Direction.Axis across,
                                   int out, int up) {
        return axis == Direction.Axis.X
                ? origin.offset(along, up, out)
                : origin.offset(out, up, along);
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (open(level.getBlockState(pos))) {
            level.setBlock(pos, state, PLACE_FLAGS);
        }
    }

    static boolean open(BlockState state) {
        return state.isAir() || state.is(BlockTags.REPLACEABLE) || state.is(Blocks.WATER);
    }

    static BlockState bone(Direction.Axis axis) {
        return Blocks.BONE_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }
}
