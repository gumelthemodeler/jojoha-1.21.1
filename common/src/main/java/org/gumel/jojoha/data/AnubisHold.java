package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record AnubisHold(int use, int possessed) {
    public static final AnubisHold NONE = new AnubisHold(0, 0);

    public static final Codec<AnubisHold> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("use", 0).forGetter(AnubisHold::use),
            Codec.INT.optionalFieldOf("possessed", 0).forGetter(AnubisHold::possessed)
    ).apply(instance, AnubisHold::new));
}
