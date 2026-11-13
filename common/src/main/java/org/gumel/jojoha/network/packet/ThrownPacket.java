package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record ThrownPacket(int entityId, float heading, float spin) implements CustomPacketPayload {
    public static final Type<ThrownPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "thrown"));

    public static final StreamCodec<ByteBuf, ThrownPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ThrownPacket::entityId,
            ByteBufCodecs.FLOAT, ThrownPacket::heading,
            ByteBufCodecs.FLOAT, ThrownPacket::spin,
            ThrownPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
