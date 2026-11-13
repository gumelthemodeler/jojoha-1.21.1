package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record WingsStatePacket(int entityId, boolean out, boolean flap) implements CustomPacketPayload {
    public static final Type<WingsStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "wings_state"));

    public static final StreamCodec<ByteBuf, WingsStatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WingsStatePacket::entityId,
            ByteBufCodecs.BOOL, WingsStatePacket::out,
            ByteBufCodecs.BOOL, WingsStatePacket::flap,
            WingsStatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
