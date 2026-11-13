package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record PossessionSteerPacket(float yaw, float pitch, double x, double y, double z)
        implements CustomPacketPayload {

    public static final Type<PossessionSteerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "possession_steer"));

    public static final StreamCodec<ByteBuf, PossessionSteerPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeFloat(packet.yaw);
                buf.writeFloat(packet.pitch);
                buf.writeDouble(packet.x);
                buf.writeDouble(packet.y);
                buf.writeDouble(packet.z);
            },
            buf -> new PossessionSteerPacket(buf.readFloat(), buf.readFloat(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
