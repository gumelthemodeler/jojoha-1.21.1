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

/**
 * Gives generated turf its slope connections, once, as the chunk is made.
 *
 * <h2>Why this exists at all</h2>
 *
 * <p>Surface rules do not place blocks the way anything else does. They return a state and it is
 * written straight into the chunk's storage - no neighbour notifications, no {@code updateShape},
 * nothing the block itself can hook. So every square metre of turf the world generates would sit
 * there in its default state with all four sides off, and the connections would only ever appear
 * where a player had personally placed a block. The feature stage is the first moment the terrain
 * exists and ordinary code is allowed to look at it.
 *
 * <p>It is registered into the {@code top_layer_modification} step, which is where vanilla puts
 * {@code freeze_top_layer} - a pass that walks every column of the chunk and adjusts what it finds
 * on top. That is the same shape of job, in the same place, for the same reason.
 *
 * <h2>Cost</h2>
 *
 * <p>256 columns, a handful of blocks deep, four reads each, once per chunk ever. It is far cheaper
 * than the tree that generates beside it.
 */
public class PhantomGrassConnectFeature extends Feature<NoneFeatureConfiguration> {

    /**
     * How far below the heightmap to keep looking.
     *
     * <p>The heightmap is the top solid block, which is the turf itself on open ground. A couple
     * below covers turf that has something sitting on it - grass, a flower, a boulder's skirt - so
     * a block does not lose its connections merely because a fern grew on it.
     */
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
                    // Flag 2 is "tell clients, cascade nothing". During generation there are no
                    // clients yet and there is nothing to cascade to - every block in range is
                    // getting the same treatment from this same loop.
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
