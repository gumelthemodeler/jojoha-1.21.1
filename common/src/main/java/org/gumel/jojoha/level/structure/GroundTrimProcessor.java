package org.gumel.jojoha.level.structure;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.Map;
import java.util.Set;

public final class GroundTrimProcessor extends StructureProcessor {
    public static final GroundTrimProcessor NONE =
            new GroundTrimProcessor(new GroundFit.Footprint(Set.of(), Map.of()), 0);
    public static final MapCodec<GroundTrimProcessor> CODEC = MapCodec.unit(() -> NONE);

    private final GroundFit.Footprint footprint;
    private final int sink;

    public GroundTrimProcessor(GroundFit.Footprint footprint, int sink) {
        this.footprint = footprint;
        this.sink = sink;
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
                                                             StructureTemplate.StructureBlockInfo original,
                                                             StructureTemplate.StructureBlockInfo placed,
                                                             StructurePlaceSettings settings) {
        return GroundFit.trimmed(footprint, original.pos(), original.state(), sink) ? null : placed;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModRegistries.GROUND_TRIM_PROCESSOR.get();
    }
}
