package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record PilotPosePacket(double x, double y, double z, float yRot, float xRot)
        implements CustomPacketPayload {

    public static final Type<PilotPosePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "pilot_pose"));

    public static final StreamCodec<ByteBuf, PilotPosePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, PilotPosePacket::x,
            ByteBufCodecs.DOUBLE, PilotPosePacket::y,
            ByteBufCodecs.DOUBLE, PilotPosePacket::z,
            ByteBufCodecs.FLOAT, PilotPosePacket::yRot,
            ByteBufCodecs.FLOAT, PilotPosePacket::xRot,
            PilotPosePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
