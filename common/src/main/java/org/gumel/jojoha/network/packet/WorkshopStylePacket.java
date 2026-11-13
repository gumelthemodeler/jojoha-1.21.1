package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record WorkshopStylePacket(int summon, int shout, int rest) implements CustomPacketPayload {
    public static final Type<WorkshopStylePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "workshop_style"));

    public static final StreamCodec<ByteBuf, WorkshopStylePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WorkshopStylePacket::summon,
            ByteBufCodecs.VAR_INT, WorkshopStylePacket::shout,
            ByteBufCodecs.VAR_INT, WorkshopStylePacket::rest,
            WorkshopStylePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
