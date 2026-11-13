package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record VampireMarkPacket(int entityId, Mark mark, int ticks) implements CustomPacketPayload {
    public enum Mark {
        BUD,
        DRAINED;

        private static final Mark[] VALUES = values();

        static Mark byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : BUD;
        }
    }

    public static final Type<VampireMarkPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "vampire_mark"));

    public static final StreamCodec<ByteBuf, VampireMarkPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, VampireMarkPacket::entityId,
                    ByteBufCodecs.VAR_INT, packet -> packet.mark().ordinal(),
                    ByteBufCodecs.VAR_INT, VampireMarkPacket::ticks,
                    (id, ordinal, ticks) -> new VampireMarkPacket(id, Mark.byOrdinal(ordinal), ticks));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
