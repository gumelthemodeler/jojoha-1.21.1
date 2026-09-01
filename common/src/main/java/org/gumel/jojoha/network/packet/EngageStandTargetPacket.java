package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/**
 * Client -> server. Sends the Stand after a specific entity the player has lined up.
 *
 * <p>Carries the entity id rather than a position: the server re-resolves and re-validates it
 * (still alive, still in range, actually targetable) rather than trusting the client's pick, so a
 * crafted packet can't order a strike on something across the world.
 */
public record EngageStandTargetPacket(int targetId) implements CustomPacketPayload {
    public static final Type<EngageStandTargetPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "engage_stand_target"));

    public static final StreamCodec<ByteBuf, EngageStandTargetPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EngageStandTargetPacket::targetId,
            EngageStandTargetPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
