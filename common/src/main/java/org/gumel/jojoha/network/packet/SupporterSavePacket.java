package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record SupporterSavePacket(int aura) implements CustomPacketPayload {
    public static final Type<SupporterSavePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "supporter_save"));

    public static final StreamCodec<ByteBuf, SupporterSavePacket> STREAM_CODEC =
            ByteBufCodecs.INT.map(SupporterSavePacket::new, SupporterSavePacket::aura);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
