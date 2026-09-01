package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/**
 * Client -> server: take this node on the skill tree.
 *
 * <p>Carries the node and nothing else. Everything about whether it is allowed - the parent being
 * done, the stats being high enough, the items being in the bag - is worked out again on the server
 * from the player it belongs to. The client asks; it does not tell.
 */
public record UnlockNodePacket(String nodeId) implements CustomPacketPayload {

    public static final Type<UnlockNodePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "unlock_node"));

    public static final StreamCodec<ByteBuf, UnlockNodePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, UnlockNodePacket::nodeId,
            UnlockNodePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
