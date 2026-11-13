package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record StandPlacementPacket(float side, float height, float ahead, float turn) implements CustomPacketPayload {
    public static final Type<StandPlacementPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_placement"));

    public static final StreamCodec<ByteBuf, StandPlacementPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, StandPlacementPacket::side,
            ByteBufCodecs.FLOAT, StandPlacementPacket::height,
            ByteBufCodecs.FLOAT, StandPlacementPacket::ahead,
            ByteBufCodecs.FLOAT, StandPlacementPacket::turn,
            StandPlacementPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
