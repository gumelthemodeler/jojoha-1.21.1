package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record MoveInputPacket(byte forward, byte left) implements CustomPacketPayload {
    public static final Type<MoveInputPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "move_input"));

    public static final StreamCodec<ByteBuf, MoveInputPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, MoveInputPacket::forward,
            ByteBufCodecs.BYTE, MoveInputPacket::left,
            MoveInputPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
