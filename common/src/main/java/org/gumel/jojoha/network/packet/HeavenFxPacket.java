package org.gumel.jojoha.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public record HeavenFxPacket(int kind, int entity, int other, double x, double y, double z, float size, int ticks)
        implements CustomPacketPayload {
    public static final int ERASURE = 0;
    public static final int FLASH = 1;
    public static final int SUMMON = 2;
    public static final int TIER = 3;
    public static final int HALO = 4;
    public static final int RUSH = 5;
    public static final int RUSH_LEG = 6;
    public static final int ROAR = 7;
    public static final int HIGH_HIT = 8;
    public static final int GOLD_STOP = 9;
    public static final int PULSE = 10;
    public static final int TETHER = 11;
    public static final int BEAM = 12;
    public static final int PILLAR = 13;
    public static final int DOME = 14;
    public static final int PILLARS = 15;
    public static final int LIFT = 16;
    public static final int SLAM = 17;
    public static final int REWIND = 18;
    public static final int RISE = 19;
    public static final int SERVANT = 20;
    public static final int ERASED = 21;
    public static final int HIGH = 22;
    public static final int RUSH_MARK = 23;
    public static final int CHARGE = 24;
    public static final int JUDGE = 25;
    public static final int STEP = 26;
    public static final int UPPER = 27;
    public static final int KNIVES = 28;
    public static final int CRACKS = 29;
    public static final int UNMADE = 30;
    public static final int STAND_TIER = 31;
    public static final int SKY = 32;
    public static final int TELEGRAPH = 33;
    public static final int REWRITE = 34;
    public static final int ERASE_WORLD = 35;
    public static final int REALM = 36;

    public static final Type<HeavenFxPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "heaven_fx"));

    public static final StreamCodec<FriendlyByteBuf, HeavenFxPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.kind);
                buf.writeVarInt(packet.entity);
                buf.writeVarInt(packet.other);
                buf.writeDouble(packet.x);
                buf.writeDouble(packet.y);
                buf.writeDouble(packet.z);
                buf.writeFloat(packet.size);
                buf.writeVarInt(packet.ticks);
            },
            buf -> new HeavenFxPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readVarInt()));

    public static HeavenFxPacket screen(int kind, int ticks) {
        return new HeavenFxPacket(kind, -1, -1, 0.0, 0.0, 0.0, 0F, ticks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
