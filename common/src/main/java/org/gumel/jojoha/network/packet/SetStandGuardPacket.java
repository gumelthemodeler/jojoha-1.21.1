package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/** Client -> server. Sent whenever the guard key is pressed or released while a Stand is summoned. */
public record SetStandGuardPacket(boolean guarding) implements CustomPacketPayload {
    public static final Type<SetStandGuardPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "set_stand_guard"));

    public static final StreamCodec<ByteBuf, SetStandGuardPacket> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(SetStandGuardPacket::new, SetStandGuardPacket::guarding);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
