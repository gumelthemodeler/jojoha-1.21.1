package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record FirstPersonPosePacket(String pose, int ticks) implements CustomPacketPayload {
    public static final int RELEASE = -1;

    public static final Type<FirstPersonPosePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "first_person_pose"));

    public static final StreamCodec<ByteBuf, FirstPersonPosePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, FirstPersonPosePacket::pose,
            ByteBufCodecs.VAR_INT, FirstPersonPosePacket::ticks,
            FirstPersonPosePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
