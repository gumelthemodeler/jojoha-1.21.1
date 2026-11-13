package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.UUID;

public record BodyStatePacket(UUID player, int state)
        implements CustomPacketPayload {

    public static final Type<BodyStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "body_state"));

    public static final StreamCodec<ByteBuf, BodyStatePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8
                            .map(UUID::fromString, UUID::toString),
                    BodyStatePacket::player,
                    ByteBufCodecs.VAR_INT, BodyStatePacket::state,
                    BodyStatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
