package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/**
 * One vine, thrown from somebody at somebody, for a moment.
 *
 * <p>Told rather than spawned. The grapple gets an entity because it stays out and has to be
 * simulated; a whip is over inside a tick, and an entity that exists for one tick costs a spawn, a
 * tracker and a despawn to be seen once. Two entity ids and a duration is the whole of what a client
 * needs to draw the same rope.
 *
 * <p>Ids rather than positions, deliberately: both ends move while the lash is on screen - the
 * thrower turns, the victim is dragged - and a pair of coordinates captured on the server tick would
 * leave the rope hanging where the two of them used to be.
 */
public record ThornLashPacket(int fromId, int toId, int ticks) implements CustomPacketPayload {
    public static final Type<ThornLashPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "thorn_lash"));

    // Three ints in a row: the order here has to match the record exactly, and nothing would
    // complain at compile time if it did not.
    public static final StreamCodec<ByteBuf, ThornLashPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ThornLashPacket::fromId,
            ByteBufCodecs.VAR_INT, ThornLashPacket::toId,
            ByteBufCodecs.VAR_INT, ThornLashPacket::ticks,
            ThornLashPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
