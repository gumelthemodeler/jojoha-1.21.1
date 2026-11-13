package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.WorkshopData;

public record StandIdlePacket(String key, String clip) implements CustomPacketPayload {
    public static final int MAX_CHARS = 30000;

    public static final Type<StandIdlePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_idle"));

    public static final StreamCodec<ByteBuf, StandIdlePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(WorkshopData.IDLE_KEY_CHARS), StandIdlePacket::key,
            ByteBufCodecs.stringUtf8(MAX_CHARS), StandIdlePacket::clip,
            StandIdlePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
