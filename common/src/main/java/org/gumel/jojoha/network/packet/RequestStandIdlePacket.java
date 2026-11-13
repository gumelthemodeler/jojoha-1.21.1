package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record RequestStandIdlePacket(int stamp) implements CustomPacketPayload {
    public static final Type<RequestStandIdlePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "request_stand_idle"));

    public static final StreamCodec<ByteBuf, RequestStandIdlePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, RequestStandIdlePacket::stamp,
            RequestStandIdlePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
