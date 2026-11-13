package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record DreamAnswerPacket(int turn, int answer) implements CustomPacketPayload {
    public static final Type<DreamAnswerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "dream_answer"));

    public static final StreamCodec<ByteBuf, DreamAnswerPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DreamAnswerPacket::turn,
            ByteBufCodecs.VAR_INT, DreamAnswerPacket::answer,
            DreamAnswerPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
