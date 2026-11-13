package org.gumel.jojoha.labyrinth;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.gumel.jojoha.registry.ModRegistries;
import org.joml.Vector3f;

public class DungeonSpawnerBlock extends BaseEntityBlock {
    private static final DustParticleOptions EMBER = new DustParticleOptions(new Vector3f(0.85F, 0.08F, 0.06F), 0.8F);
    public static final MapCodec<DungeonSpawnerBlock> CODEC = simpleCodec(DungeonSpawnerBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty SPENT = BooleanProperty.create("spent");

    public DungeonSpawnerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, true).setValue(SPENT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE, SPENT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public static boolean torched(BlockGetter level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        if (above.getBlock() instanceof TorchBlock && !(above.getBlock() instanceof WallTorchBlock)) {
            return true;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState side = level.getBlockState(pos.relative(direction));
            if (side.getBlock() instanceof WallTorchBlock && side.getValue(WallTorchBlock.FACING) == direction) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ACTIVE, !torched(context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
                                     LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return state.setValue(ACTIVE, !torched(level, pos));
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool,
                                   boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (dropExperience) {
            popExperience(level, pos, 15 + level.random.nextInt(15) + level.random.nextInt(15));
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonSpawnerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type,
                ModRegistries.DUNGEON_SPAWNER_BLOCK_ENTITY.get(), DungeonSpawnerBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE) || state.getValue(SPENT)) {
            return;
        }
        double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
        double y = pos.getY() + 0.2 + random.nextDouble() * 0.6;
        double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
        level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 1.05, z, 0.0, 0.02, 0.0);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.01, 0.0);
        if (random.nextInt(3) == 0) {
            level.addParticle(EMBER, x, y + 0.2, z, 0.0, 0.0, 0.0);
        }
    }
}
