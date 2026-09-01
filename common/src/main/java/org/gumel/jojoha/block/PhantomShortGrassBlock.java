package org.gumel.jojoha.block;

import net.minecraft.world.level.block.TallGrassBlock;

/**
 * The biome's own short grass.
 *
 * <p>Nothing is added to vanilla's behaviour - it survives on soil, breaks instantly, is replaceable
 * and grows into its tall form under bonemeal. The class exists only because
 * {@link TallGrassBlock}'s constructor is protected, so a subclass is the only way to build one.
 */
public class PhantomShortGrassBlock extends TallGrassBlock {
    public PhantomShortGrassBlock(Properties properties) {
        super(properties);
    }
}
