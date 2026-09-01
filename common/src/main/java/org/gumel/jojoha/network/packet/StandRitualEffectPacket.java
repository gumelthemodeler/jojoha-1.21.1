package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.UUID;

/**
 * Server -> client. Fires one beat of the Stand Arrow ritual on a given player.
 *
 * <p>Carries the player's UUID rather than relying on the receiving client's own player, because
 * the ritual is a spectacle: everyone in range renders the stab and the awakening rays on whoever
 * used the arrow, not just the user themselves.
 */
public record StandRitualEffectPacket(UUID playerId, Effect effect) implements CustomPacketPayload {
    public enum Effect {
        /** Drive the arrow in. Main hand only - the item refuses off-hand use. */
        STAB,
        /** Begin the Ender-Dragon-style awakening rays. */
        AWAKENING_RAYS,
        /**
         * The same eruption, for an arrow that is changing a Stand's skin rather than granting one.
         *
         * <p>Its own value rather than a flag alongside the other, because every one of these is a
         * whole beat of the sequence and the client switches on them - and because the two say
         * different things out loud. Appended, since the ordinal is what goes over the wire.
         */
        SKIN_RAYS,
        /**
         * The new Stand has arrived and is wearing the new skin; name it.
         *
         * <p>Sent rather than scheduled, and that is the point of it. The name has to be read off
         * data the server only writes at the last moment, so a client counting ticks toward it
         * races the sync that carries the answer - and loses often enough to shout the skin that
         * was just destroyed. Sent immediately after that sync, packet ordering settles it.
         */
        SKIN_NAMED,
        /** Touchdown after the awakening's levitation lets go. */
        LAND,
        /** Raise the Stone Mask and turn it over - the equip_mask animation. */
        MASK_EQUIP,
        /** The mask has reached the face; it is drawn there from now on. */
        MASK_SEATED,
        /** The stone goes over to red. Before the awakening, and quieter than it. */
        MASK_TURNING,
        /** The mask wakes: the awaken animation, and the rays in red. */
        MASK_AWAKENING,
        /** Spent. It comes off the face and is gone. */
        MASK_SPENT,
        /** Taken out of the hand before it reached the face - stop the animation. */
        MASK_CANCELLED
    }

    public static final Type<StandRitualEffectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_ritual_effect"));

    private static final Effect[] EFFECTS = Effect.values();

    public static final StreamCodec<ByteBuf, StandRitualEffectPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, StandRitualEffectPacket::playerId,
            ByteBufCodecs.idMapper(id -> EFFECTS[id], Effect::ordinal), StandRitualEffectPacket::effect,
            StandRitualEffectPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
