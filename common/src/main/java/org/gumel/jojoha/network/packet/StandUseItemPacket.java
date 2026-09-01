package org.gumel.jojoha.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.Optional;

/**
 * Client -> server. "My Stand should be the one doing this."
 *
 * <p>Carries one thing, and only because the server cannot know it: where the player began
 * stretching a run. Everything else - which hand, which item, what is under the crosshair, whether
 * any of it is permitted - the server re-derives for itself, so a tampered client can at worst ask
 * for a use it was entitled to anyway.
 *
 * <p>The anchor is a selection rather than a permission, which is why sending it is safe. The server
 * still decides what the run means: it clamps the length, re-traces the far end from the player's
 * own eyes, and refuses anything out of the Stand's range. A client that lies about the anchor gets
 * a shorter run, not a longer one.
 */
public record StandUseItemPacket(Optional<BlockPos> stretchAnchor) implements CustomPacketPayload {
    public static final Type<StandUseItemPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_use_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandUseItemPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.optional(BlockPos.STREAM_CODEC), StandUseItemPacket::stretchAnchor,
                    StandUseItemPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
