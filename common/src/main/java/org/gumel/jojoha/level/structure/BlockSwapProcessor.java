package org.gumel.jojoha.level.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.Map;

public final class BlockSwapProcessor extends StructureProcessor {
    public static final Codec<Map<Block, Block>> SWAPS =
            Codec.unboundedMap(BuiltInRegistries.BLOCK.byNameCodec(), BuiltInRegistries.BLOCK.byNameCodec());
    public static final MapCodec<BlockSwapProcessor> CODEC =
            SWAPS.fieldOf("swaps").xmap(BlockSwapProcessor::new, processor -> processor.swaps);

    private final Map<Block, Block> swaps;

    public BlockSwapProcessor(Map<Block, Block> swaps) {
        this.swaps = swaps;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
                                                             StructureTemplate.StructureBlockInfo original,
                                                             StructureTemplate.StructureBlockInfo placed,
                                                             StructurePlaceSettings settings) {
        Block swap = swaps.get(placed.state().getBlock());
        if (swap == null) {
            return placed;
        }
        return new StructureTemplate.StructureBlockInfo(placed.pos(), copy(placed.state(), swap.defaultBlockState()),
                placed.nbt());
    }

    private static BlockState copy(BlockState from, BlockState to) {
        for (Property<?> property : from.getProperties()) {
            to = carry(from, to, property);
        }
        return to;
    }

    private static <T extends Comparable<T>> BlockState carry(BlockState from, BlockState to, Property<T> property) {
        return to.hasProperty(property) ? to.setValue(property, from.getValue(property)) : to;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModRegistries.BLOCK_SWAP_PROCESSOR.get();
    }
}
