package org.gumel.jojoha.stand;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.List;

public final class PlacementTurn {
    public static final int TURNS = 48;

    private static final List<Direction.Axis> AXES =
            List.of(Direction.Axis.X, Direction.Axis.Y, Direction.Axis.Z);

    private static final List<Direction> AROUND = List.of(Direction.NORTH, Direction.EAST,
            Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN);

    private static final List<Direction> COMPASS =
            List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    private static final List<Direction.Axis> FLAT = List.of(Direction.Axis.X, Direction.Axis.Z);

    private static final List<SlabType> HALVES = List.of(SlabType.BOTTOM, SlabType.TOP);

    private PlacementTurn() {
    }

    public static BlockState turn(BlockState state, int steps) {
        if (state == null || steps <= 0) {
            return state;
        }

        if (state.hasProperty(BlockStateProperties.AXIS)) {
            return step(state, BlockStateProperties.AXIS, AXES, steps);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return step(state, BlockStateProperties.FACING, AROUND, steps);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return step(state, BlockStateProperties.HORIZONTAL_FACING, COMPASS, steps);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            return step(state, BlockStateProperties.HORIZONTAL_AXIS, FLAT, steps);
        }
        if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
            int from = state.getValue(BlockStateProperties.ROTATION_16);
            return state.setValue(BlockStateProperties.ROTATION_16, (from + steps) % 16);
        }
        return step(state, BlockStateProperties.SLAB_TYPE, HALVES, steps);
    }

    public static boolean turnable(BlockState state) {
        if (state == null) {
            return false;
        }
        for (Property<?> property : new Property<?>[] {BlockStateProperties.AXIS,
                BlockStateProperties.FACING, BlockStateProperties.HORIZONTAL_FACING,
                BlockStateProperties.HORIZONTAL_AXIS, BlockStateProperties.ROTATION_16}) {
            if (state.hasProperty(property)) {
                return true;
            }
        }
        return state.hasProperty(BlockStateProperties.SLAB_TYPE)
                && state.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.DOUBLE;
    }

    public static BlockState settled(LevelAccessor level, BlockPos at, BlockState state, int steps) {
        BlockState turned = turn(state, steps);
        if (turned == state) {
            return state;
        }
        turned = Block.updateFromNeighbourShapes(turned, level, at);
        return turned.isAir() || !turned.canSurvive(level, at) ? state : turned;
    }

    public static void apply(ServerLevel level, BlockPos at, ItemStack placed, int steps) {
        if (steps <= 0 || !(placed.getItem() instanceof BlockItem item)) {
            return;
        }

        BlockState state = level.getBlockState(at);
        if (!state.is(item.getBlock())) {
            return;
        }

        BlockState turned = settled(level, at, state, steps);
        if (turned != state) {
            level.setBlock(at, turned, Block.UPDATE_ALL);
        }
    }

    private static <T extends Comparable<T>> BlockState step(BlockState state, Property<T> property,
                                                             List<T> order, int steps) {
        if (!state.hasProperty(property)) {
            return state;
        }
        int from = order.indexOf(state.getValue(property));
        if (from < 0) {
            return state;
        }
        return state.setValue(property, order.get((from + steps) % order.size()));
    }
}
