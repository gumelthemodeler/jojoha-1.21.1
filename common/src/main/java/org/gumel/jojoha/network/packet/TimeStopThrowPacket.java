package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record TimeStopThrowPacket() implements CustomPacketPayload {
    public static final Type<TimeStopThrowPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "time_stop_throw"));

    public static final StreamCodec<ByteBuf, TimeStopThrowPacket> STREAM_CODEC =
            StreamCodec.unit(new TimeStopThrowPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
