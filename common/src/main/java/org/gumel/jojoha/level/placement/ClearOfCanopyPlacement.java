package org.gumel.jojoha.level.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.minecraft.util.RandomSource;
import org.gumel.jojoha.registry.ModRegistries;

public class ClearOfCanopyPlacement extends PlacementFilter {

    public static final MapCodec<ClearOfCanopyPlacement> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.intRange(1, 128).optionalFieldOf("height", 32)
                            .forGetter(p -> p.height),
                    Codec.intRange(0, 64).optionalFieldOf("margin", 0)
                            .forGetter(p -> p.margin)
            ).apply(instance, ClearOfCanopyPlacement::new));

    private final int height;

    private final int margin;

    public ClearOfCanopyPlacement(int height, int margin) {
        this.height = height;
        this.margin = margin;
    }

    private static final int[][] SAMPLES = {
            {0, 0},
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
            {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
    };

    private static final int[][] INNER = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1},
    };

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        for (int[] offset : SAMPLES) {
            if (occupied(context, pos.getX() + offset[0] * margin,
                    pos.getZ() + offset[1] * margin)) {
                return false;
            }
        }

        if (margin >= 4) {
            int half = margin / 2;
            for (int[] offset : INNER) {
                if (occupied(context, pos.getX() + offset[0] * half,
                        pos.getZ() + offset[1] * half)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean occupied(PlacementContext context, int x, int z) {
        int ground = context.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
        int top = Math.min(ground + height, context.getLevel().getMaxBuildHeight() - 1);

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int y = ground; y <= top; y++) {
            cursor.set(x, y, z);
            BlockState state = context.getLevel().getBlockState(cursor);

            if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModRegistries.CLEAR_OF_CANOPY.get();
    }
}
