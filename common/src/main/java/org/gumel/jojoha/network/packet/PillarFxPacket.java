package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record PillarFxPacket(int kind, int a, int b, int ticks, double x, double y, double z,
                             float dx, float dy, float dz) implements CustomPacketPayload {
    public static final Type<PillarFxPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "pillar_fx"));

    public static final StreamCodec<ByteBuf, PillarFxPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeByte(packet.kind());
                buf.writeInt(packet.a());
                buf.writeInt(packet.b());
                buf.writeShort(packet.ticks());
                buf.writeDouble(packet.x());
                buf.writeDouble(packet.y());
                buf.writeDouble(packet.z());
                buf.writeFloat(packet.dx());
                buf.writeFloat(packet.dy());
                buf.writeFloat(packet.dz());
            },
            buf -> new PillarFxPacket(buf.readByte(), buf.readInt(), buf.readInt(), buf.readShort(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
