package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.List;

public record NpcDialoguePacket(int entityId, String text, List<String> replies, int trust, boolean canGift, String trait) implements CustomPacketPayload {
    public static final Type<NpcDialoguePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "npc_dialogue"));

    public static final StreamCodec<ByteBuf, NpcDialoguePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NpcDialoguePacket::entityId,
            ByteBufCodecs.STRING_UTF8, NpcDialoguePacket::text,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), NpcDialoguePacket::replies,
            ByteBufCodecs.VAR_INT, NpcDialoguePacket::trust,
            ByteBufCodecs.BOOL, NpcDialoguePacket::canGift,
            ByteBufCodecs.STRING_UTF8, NpcDialoguePacket::trait,
            NpcDialoguePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
