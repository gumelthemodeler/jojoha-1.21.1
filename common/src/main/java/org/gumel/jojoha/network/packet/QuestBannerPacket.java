package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.List;

public record QuestBannerPacket(String quest, boolean complete, List<String> items, List<Integer> counts, int trust,
                                int standing) implements CustomPacketPayload {
    public static final Type<QuestBannerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "quest_banner"));

    public static final StreamCodec<ByteBuf, QuestBannerPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, QuestBannerPacket::quest,
            ByteBufCodecs.BOOL, QuestBannerPacket::complete,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), QuestBannerPacket::items,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), QuestBannerPacket::counts,
            ByteBufCodecs.VAR_INT, QuestBannerPacket::trust,
            ByteBufCodecs.VAR_INT, QuestBannerPacket::standing,
            QuestBannerPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
