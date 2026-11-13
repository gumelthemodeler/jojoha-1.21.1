package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record WorkshopLookPacket(int slot, boolean save, int skin) implements CustomPacketPayload {
    public static final Type<WorkshopLookPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "workshop_look"));

    public static final StreamCodec<ByteBuf, WorkshopLookPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WorkshopLookPacket::slot,
            ByteBufCodecs.BOOL, WorkshopLookPacket::save,
            ByteBufCodecs.VAR_INT, WorkshopLookPacket::skin,
            WorkshopLookPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
