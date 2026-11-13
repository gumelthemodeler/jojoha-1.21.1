package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record ClackerThrowPacket() implements CustomPacketPayload {
    public static final Type<ClackerThrowPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "clacker_throw"));

    public static final StreamCodec<ByteBuf, ClackerThrowPacket> STREAM_CODEC =
            StreamCodec.unit(new ClackerThrowPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
