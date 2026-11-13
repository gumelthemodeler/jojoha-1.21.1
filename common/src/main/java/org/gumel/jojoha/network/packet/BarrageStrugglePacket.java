package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record BarrageStrugglePacket(boolean active, float mine, int myColour, int theirColour, int result,
                                    int beat, int presses, boolean onBeat)
        implements CustomPacketPayload {

    public static final int NONE = 0;
    public static final int WON = 1;
    public static final int LOST = 2;

    public static final Type<BarrageStrugglePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "barrage_struggle"));

    public static final StreamCodec<ByteBuf, BarrageStrugglePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                ByteBufCodecs.BOOL.encode(buf, packet.active());
                ByteBufCodecs.FLOAT.encode(buf, packet.mine());
                ByteBufCodecs.INT.encode(buf, packet.myColour());
                ByteBufCodecs.INT.encode(buf, packet.theirColour());
                ByteBufCodecs.VAR_INT.encode(buf, packet.result());
                ByteBufCodecs.VAR_INT.encode(buf, packet.beat());
                ByteBufCodecs.VAR_INT.encode(buf, packet.presses());
                ByteBufCodecs.BOOL.encode(buf, packet.onBeat());
            },
            buf -> new BarrageStrugglePacket(
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
