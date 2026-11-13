package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record EquipStandSkinPacket(int skin) implements CustomPacketPayload {
    public static final Type<EquipStandSkinPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "equip_stand_skin"));

    public static final StreamCodec<ByteBuf, EquipStandSkinPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EquipStandSkinPacket::skin,
            EquipStandSkinPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
