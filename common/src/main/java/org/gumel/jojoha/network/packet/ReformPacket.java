package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record ReformPacket(int entityId, int left, int total) implements CustomPacketPayload {
    public static final Type<ReformPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "reform"));

    public static final StreamCodec<ByteBuf, ReformPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ReformPacket::entityId,
            ByteBufCodecs.VAR_INT, ReformPacket::left,
            ByteBufCodecs.VAR_INT, ReformPacket::total,
            ReformPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
