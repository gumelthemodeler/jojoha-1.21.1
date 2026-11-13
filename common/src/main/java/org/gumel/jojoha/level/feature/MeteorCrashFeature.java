package org.gumel.jojoha.level.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.gumel.jojoha.registry.ModBlocks;

public class MeteorCrashFeature extends Feature<NoneFeatureConfiguration> {
    private static final int CRATER_RADIUS_MIN = 7;
    private static final int CRATER_RADIUS_MAX = 11;
    private static final float CRATER_FLATTEN = 1.9F;

    private static final float BODY_SCALE = 0.55F;

    private static final double SHELL_THICKNESS = 1.6;

    private static final float ORE_SHARE = 0.11F;

    private static final double WOBBLE = 0.07;

    private static final double RIM_THICKNESS = 2.6;
    private static final float SCORCH_AT_CENTRE = 1.0F;
    private static final float SCORCH_AT_LIP = 0.15F;

    private static final int SPATTER_REACH = 4;
    private static final float SPATTER_CHANCE = 0.16F;

    private static final double BURIED = 0.35;

    public MeteorCrashFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int surface = origin.getY();
        int radius = CRATER_RADIUS_MIN + random.nextInt(CRATER_RADIUS_MAX - CRATER_RADIUS_MIN + 1);
        int depth = Math.max(3, Math.round(radius / CRATER_FLATTEN));

        BlockPos centre = new BlockPos(origin.getX(), surface - Math.round(depth * (float) BURIED),
                origin.getZ());

        if (level.isOutsideBuildHeight(centre.getY() - depth)
                || level.isOutsideBuildHeight(centre.getY() + radius)) {
            return false;
        }

        carve(level, random, centre, radius, depth);
        spatter(level, random, centre, radius);
        placeBody(level, random, centre, Math.max(2, Math.round(radius * BODY_SCALE)));
        return true;
    }

    private void carve(WorldGenLevel level, RandomSource random, BlockPos centre, int radius, int depth) {
        BlockState sand = ModBlocks.SUNSCORCHED_SAND.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -depth; y <= radius; y++) {
                    double stretched = y * CRATER_FLATTEN;
                    double distance = Math.sqrt(x * x + stretched * stretched + z * z);
                    double edge = radius * (1 + wobble(centre, x, y, z));

                    if (distance > edge) {
                        continue;
                    }

                    pos.set(centre.getX() + x, centre.getY() + y, centre.getZ() + z);
                    if (level.isOutsideBuildHeight(pos) || level.getBlockState(pos).isAir()) {
                        continue;
                    }

                    double intoRim = edge - distance;
                    if (intoRim > RIM_THICKNESS) {
                        setBlock(level, pos, Blocks.AIR.defaultBlockState());
                        continue;
                    }

                    float through = (float) (intoRim / RIM_THICKNESS);
                    float chance = SCORCH_AT_LIP + (SCORCH_AT_CENTRE - SCORCH_AT_LIP) * through;
                    if (random.nextFloat() < chance) {
                        setBlock(level, pos, sand);
                    }
                }
            }
        }
    }

    private void spatter(WorldGenLevel level, RandomSource random, BlockPos centre, int radius) {
        BlockState sand = ModBlocks.SUNSCORCHED_SAND.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int reach = radius + SPATTER_REACH;

        for (int x = -reach; x <= reach; x++) {
            for (int z = -reach; z <= reach; z++) {
                double flat = Math.sqrt(x * x + z * z);
                if (flat <= radius || flat > reach) {
                    continue;
                }

                float fade = 1F - (float) ((flat - radius) / SPATTER_REACH);
                if (random.nextFloat() > SPATTER_CHANCE * fade) {
                    continue;
                }

                int x0 = centre.getX() + x;
                int z0 = centre.getZ() + z;
                pos.set(x0, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x0, z0) - 1, z0);

                if (!level.isOutsideBuildHeight(pos) && level.getBlockState(pos).isSolid()) {
                    setBlock(level, pos, sand);
                }
            }
        }
    }

    private void placeBody(WorldGenLevel level, RandomSource random, BlockPos craterCentre, int radius) {
        BlockState shell = ModBlocks.METEORITE_ROCK.get().defaultBlockState();
        BlockState pale = ModBlocks.PALE_METEORITE_ROCK.get().defaultBlockState();
        BlockState ore = ModBlocks.METEORITE.get().defaultBlockState();

        BlockPos centre = craterCentre.offset(
                random.nextInt(5) - 2, -Math.max(1, radius / 2), random.nextInt(5) - 2);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= radius; y++) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    double edge = radius * (1 + wobble(centre, x, y, z));

                    if (distance > edge) {
                        continue;
                    }

                    pos.set(centre.getX() + x, centre.getY() + y, centre.getZ() + z);
                    if (level.isOutsideBuildHeight(pos)) {
                        continue;
                    }

                    if (distance > edge - SHELL_THICKNESS) {
                        setBlock(level, pos, shell);
                    } else {
                        setBlock(level, pos, random.nextFloat() < ORE_SHARE ? ore : pale);
                    }
                }
            }
        }
    }

    private static double wobble(BlockPos centre, int x, int y, int z) {
        int hash = (centre.getX() + x) * 73856093 ^ (centre.getY() + y) * 19349663
                ^ (centre.getZ() + z) * 83492791;
        return ((hash & 0xFF) / 255.0 - 0.5) * 2 * WOBBLE;
    }
}
