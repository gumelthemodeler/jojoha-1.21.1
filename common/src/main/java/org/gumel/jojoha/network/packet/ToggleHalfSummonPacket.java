package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record ToggleHalfSummonPacket() implements CustomPacketPayload {
    public static final Type<ToggleHalfSummonPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "toggle_half_summon"));

    public static final StreamCodec<ByteBuf, ToggleHalfSummonPacket> STREAM_CODEC =
            StreamCodec.unit(new ToggleHalfSummonPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
