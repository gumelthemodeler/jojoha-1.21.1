package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record SpiritData(Optional<ResourceLocation> aligned, float alignOdds, float hamonSpent) {
    public static final SpiritData NONE = new SpiritData(Optional.empty(), 0F, 0F);

    public static final Codec<SpiritData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("aligned").forGetter(SpiritData::aligned),
            Codec.FLOAT.optionalFieldOf("align_odds", 0F).forGetter(SpiritData::alignOdds),
            Codec.FLOAT.optionalFieldOf("hamon_spent", 0F).forGetter(SpiritData::hamonSpent)
    ).apply(instance, SpiritData::new));
}
