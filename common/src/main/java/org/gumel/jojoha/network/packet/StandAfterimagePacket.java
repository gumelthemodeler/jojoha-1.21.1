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
 * Server -> client. Start trailing after-images behind a player for a while.
 *
 * <p>Carries the colour rather than letting each client work it out. A viewer knows nothing about
 * anyone else's Stand - that data is only ever synced to its owner - so the one place that can name
 * the colour is the server.
 */
public record StandAfterimagePacket(UUID playerId, int color, int durationTicks) implements CustomPacketPayload {
    public static final Type<StandAfterimagePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_afterimage"));

    public static final StreamCodec<ByteBuf, StandAfterimagePacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, StandAfterimagePacket::playerId,
            ByteBufCodecs.INT, StandAfterimagePacket::color,
            ByteBufCodecs.VAR_INT, StandAfterimagePacket::durationTicks,
            StandAfterimagePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
