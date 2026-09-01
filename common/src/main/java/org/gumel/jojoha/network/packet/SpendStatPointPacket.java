package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/**
 * Client -> server. Asks to put one earned point into one stat.
 *
 * <p>Carries only which stat, never how many or what the total should become. The server owns the
 * pool and the stat, so a client that sends this twice as fast as it should spends two points and
 * no more - where a packet saying "set strength to 12" would let it say 200 instead.
 *
 * @param stand  whether the points go to the Stand's five rather than the player's
 * @param stat   index into the five the interface lists, in the order it lists them
 * @param amount how many to spend, which the server treats as a ceiling rather than a promise
 */
public record SpendStatPointPacket(boolean stand, int stat, int amount)
        implements CustomPacketPayload {
    public static final Type<SpendStatPointPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "spend_stat_point"));

    public static final StreamCodec<ByteBuf, SpendStatPointPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, SpendStatPointPacket::stand,
                    ByteBufCodecs.VAR_INT, SpendStatPointPacket::stat,
                    ByteBufCodecs.VAR_INT, SpendStatPointPacket::amount,
                    SpendStatPointPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
