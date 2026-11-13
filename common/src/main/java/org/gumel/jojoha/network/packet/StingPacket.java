package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record StingPacket(int kind) implements CustomPacketPayload {
    public static final Type<StingPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sting"));

    public static final StreamCodec<ByteBuf, StingPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StingPacket::kind,
            StingPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
