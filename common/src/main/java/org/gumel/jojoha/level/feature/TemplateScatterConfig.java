package org.gumel.jojoha.level.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.gumel.jojoha.level.structure.BlockSwapProcessor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record TemplateScatterConfig(List<Entry> templates, int flatness,
                                    Optional<ResourceLocation> loot, Map<Block, Block> swaps)
        implements FeatureConfiguration {
    public record Entry(ResourceLocation template, int sink) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("template").forGetter(Entry::template),
                Codec.INT.optionalFieldOf("sink", 1).forGetter(Entry::sink)
        ).apply(instance, Entry::new));
    }

    public static final int DEFAULT_FLATNESS = 3;

    public static final Codec<TemplateScatterConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Entry.CODEC.listOf().fieldOf("templates")
                            .forGetter(TemplateScatterConfig::templates),
                    Codec.INT.optionalFieldOf("flatness", DEFAULT_FLATNESS)
                            .forGetter(TemplateScatterConfig::flatness),
                    ResourceLocation.CODEC.optionalFieldOf("loot")
                            .forGetter(TemplateScatterConfig::loot),
                    BlockSwapProcessor.SWAPS.optionalFieldOf("swaps", Map.of())
                            .forGetter(TemplateScatterConfig::swaps)
            ).apply(instance, TemplateScatterConfig::new));
}
