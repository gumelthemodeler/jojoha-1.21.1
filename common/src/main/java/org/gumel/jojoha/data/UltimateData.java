package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record UltimateData(Optional<ResourceLocation> adaptation, boolean wings, boolean headless,
                           Map<String, Integer> mastery, List<String> actives, List<String> passives,
                           Map<String, String> sources, boolean progenitor, int reform, boolean bornRolled) {
    public static final UltimateData NONE = new UltimateData(Optional.empty(), false, false, Map.of(), List.of(),
            List.of(), Map.of(), false, 0, false);

    public static final Codec<UltimateData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("adaptation").forGetter(UltimateData::adaptation),
            Codec.BOOL.optionalFieldOf("wings", false).forGetter(UltimateData::wings),
            Codec.BOOL.optionalFieldOf("headless", false).forGetter(UltimateData::headless),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("mastery", Map.of())
                    .forGetter(UltimateData::mastery),
            Codec.STRING.listOf().optionalFieldOf("actives", List.of()).forGetter(UltimateData::actives),
            Codec.STRING.listOf().optionalFieldOf("passives", List.of()).forGetter(UltimateData::passives),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("sources", Map.of())
                    .forGetter(UltimateData::sources),
            Codec.BOOL.optionalFieldOf("progenitor", false).forGetter(UltimateData::progenitor),
            Codec.INT.optionalFieldOf("reform", 0).forGetter(UltimateData::reform),
            Codec.BOOL.optionalFieldOf("born_rolled", false).forGetter(UltimateData::bornRolled)
    ).apply(instance, UltimateData::new));
}
