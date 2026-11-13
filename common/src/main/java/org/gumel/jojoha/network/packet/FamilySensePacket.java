package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record FamilySensePacket(int distance) implements CustomPacketPayload {
    public static final Type<FamilySensePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "family_sense"));

    public static final StreamCodec<ByteBuf, FamilySensePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FamilySensePacket::distance,
            FamilySensePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
