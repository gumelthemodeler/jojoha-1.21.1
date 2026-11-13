package org.gumel.jojoha.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class PhantomGrassBlock extends Block {

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;

    private static final Direction[] SIDES =
            {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public PhantomGrassBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false));
    }

    public static BooleanProperty property(Direction side) {
        return switch (side) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> throw new IllegalArgumentException("no property for " + side);
        };
    }

    public static BlockState connect(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction side : SIDES) {
            state = state.setValue(property(side), slopes(level, pos, side));
        }
        return state;
    }

    private static boolean slopes(BlockGetter level, BlockPos pos, Direction side) {
        return level.getBlockState(pos.relative(side).below()).getBlock() instanceof PhantomGrassBlock;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connect(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
                                  LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return connect(level, pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        refreshDiagonals(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement,
                         boolean moving) {
        super.onRemove(state, level, pos, replacement, moving);
        if (!state.is(replacement.getBlock())) refreshDiagonals(level, pos);
    }

    private static void refreshDiagonals(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        for (Direction side : SIDES) {
            BlockPos above = pos.relative(side).above();
            BlockState state = level.getBlockState(above);
            if (!(state.getBlock() instanceof PhantomGrassBlock)) continue;
            BlockState connected = connect(level, above, state);
            if (connected != state) level.setBlock(above, connected, 2);
        }
    }
}
