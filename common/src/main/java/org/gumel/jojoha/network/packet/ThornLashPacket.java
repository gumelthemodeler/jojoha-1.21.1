package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record ThornLashPacket(int fromId, int toId, int ticks, int coilAfter) implements CustomPacketPayload {
    public static final Type<ThornLashPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "thorn_lash"));

    public static final StreamCodec<ByteBuf, ThornLashPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ThornLashPacket::fromId,
            ByteBufCodecs.VAR_INT, ThornLashPacket::toId,
            ByteBufCodecs.VAR_INT, ThornLashPacket::ticks,
            ByteBufCodecs.VAR_INT, ThornLashPacket::coilAfter,
            ThornLashPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
