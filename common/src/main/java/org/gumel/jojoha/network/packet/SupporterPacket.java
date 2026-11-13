package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.UUID;

public record SupporterPacket(UUID player, int aura, boolean skins) implements CustomPacketPayload {
    public static final Type<SupporterPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "supporter"));

    public static final StreamCodec<ByteBuf, SupporterPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SupporterPacket::player,
            ByteBufCodecs.INT, SupporterPacket::aura,
            ByteBufCodecs.BOOL, SupporterPacket::skins,
            SupporterPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
