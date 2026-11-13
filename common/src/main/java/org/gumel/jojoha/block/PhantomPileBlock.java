package org.gumel.jojoha.block;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

public class PhantomPileBlock extends PinkPetalsBlock {
    private final Predicate<BlockState> alsoOn;

    public PhantomPileBlock(Properties properties, Predicate<BlockState> alsoOn) {
        super(properties);
        this.alsoOn = alsoOn;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return super.mayPlaceOn(state, level, pos) || alsoOn.test(state);
    }
}
