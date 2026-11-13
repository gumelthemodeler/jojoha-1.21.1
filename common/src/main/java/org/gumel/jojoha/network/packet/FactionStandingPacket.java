package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record FactionStandingPacket(String faction, int total, int gain, boolean quest) implements CustomPacketPayload {
    public static final Type<FactionStandingPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "faction_standing"));

    public static final StreamCodec<ByteBuf, FactionStandingPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FactionStandingPacket::faction,
            ByteBufCodecs.VAR_INT, FactionStandingPacket::total,
            ByteBufCodecs.VAR_INT, FactionStandingPacket::gain,
            ByteBufCodecs.BOOL, FactionStandingPacket::quest,
            FactionStandingPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
