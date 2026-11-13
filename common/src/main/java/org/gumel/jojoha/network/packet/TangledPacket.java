package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record TangledPacket(int entityId, int ticks) implements CustomPacketPayload {
    public static final Type<TangledPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tangled"));

    public static final StreamCodec<ByteBuf, TangledPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TangledPacket::entityId,
            ByteBufCodecs.VAR_INT, TangledPacket::ticks,
            TangledPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
