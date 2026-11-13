package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record UseStandSkillPacket(int slot, int chargeTicks) implements CustomPacketPayload {
    public static final Type<UseStandSkillPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "use_stand_skill"));

    public static final StreamCodec<ByteBuf, UseStandSkillPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UseStandSkillPacket::slot,
            ByteBufCodecs.VAR_INT, UseStandSkillPacket::chargeTicks,
            UseStandSkillPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
