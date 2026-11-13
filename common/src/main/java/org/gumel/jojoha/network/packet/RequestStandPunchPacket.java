package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record RequestStandPunchPacket(int aimedId) implements CustomPacketPayload {
    public static final Type<RequestStandPunchPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "request_stand_punch"));

    public static final StreamCodec<ByteBuf, RequestStandPunchPacket> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(RequestStandPunchPacket::new, RequestStandPunchPacket::aimedId).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
