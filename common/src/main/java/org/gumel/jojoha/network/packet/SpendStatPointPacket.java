package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record SpendStatPointPacket(boolean stand, int stat, int amount)
        implements CustomPacketPayload {
    public static final Type<SpendStatPointPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "spend_stat_point"));

    public static final StreamCodec<ByteBuf, SpendStatPointPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, SpendStatPointPacket::stand,
                    ByteBufCodecs.VAR_INT, SpendStatPointPacket::stat,
                    ByteBufCodecs.VAR_INT, SpendStatPointPacket::amount,
                    SpendStatPointPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
