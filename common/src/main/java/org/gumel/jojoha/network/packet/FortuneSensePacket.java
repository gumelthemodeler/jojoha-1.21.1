package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.List;

public record FortuneSensePacket(int kind, int ticks, List<Integer> entityIds) implements CustomPacketPayload {
    public static final int DIM = 0;

    public static final int GLOW = 1;

    public static final Type<FortuneSensePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "fortune_sense"));

    public static final StreamCodec<ByteBuf, FortuneSensePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FortuneSensePacket::kind,
            ByteBufCodecs.VAR_INT, FortuneSensePacket::ticks,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), FortuneSensePacket::entityIds,
            FortuneSensePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
