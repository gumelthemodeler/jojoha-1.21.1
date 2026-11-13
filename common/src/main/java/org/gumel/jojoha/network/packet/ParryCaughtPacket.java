package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record ParryCaughtPacket(int entityId, boolean perfect, boolean weapon, float side)
        implements CustomPacketPayload {
    public static final Type<ParryCaughtPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "parry_caught"));

    public static final StreamCodec<ByteBuf, ParryCaughtPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ParryCaughtPacket::entityId,
            ByteBufCodecs.BOOL, ParryCaughtPacket::perfect,
            ByteBufCodecs.BOOL, ParryCaughtPacket::weapon,
            ByteBufCodecs.FLOAT, ParryCaughtPacket::side,
            ParryCaughtPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
