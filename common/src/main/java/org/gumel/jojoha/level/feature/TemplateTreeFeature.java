package org.gumel.jojoha.level.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.gumel.jojoha.mixin.StructureTemplateAccessor;

import java.util.Optional;

public class TemplateTreeFeature extends Feature<TemplateTreeConfig> {
    private static final int PLACE_FLAGS = Block.UPDATE_CLIENTS;

    private static final int SINK = 1;

    public TemplateTreeFeature() {
        super(TemplateTreeConfig.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<TemplateTreeConfig> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        TemplateTreeConfig config = context.config();

        if (config.trees().isEmpty()) {
            return false;
        }

        TemplateTreeConfig.Entry entry = config.trees().get(random.nextInt(config.trees().size()));

        ServerLevel server = level.getLevel();
        Optional<StructureTemplate> found = server.getStructureManager().get(entry.template());
        if (found.isEmpty()) {
            return false;
        }

        StructureTemplate template = found.get();
        Rotation rotation = Rotation.getRandom(random);

        BlockPos pivot = new BlockPos(entry.trunkX(), 0, entry.trunkZ());

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setRotationPivot(pivot)
                .setIgnoreEntities(true)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR)
                .setRandom(random);

        BlockPos origin = groundedOrigin(level, context.origin());
        if (origin == null) {
            return false;
        }

        BlockPos corner = origin.subtract(pivot);

        for (StructureTemplate.StructureBlockInfo info
                : settings.getRandomPalette(
                        ((StructureTemplateAccessor) template).jojoha$palettes(), corner).blocks()) {
            if (info.state().isAir()) {
                continue;
            }
            BlockPos at = StructureTemplate.calculateRelativePosition(settings, info.pos()).offset(corner);
            if (!Wild.is(level.getBlockState(at))) {
                return false;
            }
        }

        return template.placeInWorld(level, corner, corner, settings, random, PLACE_FLAGS);
    }

    private static BlockPos groundedOrigin(WorldGenLevel level, BlockPos requested) {
        int x = requested.getX();
        int z = requested.getZ();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
                x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);

        int floor = level.getMinBuildHeight() + 1;

        while (cursor.getY() > floor) {
            BlockState state = level.getBlockState(cursor);

            if (state.is(BlockTags.LOGS)) {
                return null;
            }

            if (!state.isAir()
                    && !state.is(BlockTags.LEAVES)
                    && !state.is(BlockTags.REPLACEABLE)
                    && state.getFluidState().isEmpty()) {
                return new BlockPos(x, cursor.getY() + 1 - SINK, z);
            }

            cursor.move(0, -1, 0);
        }

        return null;
    }
}
