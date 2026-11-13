package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.UUID;

public record SubzeroPacket(UUID playerId, int ticks) implements CustomPacketPayload {

    public static final Type<SubzeroPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "subzero"));

    public static final StreamCodec<ByteBuf, SubzeroPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SubzeroPacket::playerId,
            ByteBufCodecs.VAR_INT, SubzeroPacket::ticks,
            SubzeroPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
