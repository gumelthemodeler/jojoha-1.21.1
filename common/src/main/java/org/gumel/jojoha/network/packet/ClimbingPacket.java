package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.UUID;

public record ClimbingPacket(UUID player, int grip, boolean climbing)
        implements CustomPacketPayload {

    public static final Type<ClimbingPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "climbing"));

    public static final StreamCodec<ByteBuf, ClimbingPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8
                            .map(UUID::fromString, UUID::toString),
                    ClimbingPacket::player,
                    ByteBufCodecs.VAR_INT, ClimbingPacket::grip,
                    ByteBufCodecs.BOOL, ClimbingPacket::climbing,
                    ClimbingPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
