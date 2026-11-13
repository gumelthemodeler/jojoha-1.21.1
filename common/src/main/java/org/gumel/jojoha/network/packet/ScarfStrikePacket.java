package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record ScarfStrikePacket(int playerId, int targetId) implements CustomPacketPayload {
    public static final Type<ScarfStrikePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "scarf_strike"));

    public static final StreamCodec<ByteBuf, ScarfStrikePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ScarfStrikePacket::playerId,
            ByteBufCodecs.VAR_INT, ScarfStrikePacket::targetId,
            ScarfStrikePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
