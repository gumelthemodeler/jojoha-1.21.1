package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record PillarStatePacket(int entityId, int toggles, int stone) implements CustomPacketPayload {
    public static final Type<PillarStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "pillar_state"));

    public static final StreamCodec<ByteBuf, PillarStatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PillarStatePacket::entityId,
            ByteBufCodecs.VAR_INT, PillarStatePacket::toggles,
            ByteBufCodecs.VAR_INT, PillarStatePacket::stone,
            PillarStatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
