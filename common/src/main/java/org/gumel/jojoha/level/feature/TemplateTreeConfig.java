package org.gumel.jojoha.level.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

public record TemplateTreeConfig(List<Entry> trees) implements FeatureConfiguration {
    public record Entry(ResourceLocation template, int trunkX, int trunkZ) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("template").forGetter(Entry::template),
                Codec.INT.fieldOf("trunk_x").forGetter(Entry::trunkX),
                Codec.INT.fieldOf("trunk_z").forGetter(Entry::trunkZ)
        ).apply(instance, Entry::new));
    }

    public static final Codec<TemplateTreeConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Entry.CODEC.listOf().fieldOf("trees").forGetter(TemplateTreeConfig::trees)
            ).apply(instance, TemplateTreeConfig::new));
}
