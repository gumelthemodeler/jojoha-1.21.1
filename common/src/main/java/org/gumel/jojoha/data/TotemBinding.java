package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record TotemBinding(String dimension, BlockPos pos, int lives) {
    public static final int LIVES = 3;

    public static final TotemBinding NONE = new TotemBinding("", BlockPos.ZERO, 0);

    public static final Codec<TotemBinding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("dimension", "").forGetter(TotemBinding::dimension),
            BlockPos.CODEC.optionalFieldOf("pos", BlockPos.ZERO).forGetter(TotemBinding::pos),
            Codec.INT.optionalFieldOf("lives", 0).forGetter(TotemBinding::lives)
    ).apply(instance, TotemBinding::new));

    public static TotemBinding woken(ResourceKey<Level> dimension, BlockPos pos) {
        return new TotemBinding(dimension.location().toString(), pos.immutable(), LIVES);
    }

    public boolean holds() {
        return lives > 0 && !dimension.isEmpty();
    }

    public boolean at(ResourceKey<Level> level, BlockPos at) {
        return holds() && pos.equals(at) && dimension.equals(level.location().toString());
    }

    public TotemBinding spent() {
        return lives <= 1 ? NONE : new TotemBinding(dimension, pos, lives - 1);
    }
}
