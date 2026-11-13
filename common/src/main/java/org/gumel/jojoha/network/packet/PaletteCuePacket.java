package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record PaletteCuePacket(int palette, int ticks) implements CustomPacketPayload {
    public static final Type<PaletteCuePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "palette_cue"));

    public static final StreamCodec<ByteBuf, PaletteCuePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PaletteCuePacket::palette,
            ByteBufCodecs.VAR_INT, PaletteCuePacket::ticks,
            PaletteCuePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
