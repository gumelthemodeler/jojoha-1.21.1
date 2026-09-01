package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/**
 * Client -> server. The player pressed a Stand skill slot.
 *
 * <p>The charge is how many ticks the key was held, for moves that are held rather than tapped. It
 * is clamped server-side against the move's own maximum, so a modified client claiming an hour of
 * charge gets whatever the move actually allows and nothing more.
 *
 * <p>Carries the slot index rather than a move id on purpose. The server already knows which Stand
 * the player has and therefore which move sits in each slot, so sending only the slot means a
 * modified client cannot nominate a move its Stand does not have - the worst it can do is press an
 * empty slot, which resolves to nothing.
 */
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
