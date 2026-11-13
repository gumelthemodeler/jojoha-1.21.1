package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.List;

public record SpaceRipperPacket(int playerId, double x, double y, double z,
                                List<Float> offsets, int eye, int fireTicks, int ticks) implements CustomPacketPayload {

    public static final Type<SpaceRipperPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "space_ripper"));

    private static final int MAX_FLOATS = 3 * 128;

    private static final StreamCodec<ByteBuf, List<Float>> OFFSETS =
            ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list(MAX_FLOATS));

    public static final StreamCodec<ByteBuf, SpaceRipperPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                ByteBufCodecs.VAR_INT.encode(buf, packet.playerId());
                ByteBufCodecs.DOUBLE.encode(buf, packet.x());
                ByteBufCodecs.DOUBLE.encode(buf, packet.y());
                ByteBufCodecs.DOUBLE.encode(buf, packet.z());
                OFFSETS.encode(buf, packet.offsets());
                ByteBufCodecs.VAR_INT.encode(buf, packet.eye());
                ByteBufCodecs.VAR_INT.encode(buf, packet.fireTicks());
                ByteBufCodecs.VAR_INT.encode(buf, packet.ticks());
            },
            buf -> new SpaceRipperPacket(ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf), ByteBufCodecs.DOUBLE.decode(buf), OFFSETS.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
