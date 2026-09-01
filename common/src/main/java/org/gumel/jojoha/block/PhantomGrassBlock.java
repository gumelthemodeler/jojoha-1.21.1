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

/**
 * The turf of the Phantom Highlands, which wraps its grass around a step down.
 *
 * <h2>What the four properties mean</h2>
 *
 * <p>Each of the four horizontal booleans says one thing: the ground on that side continues, one
 * block lower. That is a slope - a staircase of terrain - and it is the case where vanilla's grass
 * block looks wrong, because every step of the hillside turns its dirt side to the camera and a
 * slope reads as a stack of dirt blocks with green lids.
 *
 * <p>When the property is true that face is drawn as solid turf, so the hillside reads as one
 * continuous surface. When it is false the face is drawn exactly like vanilla's - dirt with a
 * tinted fringe along the top - which is the right look for a cliff, a lone block, or the wall of a
 * hole, none of which are slopes and none of which should be grassed down their whole height.
 *
 * <h2>Why the check is a diagonal, and what that costs</h2>
 *
 * <p>"The ground continues one lower" is the block at {@code pos.relative(dir).below()}, which is
 * diagonal from this one. Minecraft only notifies the six blocks orthogonally touching a change, so
 * a diagonal neighbour appearing or vanishing never reaches us on its own. That is handled from the
 * other end: this block tells its own four diagonal neighbours to look again whenever one of these
 * is placed or removed. Since the condition only ever asks about phantom turf, those are the only
 * changes that can matter, so the notification is complete rather than merely likely.
 *
 * <p>Generated terrain is a separate problem and is solved separately - see
 * {@code PhantomGrassConnectFeature}. Surface rules write a fixed state straight into the chunk
 * with no block updates at all, so nothing here would ever fire for the millions of blocks that
 * matter most.
 */
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

    /**
     * Takes a state and answers what its four sides should be in this world, at this position.
     *
     * <p>Kept static and taking a plain {@link BlockGetter} so the worldgen pass can call it against
     * a chunk region, where there is no {@link Level} to hand.
     */
    public static BlockState connect(BlockGetter level, BlockPos pos, BlockState state) {
        for (Direction side : SIDES) {
            state = state.setValue(property(side), slopes(level, pos, side));
        }
        return state;
    }

    /** True when the ground on this side carries on one block lower, which is what a slope is. */
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

    /**
     * An orthogonal neighbour changed. Ours is a diagonal condition, so this cannot be narrowed to
     * the one direction that moved - a change directly below us moves what our four sides see just
     * as much as one beside us does. Recomputing all four is four block reads and is not worth
     * being clever about.
     */
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

    /**
     * Tells the four turf blocks that can see this one - each one up and one across - to look again.
     *
     * <p>The flag is 2, "send to clients, do not cascade updates". Cascading would be wrong here:
     * these are recomputations, not changes with consequences of their own, and letting them
     * propagate would walk a whole hillside on every block placed.
     */
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
