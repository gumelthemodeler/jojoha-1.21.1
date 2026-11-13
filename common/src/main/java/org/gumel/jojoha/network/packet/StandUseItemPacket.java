package org.gumel.jojoha.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.Optional;

public record StandUseItemPacket(Optional<BlockPos> stretchAnchor, int turns)
        implements CustomPacketPayload {
    public static final Type<StandUseItemPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_use_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandUseItemPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.optional(BlockPos.STREAM_CODEC), StandUseItemPacket::stretchAnchor,
                    ByteBufCodecs.VAR_INT, StandUseItemPacket::turns,
                    StandUseItemPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
