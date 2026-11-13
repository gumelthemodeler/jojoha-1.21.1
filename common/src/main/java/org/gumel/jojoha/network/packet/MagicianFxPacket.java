package org.gumel.jojoha.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;

public record MagicianFxPacket(int kind, int id, double x, double y, double z, float dx, float dy, float dz,
                               float radius, int ticks) implements CustomPacketPayload {
    public static final int ANKH = 0;
    public static final int ANKH_END = 1;
    public static final int ZONE_RING = 2;
    public static final int ZONE_DOME = 3;
    public static final int ZONE_END = 4;
    public static final int BIND = 5;
    public static final int RADAR = 6;
    public static final int HEAT = 7;
    public static final int INFERNO = 8;
    public static final int KICK = 9;
    public static final int WALL = 10;
    public static final int WALL_END = 11;

    public static final Type<MagicianFxPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "magician_fx"));

    public static final StreamCodec<ByteBuf, MagicianFxPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeInt(packet.kind());
                buf.writeInt(packet.id());
                buf.writeDouble(packet.x());
                buf.writeDouble(packet.y());
                buf.writeDouble(packet.z());
                buf.writeFloat(packet.dx());
                buf.writeFloat(packet.dy());
                buf.writeFloat(packet.dz());
                buf.writeFloat(packet.radius());
                buf.writeInt(packet.ticks());
            },
            buf -> new MagicianFxPacket(
                    buf.readInt(),
                    buf.readInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readInt()));

    public static MagicianFxPacket at(int kind, int id, Vec3 at, float radius, int ticks) {
        return new MagicianFxPacket(kind, id, at.x, at.y, at.z, 0F, 0F, 0F, radius, ticks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
