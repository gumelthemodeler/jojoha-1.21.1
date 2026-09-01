package org.gumel.jojoha.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.gumel.jojoha.registry.ModRegistries;

/**
 * A camera, standing where somebody put it.
 *
 * <p>The move used to take a camera out of the inventory, which asked nothing of the player beyond
 * having one. Placing it makes the picture something you set up: you choose where the camera stands
 * and which way it looks, and then you break it. That is a small ritual rather than a menu action,
 * and it is the difference between using an item and taking a photograph.
 *
 * <p>Nothing happens on right-click, deliberately. The camera has exactly one use and Hermit Purple
 * is it - see CameraCrushSkill. A block that also did something on its own would invite players to
 * find out what, and the answer would be nothing.
 *
 * <h2>Invisible to the block renderer</h2>
 *
 * <p>Everything visible about it is the animated model, drawn by CameraBlockRenderer from the block
 * entity. The blockstate still points at a real (empty) model file because the game loads one either
 * way and complains loudly about a missing one.
 */
public final class CameraBlock extends BaseEntityBlock {
    public static final MapCodec<CameraBlock> CODEC = simpleCodec(CameraBlock::new);

    /**
     * Which way the lens points.
     *
     * <p>Vanilla's own property rather than one of ours, because GeckoLib's block renderer looks for
     * exactly this one when it decides how to turn the model - see {@code GeoBlockRenderer.getFacing}.
     * A property of our own would have left every camera facing north.
     */
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;

    /**
     * The model's own footprint, per facing.
     *
     * <p>Measured off the geometry rather than eyeballed: the cubes span x -5..5, y 0..9 and
     * z -4..6 in model units, which is the north box below, and the other three are that box turned
     * about the block centre. GeckoLib leaves a north-facing model unrotated and turns the rest by
     * 90, 180 and 270 - confirmed against {@code rotateBlock} - so these line up with what is drawn.
     */
    private static final VoxelShape NORTH_SHAPE = box(3, 0, 4, 13, 9, 14);
    private static final VoxelShape SOUTH_SHAPE = box(3, 0, 2, 13, 9, 12);
    private static final VoxelShape WEST_SHAPE = box(4, 0, 3, 14, 9, 13);
    private static final VoxelShape EAST_SHAPE = box(2, 0, 3, 12, 9, 13);

    public CameraBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    /**
     * Placed looking back at whoever put it down.
     *
     * <p>The lens and the slot the picture comes out of are the same face of the model, so pointing
     * it at the player means the photograph arrives on the side they are standing on. A camera
     * facing away would print into the wall behind it.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    /** Drawn by the block entity renderer, so the block itself contributes nothing. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /**
     * A camera broken while it was developing gives the picture up rather than eating it.
     *
     * <p>The answer was already paid for by the time the print started, so losing it to a pickaxe -
     * or to somebody else's - would be the move taking the cost twice. The print itself clears the
     * held stack before it removes the block, so the normal finish does not drop a second copy.
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement,
                            boolean movedByPiston) {
        if (!state.is(replacement.getBlock())
                && level.getBlockEntity(pos) instanceof CameraBlockEntity camera) {
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                    camera.takePending());
        }
        super.onRemove(state, level, pos, replacement, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CameraBlockEntity(pos, state);
    }

    /**
     * Ticked only while it is developing something, and only on the server.
     *
     * <p>The client has no countdown to run - the animation is its own clock once triggered - and a
     * camera sitting unused is not doing anything worth a tick either. The ticker is handed out
     * regardless and returns immediately when idle, which is cheaper than swapping tickers in and
     * out of a block that is about to be broken anyway.
     */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModRegistries.CAMERA_BLOCK_ENTITY.get(),
                CameraBlockEntity::serverTick);
    }
}
