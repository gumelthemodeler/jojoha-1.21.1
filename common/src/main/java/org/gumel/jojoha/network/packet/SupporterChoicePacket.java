package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record SupporterChoicePacket(int aura) implements CustomPacketPayload {
    public static final Type<SupporterChoicePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "supporter_choice"));

    public static final StreamCodec<ByteBuf, SupporterChoicePacket> STREAM_CODEC =
            ByteBufCodecs.INT.map(SupporterChoicePacket::new, SupporterChoicePacket::aura);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
