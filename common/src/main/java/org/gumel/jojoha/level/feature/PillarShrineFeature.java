package org.gumel.jojoha.level.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;
import org.gumel.jojoha.Jojoha;

public class PillarShrineFeature extends Feature<NoneFeatureConfiguration> {
    public static final int HALF = 5;
    public static final int DEPTH = 4;
    public static final int WALL_HEIGHT = 4;
    public static final int RAMP = 4;
    private static final int COLUMN = 3;
    private static final float MOSSY = 0.25F;
    private static final float CRACKED = 0.15F;
    private static final float CRUMBLED = 0.2F;
    private static final int COBWEBS = 4;
    private static final int PLACE_FLAGS = 2;

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "chests/pillar_shrine"));

    public PillarShrineFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int floor = origin.getY() - DEPTH;
        if (level.isOutsideBuildHeight(floor - 1) || level.isOutsideBuildHeight(origin.getY() + WALL_HEIGHT)) {
            return false;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = -HALF; x <= HALF; x++) {
            for (int z = -HALF; z <= HALF; z++) {
                pos.set(origin.getX() + x, floor - 1, origin.getZ() + z);
                setBlock(level, pos, brick(random));
                boolean wall = Math.abs(x) == HALF || Math.abs(z) == HALF;
                for (int y = 0; y <= DEPTH + WALL_HEIGHT; y++) {
                    pos.set(origin.getX() + x, floor + y, origin.getZ() + z);
                    if (wall && y < DEPTH + 1 && !(y >= DEPTH - 1 && random.nextFloat() < CRUMBLED)) {
                        setBlock(level, pos, brick(random));
                    } else if (!level.getBlockState(pos).isAir()) {
                        setBlock(level, pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int height = 2 + random.nextInt(DEPTH);
                for (int y = 0; y < height; y++) {
                    pos.set(origin.getX() + sx * COLUMN, floor + y, origin.getZ() + sz * COLUMN);
                    setBlock(level, pos, y == height - 1 && height < DEPTH + 1
                            ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                            : Blocks.STONE_BRICK_WALL.defaultBlockState());
                }
            }
        }

        ramp(level, random, origin, floor, pos);

        pos.set(origin.getX(), floor, origin.getZ());
        setBlock(level, pos, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        pos.set(origin.getX(), floor + 1, origin.getZ());
        setBlock(level, pos, Blocks.CHISELED_TUFF.defaultBlockState());
        pos.set(origin.getX(), floor + 2, origin.getZ());
        setBlock(level, pos, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());

        BlockPos chest = new BlockPos(origin.getX(), floor, origin.getZ() - COLUMN);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), PLACE_FLAGS);
        BlockEntity entity = level.getBlockEntity(chest);
        if (entity instanceof RandomizableContainerBlockEntity container) {
            container.setLootTable(LOOT);
            container.setLootTableSeed(random.nextLong());
        }

        for (int i = 0; i < COBWEBS; i++) {
            pos.set(origin.getX() + random.nextInt(HALF * 2 - 1) - HALF + 1, floor + DEPTH - 1 - random.nextInt(2),
                    origin.getZ() + random.nextInt(HALF * 2 - 1) - HALF + 1);
            if (level.getBlockState(pos).isAir()) {
                setBlock(level, pos, Blocks.COBWEB.defaultBlockState());
            }
        }
        return true;
    }

    private void ramp(WorldGenLevel level, RandomSource random, BlockPos origin, int floor,
                      BlockPos.MutableBlockPos pos) {
        for (int step = 0; step < RAMP; step++) {
            int z = HALF + step;
            for (int x = -1; x <= 1; x++) {
                pos.set(origin.getX() + x, floor + step, origin.getZ() + z);
                setBlock(level, pos, Blocks.STONE_BRICK_STAIRS.defaultBlockState()
                        .setValue(StairBlock.FACING, Direction.SOUTH));
                pos.set(origin.getX() + x, floor + step - 1, origin.getZ() + z);
                if (!level.getBlockState(pos).isSolid()) {
                    setBlock(level, pos, brick(random));
                }
                for (int y = step + 1; y <= DEPTH + WALL_HEIGHT; y++) {
                    pos.set(origin.getX() + x, floor + y, origin.getZ() + z);
                    if (!level.getBlockState(pos).isAir()) {
                        setBlock(level, pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    private static BlockState brick(RandomSource random) {
        float roll = random.nextFloat();
        if (roll < MOSSY) {
            return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        }
        if (roll < MOSSY + CRACKED) {
            return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        }
        return Blocks.STONE_BRICKS.defaultBlockState();
    }
}
