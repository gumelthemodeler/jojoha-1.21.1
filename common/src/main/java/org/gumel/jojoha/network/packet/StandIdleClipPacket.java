package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record StandIdleClipPacket(int stamp, String clip) implements CustomPacketPayload {
    public static final Type<StandIdleClipPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_idle_clip"));

    public static final StreamCodec<ByteBuf, StandIdleClipPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, StandIdleClipPacket::stamp,
            ByteBufCodecs.stringUtf8(StandIdlePacket.MAX_CHARS), StandIdleClipPacket::clip,
            StandIdleClipPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
