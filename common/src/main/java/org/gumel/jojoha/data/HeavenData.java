package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record HeavenData(float ascension, int fightTicks, int greatestHighTicks) {
    public static final HeavenData NONE = new HeavenData(0F, 0, 0);

    public static final Codec<HeavenData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("ascension", 0F).forGetter(HeavenData::ascension),
            Codec.INT.optionalFieldOf("fight_ticks", 0).forGetter(HeavenData::fightTicks),
            Codec.INT.optionalFieldOf("greatest_high_ticks", 0).forGetter(HeavenData::greatestHighTicks)
    ).apply(instance, HeavenData::new));
}
