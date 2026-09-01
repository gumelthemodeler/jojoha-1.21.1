package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/** Client -> server. Sent when the player presses the Summon/Dismiss Stand keybind. */
public record ToggleStandSummonPacket() implements CustomPacketPayload {
    public static final Type<ToggleStandSummonPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "toggle_stand_summon"));

    public static final StreamCodec<ByteBuf, ToggleStandSummonPacket> STREAM_CODEC =
            StreamCodec.unit(new ToggleStandSummonPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
