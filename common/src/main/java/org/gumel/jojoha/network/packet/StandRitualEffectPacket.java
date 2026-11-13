package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.UUID;

public record StandRitualEffectPacket(UUID playerId, Effect effect) implements CustomPacketPayload {
    public enum Effect {
        STAB,
        AWAKENING_RAYS,
        SKIN_RAYS,
        SKIN_NAMED,
        AWAKENING_REFUSED,
        LAND,
        MASK_EQUIP,
        MASK_SEATED,
        MASK_TURNING,
        MASK_AWAKENING,
        MASK_SPENT,
        MASK_CANCELLED,
        PIERCED,
        SOUL_CALLED,
        AJA_SEATED
    }

    public static final Type<StandRitualEffectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_ritual_effect"));

    private static final Effect[] EFFECTS = Effect.values();

    public static final StreamCodec<ByteBuf, StandRitualEffectPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, StandRitualEffectPacket::playerId,
            ByteBufCodecs.idMapper(id -> EFFECTS[id], Effect::ordinal), StandRitualEffectPacket::effect,
            StandRitualEffectPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
