package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.List;

public record TendrilPacket(int key, double x, double y, double z, List<Float> offsets, float width, int ticks,
                            int tint) implements CustomPacketPayload {

    public static final Type<TendrilPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tendril"));

    private static final int MAX_FLOATS = 3 * 96;

    private static final StreamCodec<ByteBuf, List<Float>> OFFSETS =
            ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list(MAX_FLOATS));

    public static final StreamCodec<ByteBuf, TendrilPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                ByteBufCodecs.VAR_INT.encode(buf, packet.key());
                ByteBufCodecs.DOUBLE.encode(buf, packet.x());
                ByteBufCodecs.DOUBLE.encode(buf, packet.y());
                ByteBufCodecs.DOUBLE.encode(buf, packet.z());
                OFFSETS.encode(buf, packet.offsets());
                ByteBufCodecs.FLOAT.encode(buf, packet.width());
                ByteBufCodecs.VAR_INT.encode(buf, packet.ticks());
                ByteBufCodecs.VAR_INT.encode(buf, packet.tint());
            },
            buf -> new TendrilPacket(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf), ByteBufCodecs.DOUBLE.decode(buf), OFFSETS.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
