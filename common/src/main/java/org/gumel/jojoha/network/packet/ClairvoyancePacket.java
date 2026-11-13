package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record ClairvoyancePacket(int ticks) implements CustomPacketPayload {

    public static final Type<ClairvoyancePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "clairvoyance"));

    public static final StreamCodec<ByteBuf, ClairvoyancePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ClairvoyancePacket::ticks,
            ClairvoyancePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
