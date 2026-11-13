package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record ClackerSwingPacket(int playerId, int kind, int targetId) implements CustomPacketPayload {
    public static final int THROW = 0;
    public static final int SPIN = 1;
    public static final int VOLLEY = 2;

    public static final Type<ClackerSwingPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "clacker_swing"));

    public static final StreamCodec<ByteBuf, ClackerSwingPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ClackerSwingPacket::playerId,
            ByteBufCodecs.VAR_INT, ClackerSwingPacket::kind,
            ByteBufCodecs.VAR_INT, ClackerSwingPacket::targetId,
            ClackerSwingPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
