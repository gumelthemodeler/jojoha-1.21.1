package org.gumel.jojoha.stand.hierophant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.gumel.jojoha.registry.ModRegistries;

public record EmeraldChip(int colour) implements ParticleOptions {
    public static final MapCodec<EmeraldChip> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("colour").forGetter(EmeraldChip::colour)).apply(instance, EmeraldChip::new));

    public static final StreamCodec<ByteBuf, EmeraldChip> STREAM_CODEC =
            ByteBufCodecs.INT.map(EmeraldChip::new, EmeraldChip::colour);

    @Override
    public ParticleType<?> getType() {
        return ModRegistries.EMERALD_CHIP.get();
    }
}
