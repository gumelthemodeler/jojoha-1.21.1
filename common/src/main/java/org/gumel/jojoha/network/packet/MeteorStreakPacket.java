package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record MeteorStreakPacket(double x, double y, double z, float vx, float vy, float vz, int life, boolean big)
        implements CustomPacketPayload {

    public static final Type<MeteorStreakPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "meteor_streak"));

    public static final StreamCodec<ByteBuf, MeteorStreakPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                ByteBufCodecs.DOUBLE.encode(buf, packet.x());
                ByteBufCodecs.DOUBLE.encode(buf, packet.y());
                ByteBufCodecs.DOUBLE.encode(buf, packet.z());
                ByteBufCodecs.FLOAT.encode(buf, packet.vx());
                ByteBufCodecs.FLOAT.encode(buf, packet.vy());
                ByteBufCodecs.FLOAT.encode(buf, packet.vz());
                ByteBufCodecs.VAR_INT.encode(buf, packet.life());
                ByteBufCodecs.BOOL.encode(buf, packet.big());
            },
            buf -> new MeteorStreakPacket(
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
