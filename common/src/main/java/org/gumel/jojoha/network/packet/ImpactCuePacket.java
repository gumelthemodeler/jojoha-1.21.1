package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record ImpactCuePacket(double x, double y, double z, float dx, float dy, float dz,
                              int tier, int sparks, float spread, float size, float shake, float frame,
                              int colour, int targetId, int strikerId, int seed) implements CustomPacketPayload {

    public static final Type<ImpactCuePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "impact_cue"));

    public static final StreamCodec<ByteBuf, ImpactCuePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeDouble(packet.x());
                buf.writeDouble(packet.y());
                buf.writeDouble(packet.z());
                buf.writeFloat(packet.dx());
                buf.writeFloat(packet.dy());
                buf.writeFloat(packet.dz());
                ByteBufCodecs.VAR_INT.encode(buf, packet.tier());
                ByteBufCodecs.VAR_INT.encode(buf, packet.sparks());
                buf.writeFloat(packet.spread());
                buf.writeFloat(packet.size());
                buf.writeFloat(packet.shake());
                buf.writeFloat(packet.frame());
                buf.writeInt(packet.colour());
                ByteBufCodecs.VAR_INT.encode(buf, packet.targetId() + 1);
                ByteBufCodecs.VAR_INT.encode(buf, packet.strikerId() + 1);
                buf.writeInt(packet.seed());
            },
            buf -> new ImpactCuePacket(
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readInt(),
                    ByteBufCodecs.VAR_INT.decode(buf) - 1,
                    ByteBufCodecs.VAR_INT.decode(buf) - 1,
                    buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
