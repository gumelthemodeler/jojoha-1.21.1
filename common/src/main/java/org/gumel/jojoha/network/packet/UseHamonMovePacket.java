package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/** Client -> server. Sent when the player presses a Hamon move keybind; server validates and applies. */
public record UseHamonMovePacket(ResourceLocation moveId) implements CustomPacketPayload {
    public static final Type<UseHamonMovePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "use_hamon_move"));

    public static final StreamCodec<ByteBuf, UseHamonMovePacket> STREAM_CODEC =
            ResourceLocation.STREAM_CODEC.map(UseHamonMovePacket::new, UseHamonMovePacket::moveId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
