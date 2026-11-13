package org.gumel.jojoha.level.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.gumel.jojoha.block.PhantomGrassBlock;

public class PhantomGrassConnectFeature extends Feature<NoneFeatureConfiguration> {
    private static final int DEPTH = 3;

    public PhantomGrassConnectFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean touched = false;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);

                for (int y = top; y > top - DEPTH; y--) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (!(state.getBlock() instanceof PhantomGrassBlock)) continue;

                    BlockState connected = PhantomGrassBlock.connect(level, cursor, state);
                    if (connected != state) {
                        level.setBlock(cursor.immutable(), connected, 2);
                        touched = true;
                    }
                    break;
                }
            }
        }
        return touched;
    }
}
