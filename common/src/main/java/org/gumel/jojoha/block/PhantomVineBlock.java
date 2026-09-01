package org.gumel.jojoha.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A strand of vine hanging off a Phantom tree.
 *
 * <h2>One block, no state</h2>
 *
 * <p>Vanilla's vine carries five booleans - one per face it clings to - and needs a multipart model
 * and a decorator that knows how to set them. This one only ever hangs downward from foliage, which
 * is what the tree wants and all it wants, so it has no state at all. That is what lets the tree
 * place it through {@code minecraft:attached_to_leaves}, a plain data decorator that writes one
 * blockstate and would have written vanilla's vine as an unattached, invisible stub.
 *
 * <p>Climbable through the {@code minecraft:climbable} tag rather than through code, which is where
 * that lives in modern versions - ladders, scaffolding and vanilla vines all get it the same way.
 */
public class PhantomVineBlock extends Block {
    public PhantomVineBlock(Properties properties) {
        super(properties);
    }

    /**
     * Only under something solid enough to hang from.
     *
     * <p>Without this a vine outlives the branch it grew on: fell the tree and the strands are left
     * floating where the leaves used to be. Checking the block above rather than a specific one means
     * a vine can hang from another vine, so a strand several long comes down all at once.
     */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.is(this) || above.isFaceSturdy(level, pos.above(), Direction.DOWN)
                || above.getBlock() instanceof net.minecraft.world.level.block.LeavesBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour,
                                     net.minecraft.world.level.LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        if (direction == Direction.UP && !canSurvive(state, level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }
}
