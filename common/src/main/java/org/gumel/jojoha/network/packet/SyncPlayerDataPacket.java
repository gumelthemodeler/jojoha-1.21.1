package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;

/** Server -> client. Sent on join and whenever the server mutates a player's data. */
public record SyncPlayerDataPacket(JojohaPlayerData data) implements CustomPacketPayload {
    public static final Type<SyncPlayerDataPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "sync_player_data"));

    public static final StreamCodec<ByteBuf, SyncPlayerDataPacket> STREAM_CODEC =
            ByteBufCodecs.fromCodec(JojohaPlayerData.CODEC).map(SyncPlayerDataPacket::new, SyncPlayerDataPacket::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
