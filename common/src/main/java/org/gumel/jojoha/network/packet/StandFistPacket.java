package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.combat.StandFist;

public record StandFistPacket(int standId, int slot, long spawnTick, double aimX, double aimY, double aimZ,
                              float bendSide, float bendUp) implements CustomPacketPayload {

    public static final Type<StandFistPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_fist"));

    public static final StreamCodec<ByteBuf, StandFistPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                ByteBufCodecs.VAR_INT.encode(buf, packet.standId());
                ByteBufCodecs.VAR_INT.encode(buf, packet.slot());
                buf.writeLong(packet.spawnTick());
                buf.writeDouble(packet.aimX());
                buf.writeDouble(packet.aimY());
                buf.writeDouble(packet.aimZ());
                buf.writeFloat(packet.bendSide());
                buf.writeFloat(packet.bendUp());
            },
            buf -> new StandFistPacket(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    buf.readLong(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat()));

    public static StandFistPacket of(int standId, StandFist fist) {
        return new StandFistPacket(standId, fist.slot(), fist.spawnTick(),
                fist.aim().x, fist.aim().y, fist.aim().z, fist.bendSide(), fist.bendUp());
    }

    public StandFist fist() {
        return new StandFist(slot, spawnTick, new Vec3(aimX, aimY, aimZ), bendSide, bendUp);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
