package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record ZoomPunchPacket(int playerId, double x, double y, double z, int ticks)
        implements CustomPacketPayload {

    public static final Type<ZoomPunchPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "zoom_punch"));

    public static final StreamCodec<ByteBuf, ZoomPunchPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ZoomPunchPacket::playerId,
            ByteBufCodecs.DOUBLE, ZoomPunchPacket::x,
            ByteBufCodecs.DOUBLE, ZoomPunchPacket::y,
            ByteBufCodecs.DOUBLE, ZoomPunchPacket::z,
            ByteBufCodecs.VAR_INT, ZoomPunchPacket::ticks,
            ZoomPunchPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
