package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/**
 * Client -> server. Asks to put a move on a bar slot, or to take one off.
 *
 * <p>Carries what the player pointed at, never what the outcome should be. The server decides which
 * slot a move lands in and whether they may have it there at all - so the worst a client can do by
 * sending this repeatedly is equip a move it already owns, repeatedly.
 *
 * @param skillId the move, or empty to clear the slot
 * @param slot    where to put it, or -1 to mean "the first free one"
 * @param utility which of the two bars is meant - the Utility stance keeps its own
 */
public record EquipSkillPacket(String skillId, int slot, boolean utility)
        implements CustomPacketPayload {
    /** Sent with an empty id to take a move off the bar. */
    public static final String CLEAR = "";

    /** Sent as the slot when the client has no opinion and the server should find room. */
    public static final int FIRST_FREE = -1;

    /** Sent as the slot to take the named move off the bar, wherever it happens to be. */
    public static final int UNEQUIP = -2;

    public static final Type<EquipSkillPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "equip_skill"));

    public static final StreamCodec<ByteBuf, EquipSkillPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, EquipSkillPacket::skillId,
                    ByteBufCodecs.VAR_INT, EquipSkillPacket::slot,
                    ByteBufCodecs.BOOL, EquipSkillPacket::utility,
                    EquipSkillPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
