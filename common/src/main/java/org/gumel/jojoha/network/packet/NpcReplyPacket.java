package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record NpcReplyPacket(int entityId, String text, String reply) implements CustomPacketPayload {
    public static final Type<NpcReplyPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "npc_reply"));

    public static final StreamCodec<ByteBuf, NpcReplyPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NpcReplyPacket::entityId,
            ByteBufCodecs.STRING_UTF8, NpcReplyPacket::text,
            ByteBufCodecs.STRING_UTF8, NpcReplyPacket::reply,
            NpcReplyPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
