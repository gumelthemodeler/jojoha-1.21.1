package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record EquipAdaptationPacket(String adaptation, boolean active, int slot) implements CustomPacketPayload {
    public static final Type<EquipAdaptationPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "equip_adaptation"));

    public static final StreamCodec<ByteBuf, EquipAdaptationPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), EquipAdaptationPacket::adaptation,
            ByteBufCodecs.BOOL, EquipAdaptationPacket::active,
            ByteBufCodecs.VAR_INT, EquipAdaptationPacket::slot,
            EquipAdaptationPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
