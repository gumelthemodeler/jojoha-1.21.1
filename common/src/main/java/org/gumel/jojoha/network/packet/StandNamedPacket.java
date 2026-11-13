package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.UUID;

public record StandNamedPacket(UUID playerId, ResourceLocation standId) implements CustomPacketPayload {
    public static final Type<StandNamedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_named"));

    public static final StreamCodec<ByteBuf, StandNamedPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, StandNamedPacket::playerId,
            ResourceLocation.STREAM_CODEC, StandNamedPacket::standId,
            StandNamedPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
