package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record PlayerMovePosePacket(int entityId, String animation, int mode) implements CustomPacketPayload {
    public static final Type<PlayerMovePosePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "player_move_pose"));

    public static final int ONCE = 0;
    public static final int HELD = 1;

    public static final StreamCodec<ByteBuf, PlayerMovePosePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PlayerMovePosePacket::entityId,
            ByteBufCodecs.STRING_UTF8, PlayerMovePosePacket::animation,
            ByteBufCodecs.VAR_INT, PlayerMovePosePacket::mode,
            PlayerMovePosePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
