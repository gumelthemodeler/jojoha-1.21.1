package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/**
 * Server -> client. A time stop has started or ended somewhere nearby.
 *
 * <p>Sent to everyone who can see it rather than only to the caster, which is the whole point: a
 * time stop is a thing that happens to a place, and a bystander standing inside one should see the
 * world drain and the sphere close over them exactly as its owner does. Every visual before this was
 * driven off the local player's own synced data, so anybody else's stop was invisible to them.
 *
 * <p>Carries the centre rather than letting the client work it out from the caster. Two reasons: the
 * effect stays where it was cast even if the caster walks out of it - it is an area, not an aura -
 * and a bystander has no way to know which player cast it in the first place.
 *
 * <p>The remaining duration is sent once and counted down client-side. Sending it every tick would
 * be a packet per player per tick for ten seconds to keep a number the client can perfectly well
 * work out on its own.
 */
public record TimeStopStatePacket(boolean active, double x, double y, double z, int remainingTicks)
        implements CustomPacketPayload {
    public static final Type<TimeStopStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "time_stop_state"));

    public static final StreamCodec<ByteBuf, TimeStopStatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, TimeStopStatePacket::active,
            ByteBufCodecs.DOUBLE, TimeStopStatePacket::x,
            ByteBufCodecs.DOUBLE, TimeStopStatePacket::y,
            ByteBufCodecs.DOUBLE, TimeStopStatePacket::z,
            ByteBufCodecs.VAR_INT, TimeStopStatePacket::remainingTicks,
            TimeStopStatePacket::new);

    /** The end of a stop, which needs no position. */
    public static TimeStopStatePacket ended() {
        return new TimeStopStatePacket(false, 0, 0, 0, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
