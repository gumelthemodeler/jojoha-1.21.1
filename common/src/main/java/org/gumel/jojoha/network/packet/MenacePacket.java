package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record MenacePacket(int entityId, int kind) implements CustomPacketPayload {
    public static final Type<MenacePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "menace"));

    public static final StreamCodec<ByteBuf, MenacePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MenacePacket::entityId,
            ByteBufCodecs.VAR_INT, MenacePacket::kind,
            MenacePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
