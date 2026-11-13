package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.StandPose;

public record WorkshopPosePacket(int slot, StandPose pose) implements CustomPacketPayload {
    public static final Type<WorkshopPosePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "workshop_pose"));

    public static final StreamCodec<ByteBuf, WorkshopPosePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WorkshopPosePacket::slot,
            ByteBufCodecs.fromCodec(StandPose.CODEC), WorkshopPosePacket::pose,
            WorkshopPosePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
