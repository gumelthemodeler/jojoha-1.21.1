package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record FlairCuePacket(int entityId, int kind, int amount) implements CustomPacketPayload {
    public static final int DRIP = 0;
    public static final int SUMMON = 1;
    public static final int UNSUMMON = 2;
    public static final int RUPTURE = 3;
    public static final int SPURT = 4;
    public static final int BANKED = 5;
    public static final int TIME_CHARGE = 6;
    public static final int NOISE = 7;
    public static final int ITCH = 8;
    public static final int SEVER = 9;
    public static final int REGROW = 10;

    public static final Type<FlairCuePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "flair_cue"));

    public static final StreamCodec<ByteBuf, FlairCuePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FlairCuePacket::entityId,
            ByteBufCodecs.VAR_INT, FlairCuePacket::kind,
            ByteBufCodecs.VAR_INT, FlairCuePacket::amount,
            FlairCuePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
