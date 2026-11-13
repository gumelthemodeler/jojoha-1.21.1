package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record MovementCuePacket(int kind, int entityId, double x, double y, double z, float dx, float dy, float dz,
                                int colour, boolean grounded, int wakeTicks, int seed) implements CustomPacketPayload {
    public static final Type<MovementCuePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "movement_cue"));

    public static final StreamCodec<ByteBuf, MovementCuePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                ByteBufCodecs.VAR_INT.encode(buf, packet.kind());
                ByteBufCodecs.VAR_INT.encode(buf, packet.entityId());
                buf.writeDouble(packet.x());
                buf.writeDouble(packet.y());
                buf.writeDouble(packet.z());
                buf.writeFloat(packet.dx());
                buf.writeFloat(packet.dy());
                buf.writeFloat(packet.dz());
                buf.writeInt(packet.colour());
                buf.writeBoolean(packet.grounded());
                ByteBufCodecs.VAR_INT.encode(buf, packet.wakeTicks());
                buf.writeInt(packet.seed());
            },
            buf -> new MovementCuePacket(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readInt(),
                    buf.readBoolean(),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
