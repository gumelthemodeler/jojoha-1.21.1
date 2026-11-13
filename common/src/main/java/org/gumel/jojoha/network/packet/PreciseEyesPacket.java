package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record PreciseEyesPacket(boolean on) implements CustomPacketPayload {

    public static final Type<PreciseEyesPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "precise_eyes"));

    public static final StreamCodec<ByteBuf, PreciseEyesPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PreciseEyesPacket::on,
            PreciseEyesPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
