package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record CycleStandModePacket() implements CustomPacketPayload {
    public static final Type<CycleStandModePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "cycle_stand_mode"));

    public static final StreamCodec<ByteBuf, CycleStandModePacket> STREAM_CODEC =
            StreamCodec.unit(new CycleStandModePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
