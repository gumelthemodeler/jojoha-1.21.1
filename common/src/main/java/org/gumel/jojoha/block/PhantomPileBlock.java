package org.gumel.jojoha.block;

import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/**
 * A scatter of something lying on the ground - leaf litter, loose stones - in one to four helpings.
 *
 * <h2>Why it extends the petal block</h2>
 *
 * <p>Cherry petals already are this: a horizontal facing, a count from one to four, a flat model per
 * count, placing another helping when you use the item on a block that already has some, and
 * bonemeal to fill it in. Reimplementing that would be reimplementing it worse. The one thing it
 * hardcodes that does not suit us is the ground it will sit on, which is why that is the only thing
 * overridden here.
 *
 * <p>Its constructor is protected rather than public, so this cannot be built by instantiating the
 * vanilla class directly - a subclass is the way in, and is needed anyway for the ground rule.
 *
 * <h2>The ground rule</h2>
 *
 * <p>{@code BushBlock.mayPlaceOn} is {@code state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND)}.
 * Leaf litter wants exactly that and nothing more - litter belongs under trees, and trees grow on
 * soil. Loose stone wants more than that, because the biome's hillsides are bare phantom stone and
 * rock scattered everywhere except the rocky ground would be a strange omission. So the extra test
 * is a constructor argument rather than a second class.
 */
public class PhantomPileBlock extends PinkPetalsBlock {

    /** Ground this will sit on over and above what a bush accepts. */
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
