package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/**
 * Client -> server, once per tick while piloting: where the pilot has flown their Stand to.
 *
 * <p>Position rather than intent, which is a deliberate reversal. Sending intent and simulating the
 * result on the server meant two simulations of the same flight - and two simulations of anything
 * disagree. The client had to either accept the server's answer, which drags the Stand back through
 * however long the round trip took, or ignore it, which lets the two drift apart forever. Both were
 * tried. The first is the rubber-banding; the second is a Stand in two places at once.
 *
 * <p>So there is only one simulation now, and it runs on the machine holding the keyboard. This is
 * exactly how vanilla treats the player's own movement, for exactly the same reason: a first-person
 * view has to answer the key on the frame it is pressed, and nothing that arrives over a network
 * can do that.
 *
 * <p>The server still decides what it will accept - see {@code PilotSystem.applyClientPose}, which
 * clamps the position to the leash before using it. A client can say where its Stand went; it
 * cannot say that it went somewhere it is not allowed to be.
 */
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
