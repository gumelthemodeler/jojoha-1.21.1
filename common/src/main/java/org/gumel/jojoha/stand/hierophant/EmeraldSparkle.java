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

public record EmeraldSparkle(int colour) implements ParticleOptions {
    public static final MapCodec<EmeraldSparkle> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("colour").forGetter(EmeraldSparkle::colour)).apply(instance, EmeraldSparkle::new));

    public static final StreamCodec<ByteBuf, EmeraldSparkle> STREAM_CODEC =
            ByteBufCodecs.INT.map(EmeraldSparkle::new, EmeraldSparkle::colour);

    @Override
    public ParticleType<?> getType() {
        return ModRegistries.EMERALD_SPARKLE.get();
    }
}
