package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record EquipSkillPacket(String skillId, int slot)
        implements CustomPacketPayload {
    public static final String CLEAR = "";

    public static final int FIRST_FREE = -1;

    public static final int UNEQUIP = -2;

    public static final Type<EquipSkillPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "equip_skill"));

    public static final StreamCodec<ByteBuf, EquipSkillPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, EquipSkillPacket::skillId,
                    ByteBufCodecs.VAR_INT, EquipSkillPacket::slot,
                    EquipSkillPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
