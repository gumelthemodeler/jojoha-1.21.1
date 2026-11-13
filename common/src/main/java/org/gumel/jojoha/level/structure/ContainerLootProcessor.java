package org.gumel.jojoha.level.structure;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;
import org.gumel.jojoha.registry.ModRegistries;

public final class ContainerLootProcessor extends StructureProcessor {
    public static final MapCodec<ContainerLootProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("chest").forGetter(p -> p.chest),
            ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("barrel").forGetter(p -> p.barrel)
    ).apply(instance, ContainerLootProcessor::new));

    private final ResourceKey<LootTable> chest;
    private final ResourceKey<LootTable> barrel;

    public ContainerLootProcessor(ResourceKey<LootTable> chest, ResourceKey<LootTable> barrel) {
        this.chest = chest;
        this.barrel = barrel;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
                                                             StructureTemplate.StructureBlockInfo original,
                                                             StructureTemplate.StructureBlockInfo placed,
                                                             StructurePlaceSettings settings) {
        ResourceKey<LootTable> loot = tableFor(placed.state());
        if (loot == null) {
            return placed;
        }
        CompoundTag nbt = placed.nbt() == null ? new CompoundTag() : placed.nbt().copy();
        nbt.remove("Items");
        nbt.putString(RandomizableContainer.LOOT_TABLE_TAG, loot.location().toString());
        nbt.putLong(RandomizableContainer.LOOT_TABLE_SEED_TAG, settings.getRandom(placed.pos()).nextLong());
        return new StructureTemplate.StructureBlockInfo(placed.pos(), placed.state(), nbt);
    }

    private ResourceKey<LootTable> tableFor(BlockState state) {
        if (state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)) {
            return chest;
        }
        return state.is(Blocks.BARREL) ? barrel : null;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModRegistries.CONTAINER_LOOT_PROCESSOR.get();
    }
}
