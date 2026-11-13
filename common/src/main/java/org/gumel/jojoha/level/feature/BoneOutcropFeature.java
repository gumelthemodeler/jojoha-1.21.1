package org.gumel.jojoha.level.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.Vec3;

public class BoneOutcropFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MIN_DROP = 3;

    private static final int PROBE = 4;
    private static final int ATTEMPTS = 6;
    private static final int SEARCH = 4;
    private static final int REACH = 11;
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS;

    private enum Shape {
        SPINE(30), RIBS(35), TUSK(20), JAW(15);

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
            return RIBS;
        }
    }

    public BoneOutcropFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            BlockPos origin = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, attempt == 0
                    ? context.origin()
                    : context.origin().offset(Mth.nextInt(random, -SEARCH, SEARCH), 0,
                            Mth.nextInt(random, -SEARCH, SEARCH)));
            Direction face = slope(level, origin);
            if (face != null && level.getBlockState(origin.below()).isSolidRender(level, origin.below())) {
                grow(level, random, origin, face);
                return true;
            }
        }
        return false;
    }

    private static Direction slope(WorldGenLevel level, BlockPos origin) {
        Direction face = null;
        int drop = MIN_DROP - 1;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos probe = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, origin.relative(direction, PROBE));
            int fall = origin.getY() - probe.getY();
            if (fall > drop) {
                drop = fall;
                face = direction;
            }
        }
        return face;
    }

    private static void grow(WorldGenLevel level, RandomSource random, BlockPos origin, Direction face) {
        BlockPos root = origin.relative(face.getOpposite(), 2).below(Mth.nextInt(random, 1, 3));
        switch (Shape.pick(random)) {
            case SPINE -> spine(level, random, root, face);
            case RIBS -> ribs(level, random, root, face);
            case TUSK -> tusk(level, random, root, face);
            case JAW -> jaw(level, random, root, face);
        }
    }

    private static void spine(WorldGenLevel level, RandomSource random, BlockPos root, Direction face) {
        Direction along = random.nextBoolean() ? face.getClockWise() : face.getCounterClockWise();
        Vec3 heading = vec(along).scale(0.8).add(vec(face).scale(0.45)).add(0, Mth.nextDouble(random, -0.25, 0.25), 0)
                .normalize();
        int length = Mth.nextInt(random, 8, REACH);
        Vec3 start = Vec3.atCenterOf(root).subtract(heading.scale(length / 2.0));
        BlockPos last = null;
        for (int i = 0; i <= length; i++) {
            BlockPos pos = BlockPos.containing(start.add(heading.scale(i)));
            if (pos.equals(last)) {
                continue;
            }
            last = pos;
            embed(level, pos, RibArchFeature.bone(axis(heading)));
            if ((i & 1) == 0) {
                int crest = Mth.nextInt(random, 1, 3);
                for (int up = 1; up <= crest; up++) {
                    embed(level, pos.above(up), RibArchFeature.bone(Direction.Axis.Y));
                }
                if (random.nextFloat() < 0.35F) {
                    embed(level, pos.relative(face), RibArchFeature.bone(face.getAxis()));
                }
            }
        }
    }

    private static void ribs(WorldGenLevel level, RandomSource random, BlockPos root, Direction face) {
        Direction along = face.getClockWise();
        int count = Mth.nextInt(random, 3, 5);
        int gap = Mth.nextInt(random, 2, 3);
        int reach = Mth.nextInt(random, 4, 7);
        int rise = Mth.nextInt(random, 3, 6);
        double curl = Mth.nextDouble(random, 0.6, 1.4);
        int first = -(count - 1) * gap / 2;
        for (int offset = first - 1; offset <= -first + 1; offset++) {
            embed(level, root.relative(along, offset), RibArchFeature.bone(along.getAxis()));
        }
        for (int i = 0; i < count; i++) {
            BlockPos base = root.relative(along, first + i * gap);
            double shrink = 1.0 - 0.15 * Math.abs(i - (count - 1) / 2.0);
            float broken = random.nextFloat() < 0.3F ? Mth.randomBetween(random, 0.45F, 0.8F) : 1F;
            curve(level, base, face, reach * shrink, rise * shrink, curl, broken);
        }
    }

    private static void tusk(WorldGenLevel level, RandomSource random, BlockPos root, Direction face) {
        Direction side = random.nextBoolean() ? face.getClockWise() : face.getCounterClockWise();
        int reach = Mth.nextInt(random, 6, 9);
        int rise = Mth.nextInt(random, 4, 8);
        curve(level, root, face, reach, rise, 0.9, 1F);
        curve(level, root.relative(side), face, reach - 2, rise - 1, 0.9, 1F);
    }

    private static void jaw(WorldGenLevel level, RandomSource random, BlockPos root, Direction face) {
        Direction along = face.getClockWise();
        int half = Mth.nextInt(random, 3, 5);
        int reach = Mth.nextInt(random, 3, 5);
        for (int side = -1; side <= 1; side += 2) {
            BlockPos hinge = root.relative(along, side * half);
            for (int out = 0; out <= reach; out++) {
                int inward = (int) Math.round(half * out / (double) (reach + 2));
                embed(level, hinge.relative(face, out).relative(along, -side * inward),
                        RibArchFeature.bone(face.getAxis()));
            }
        }
        for (int across = -half + 1; across <= half - 1; across++) {
            BlockPos chin = root.relative(face, reach).relative(along, across);
            embed(level, chin, RibArchFeature.bone(along.getAxis()));
            if (random.nextFloat() < 0.55F) {
                int tooth = Mth.nextInt(random, 1, 2);
                for (int up = 1; up <= tooth; up++) {
                    embed(level, chin.above(up).relative(face.getOpposite(), random.nextInt(2)),
                            RibArchFeature.bone(Direction.Axis.Y));
                }
            }
        }
    }

    private static void curve(WorldGenLevel level, BlockPos base, Direction face, double reach, double rise,
                              double curl, float length) {
        int steps = 24;
        int limit = Math.round(steps * length);
        BlockPos last = null;
        for (int step = 0; step <= limit; step++) {
            double t = step / (double) steps;
            double out = reach * Math.sin(t * Mth.HALF_PI);
            double up = rise * Math.pow(t, 1.0 + curl);
            BlockPos pos = base.relative(face, (int) Math.round(out)).above((int) Math.round(up));
            if (pos.equals(last)) {
                continue;
            }
            Direction.Axis axis = up > 0 && t > 0.6 ? Direction.Axis.Y : face.getAxis();
            embed(level, pos, RibArchFeature.bone(axis));
            last = pos;
        }
    }

    private static void embed(WorldGenLevel level, BlockPos pos, BlockState state) {
        BlockState there = level.getBlockState(pos);
        if (RibArchFeature.open(there) || (there.isSolidRender(level, pos) && !there.hasBlockEntity()
                && !there.is(Blocks.BEDROCK))) {
            level.setBlock(pos, state, PLACE_FLAGS);
        }
    }

    private static Direction.Axis axis(Vec3 heading) {
        double x = Math.abs(heading.x);
        double y = Math.abs(heading.y);
        double z = Math.abs(heading.z);
        return y > x && y > z ? Direction.Axis.Y : x > z ? Direction.Axis.X : Direction.Axis.Z;
    }

    private static Vec3 vec(Direction direction) {
        return new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }
}
