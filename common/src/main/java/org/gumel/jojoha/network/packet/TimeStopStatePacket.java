package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.List;

public record TimeStopStatePacket(boolean active, double x, double y, double z, int remainingTicks,
                                  List<Integer> frozenPlayers, double radius, int quietTicks,
                                  int introTicks)
        implements CustomPacketPayload {
    public static final Type<TimeStopStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "time_stop_state"));

    private static final StreamCodec<ByteBuf, List<Integer>> PLAYERS = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list());

    public static final StreamCodec<ByteBuf, TimeStopStatePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                ByteBufCodecs.BOOL.encode(buf, packet.active());
                ByteBufCodecs.DOUBLE.encode(buf, packet.x());
                ByteBufCodecs.DOUBLE.encode(buf, packet.y());
                ByteBufCodecs.DOUBLE.encode(buf, packet.z());
                ByteBufCodecs.VAR_INT.encode(buf, packet.remainingTicks());
                PLAYERS.encode(buf, packet.frozenPlayers());
                ByteBufCodecs.DOUBLE.encode(buf, packet.radius());
                ByteBufCodecs.VAR_INT.encode(buf, packet.quietTicks());
                ByteBufCodecs.VAR_INT.encode(buf, packet.introTicks());
            },
            buf -> new TimeStopStatePacket(ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf), ByteBufCodecs.DOUBLE.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    PLAYERS.decode(buf), ByteBufCodecs.DOUBLE.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)));

    public static TimeStopStatePacket ended() {
        return new TimeStopStatePacket(false, 0, 0, 0, 0, List.of(), 0.0, 0, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
