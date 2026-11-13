package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record SkullFlashPacket(int victimId, int attackerId, int phase, int ticks)
        implements CustomPacketPayload {
    public static final int WINDUP = 0;

    public static final int SHATTER = 1;

    public static final Type<SkullFlashPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "skull_flash"));

    public static final StreamCodec<ByteBuf, SkullFlashPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SkullFlashPacket::victimId,
            ByteBufCodecs.VAR_INT, SkullFlashPacket::attackerId,
            ByteBufCodecs.VAR_INT, SkullFlashPacket::phase,
            ByteBufCodecs.VAR_INT, SkullFlashPacket::ticks,
            SkullFlashPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
