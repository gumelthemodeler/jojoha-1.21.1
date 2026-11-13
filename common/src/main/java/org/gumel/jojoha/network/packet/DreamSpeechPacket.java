package org.gumel.jojoha.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.List;

public record DreamSpeechPacket(List<Component> lines, int mood, int turn, List<Component> replies,
                                List<Integer> tones, int ending) implements CustomPacketPayload {
    public static final int CALM = 0;
    public static final int COLD = 1;
    public static final int WARM = 2;

    public static final int GOLD = 0;
    public static final int PALE = 1;
    public static final int WHITE = 2;

    public static final int GOING_ON = 0;
    public static final int PARTING = 1;
    public static final int WAKING = 2;

    public static final Type<DreamSpeechPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "dream_speech"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DreamSpeechPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list()),
                    DreamSpeechPacket::lines,
                    ByteBufCodecs.VAR_INT, DreamSpeechPacket::mood,
                    ByteBufCodecs.VAR_INT, DreamSpeechPacket::turn,
                    ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list()),
                    DreamSpeechPacket::replies,
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), DreamSpeechPacket::tones,
                    ByteBufCodecs.VAR_INT, DreamSpeechPacket::ending,
                    DreamSpeechPacket::new);

    public boolean parting() {
        return ending != GOING_ON;
    }

    public boolean waking() {
        return ending == WAKING;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
