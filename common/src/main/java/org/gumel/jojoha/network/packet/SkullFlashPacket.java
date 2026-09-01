package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/**
 * Server -> client: show the skull inside this victim's head, or break it.
 *
 * <p>The skull is not a thing in the world - it is a frame of animation on the victim - so it is
 * told to the clients that can see them rather than spawned as an entity that would then have to be
 * networked, ticked and cleaned up.
 *
 * <p>Two phases, sent as two packets: the grab announces {@link #WINDUP} and the punch announces
 * {@link #SHATTER}. The client is told how long each lasts rather than working it out, so the
 * animation and the move cannot drift apart if either is retuned.
 *
 * @param victimId   whose head, by network id
 * @param attackerId who threw the punch - only their camera shakes
 * @param phase      {@link #WINDUP} or {@link #SHATTER}
 * @param ticks      how long this phase runs
 */
public record SkullFlashPacket(int victimId, int attackerId, int phase, int ticks)
        implements CustomPacketPayload {

    /** The grab: the skull glows into view inside a head that is still there. */
    public static final int WINDUP = 0;

    /** The punch: it comes apart. */
    public static final int SHATTER = 1;

    public static final Type<SkullFlashPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "skull_flash"));

    // Four ints in a row, so the order here has to match the record exactly - nothing would complain
    // at compile time if it did not. See StandSession for what that costs when it goes wrong.
    public static final StreamCodec<ByteBuf, SkullFlashPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SkullFlashPacket::victimId,
            ByteBufCodecs.VAR_INT, SkullFlashPacket::attackerId,
            ByteBufCodecs.VAR_INT, SkullFlashPacket::phase,
            ByteBufCodecs.VAR_INT, SkullFlashPacket::ticks,
            SkullFlashPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
