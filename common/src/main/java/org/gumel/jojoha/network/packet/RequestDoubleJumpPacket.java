package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record RequestDoubleJumpPacket(double x, double z) implements CustomPacketPayload {
    public static final Type<RequestDoubleJumpPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "request_double_jump"));

    public static final StreamCodec<ByteBuf, RequestDoubleJumpPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, RequestDoubleJumpPacket::x,
            ByteBufCodecs.DOUBLE, RequestDoubleJumpPacket::z,
            RequestDoubleJumpPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
