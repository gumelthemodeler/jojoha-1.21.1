package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record DowsingPacket(int playerId, BlockPos target, int ticks) implements CustomPacketPayload {

    public static final Type<DowsingPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "dowsing"));

    public static final StreamCodec<ByteBuf, DowsingPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DowsingPacket::playerId,
            BlockPos.STREAM_CODEC, DowsingPacket::target,
            ByteBufCodecs.VAR_INT, DowsingPacket::ticks,
            DowsingPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
