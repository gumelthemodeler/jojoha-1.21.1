package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record ChooseDashPacket(boolean timeSkip) implements CustomPacketPayload {
    public static final Type<ChooseDashPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "choose_dash"));

    public static final StreamCodec<ByteBuf, ChooseDashPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ChooseDashPacket::timeSkip,
            ChooseDashPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
