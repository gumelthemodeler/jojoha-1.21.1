package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record MeteorImpactPacket(double x, double y, double z, float scale)
        implements CustomPacketPayload {

    public static final Type<MeteorImpactPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "meteor_impact"));

    public static final StreamCodec<ByteBuf, MeteorImpactPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, MeteorImpactPacket::x,
            ByteBufCodecs.DOUBLE, MeteorImpactPacket::y,
            ByteBufCodecs.DOUBLE, MeteorImpactPacket::z,
            ByteBufCodecs.FLOAT, MeteorImpactPacket::scale,
            MeteorImpactPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
