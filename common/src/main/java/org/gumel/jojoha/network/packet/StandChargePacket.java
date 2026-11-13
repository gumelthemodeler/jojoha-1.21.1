package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record StandChargePacket(boolean charging) implements CustomPacketPayload {
    public static final Type<StandChargePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_charge"));

    public static final StreamCodec<ByteBuf, StandChargePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StandChargePacket::charging,
            StandChargePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
