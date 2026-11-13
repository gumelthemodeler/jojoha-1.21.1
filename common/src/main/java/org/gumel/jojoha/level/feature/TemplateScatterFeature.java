package org.gumel.jojoha.level.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.gumel.jojoha.level.structure.BlockSwapProcessor;
import org.gumel.jojoha.level.structure.ContainerLootProcessor;
import org.gumel.jojoha.level.structure.SurfaceTemplatePiece;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TemplateScatterFeature extends Feature<TemplateScatterConfig> {
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS;

    private static final int SAMPLES = 5;

    private static final int SHIFT = 5;

    private static final int[][] SHIFTS = {
            {0, 0}, {SHIFT, 0}, {-SHIFT, 0}, {0, SHIFT}, {0, -SHIFT},
            {SHIFT, SHIFT}, {-SHIFT, -SHIFT}, {SHIFT, -SHIFT}, {-SHIFT, SHIFT},
    };

    public TemplateScatterFeature() {
        super(TemplateScatterConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<TemplateScatterConfig> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        TemplateScatterConfig config = context.config();

        if (config.templates().isEmpty()) {
            return false;
        }

        TemplateScatterConfig.Entry entry =
                config.templates().get(random.nextInt(config.templates().size()));

        ServerLevel server = level.getLevel();
        Optional<StructureTemplate> found = server.getStructureManager().get(entry.template());
        if (found.isEmpty()) {
            return false;
        }

        StructureTemplate template = found.get();
        Rotation rotation = Rotation.getRandom(random);
        Vec3i size = template.getSize();

        BlockPos pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);

        Vec3i turned = template.getSize(rotation);
        int halfX = turned.getX() / 2;
        int halfZ = turned.getZ() / 2;

        List<int[]> sites = new ArrayList<>();
        for (int[] shift : SHIFTS) {
            int[] site = survey(level, context.origin().getX() + shift[0], context.origin().getZ() + shift[1],
                    halfX, halfZ);
            if (site != null && site[3] <= config.flatness()) {
                sites.add(site);
            }
        }
        sites.sort(Comparator.comparingInt(site -> site[3]));
        for (int[] site : sites) {
            if (build(level, random, config, entry, template, rotation, pivot, site[0], site[1], site[2])) {
                return true;
            }
        }
        return false;
    }

    private static int[] survey(WorldGenLevel level, int x, int z, int halfX, int halfZ) {
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        int[][] corners = {
                {x - halfX, z - halfZ}, {x + halfX, z - halfZ},
                {x - halfX, z + halfZ}, {x + halfX, z + halfZ},
                {x, z},
        };
        for (int i = 0; i < SAMPLES; i++) {
            int ground = groundAt(level, corners[i][0], corners[i][1]);
            if (ground == Integer.MIN_VALUE
                    || !level.getFluidState(new BlockPos(corners[i][0], ground + 1, corners[i][1])).isEmpty()) {
                return null;
            }
            lowest = Math.min(lowest, ground);
            highest = Math.max(highest, ground);
        }
        return new int[]{x, z, lowest, highest - lowest};
    }

    private boolean build(WorldGenLevel level, RandomSource random, TemplateScatterConfig config,
                          TemplateScatterConfig.Entry entry, StructureTemplate template, Rotation rotation,
                          BlockPos pivot, int x, int z, int lowest) {
        int baseY = lowest + 1 - entry.sink();

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setRotationPivot(pivot)
                .setIgnoreEntities(true)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
                .setRandom(random);
        config.loot().ifPresent(loot -> settings.addProcessor(new ContainerLootProcessor(
                ResourceKey.create(Registries.LOOT_TABLE, loot), SurfaceTemplatePiece.BARREL_LOOT)));
        if (!config.swaps().isEmpty()) {
            settings.addProcessor(new BlockSwapProcessor(config.swaps()));
        }

        BlockPos corner = new BlockPos(x, baseY, z).subtract(pivot);

        if (!clearSite(level, template, settings, corner, baseY + entry.sink())) {
            return false;
        }

        return template.placeInWorld(level, corner, corner, settings, random, PLACE_FLAGS);
    }

    private static boolean clearSite(WorldGenLevel level, StructureTemplate template,
                                     StructurePlaceSettings settings, BlockPos corner, int groundY) {
        BoundingBox box = template.getBoundingBox(settings, corner);
        List<BlockPos> leaves = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), Math.max(box.minY(), groundY), box.minZ(),
                box.maxX(), box.maxY(), box.maxZ())) {
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS) || !Wild.is(state)) {
                return false;
            }
            if (state.is(BlockTags.LEAVES)) {
                leaves.add(pos.immutable());
            }
        }
        for (BlockPos pos : leaves) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), PLACE_FLAGS);
        }
        return true;
    }

    private static int groundAt(WorldGenLevel level, int x, int z) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);

        int floor = level.getMinBuildHeight() + 1;

        while (cursor.getY() > floor) {
            BlockState state = level.getBlockState(cursor);

            if (!state.isAir()
                    && !state.is(BlockTags.LEAVES)
                    && !state.is(BlockTags.REPLACEABLE)
                    && state.getFluidState().isEmpty()) {
                return cursor.getY();
            }

            cursor.move(0, -1, 0);
        }

        return Integer.MIN_VALUE;
    }
}
