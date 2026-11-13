package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record GroundBreakerFxPacket(int throwerId, double x, double y, double z, float radius,
                                    int phase, int ticks) implements CustomPacketPayload {
    public static final int TELEGRAPH = 0;

    public static final int SLAM = 1;

    public static final Type<GroundBreakerFxPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ground_breaker_fx"));

    public static final StreamCodec<ByteBuf, GroundBreakerFxPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                ByteBufCodecs.VAR_INT.encode(buf, packet.throwerId());
                buf.writeDouble(packet.x());
                buf.writeDouble(packet.y());
                buf.writeDouble(packet.z());
                buf.writeFloat(packet.radius());
                ByteBufCodecs.VAR_INT.encode(buf, packet.phase());
                ByteBufCodecs.VAR_INT.encode(buf, packet.ticks());
            },
            buf -> new GroundBreakerFxPacket(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
