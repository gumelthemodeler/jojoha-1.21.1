package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.List;

public record QuestLogPacket(List<String> ids, List<Integer> done, List<Integer> needed, List<Long> marks, List<String> offering) implements CustomPacketPayload {
    public static final Type<QuestLogPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "quest_log"));

    public static final StreamCodec<ByteBuf, QuestLogPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), QuestLogPacket::ids,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), QuestLogPacket::done,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), QuestLogPacket::needed,
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), QuestLogPacket::marks,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), QuestLogPacket::offering,
            QuestLogPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
