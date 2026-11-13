package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record PossessionStatePacket(int entityId, boolean asVictim, boolean on, boolean merged) implements CustomPacketPayload {

    public static final Type<PossessionStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "possession_state"));

    public static final StreamCodec<ByteBuf, PossessionStatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PossessionStatePacket::entityId,
            ByteBufCodecs.BOOL, PossessionStatePacket::asVictim,
            ByteBufCodecs.BOOL, PossessionStatePacket::on,
            ByteBufCodecs.BOOL, PossessionStatePacket::merged,
            PossessionStatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
