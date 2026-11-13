package org.gumel.jojoha.stand.heaven;

import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.network.packet.HeavenFxPacket;

public final class HeavenShow {
    public static final double SEE_RANGE = 128.0;

    private HeavenShow() {
    }

    public static void screen(ServerPlayer player, int kind, int ticks) {
        NetworkManager.sendToPlayer(player, HeavenFxPacket.screen(kind, ticks));
    }

    public static void at(ServerLevel level, int kind, Vec3 at, float size, int ticks) {
        send(level, new HeavenFxPacket(kind, -1, -1, at.x, at.y, at.z, size, ticks), at);
    }

    public static void on(Entity entity, int kind, float size, int ticks) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = entity.position();
        send(level, new HeavenFxPacket(kind, entity.getId(), -1, at.x, at.y, at.z, size, ticks), at);
    }

    public static void on(Entity entity, int kind, Vec3 at, float size, int ticks) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        send(level, new HeavenFxPacket(kind, entity.getId(), -1, at.x, at.y, at.z, size, ticks), at);
    }

    public static void between(Entity from, Entity to, int kind, int ticks) {
        if (!(from.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = to.position();
        send(level, new HeavenFxPacket(kind, from.getId(), to.getId(), at.x, at.y, at.z, 0F, ticks), at);
    }

    public static void full(ServerLevel level, int kind, int entity, int other, Vec3 at, float size, int ticks) {
        send(level, new HeavenFxPacket(kind, entity, other, at.x, at.y, at.z, size, ticks), at);
    }

    public static void everywhere(ServerLevel level, int kind, int entity, float size, int ticks) {
        HeavenFxPacket packet = new HeavenFxPacket(kind, entity, -1, 0.0, 0.0, 0.0, size, ticks);
        for (ServerPlayer player : level.players()) {
            NetworkManager.sendToPlayer(player, packet);
        }
    }

    private static void send(ServerLevel level, HeavenFxPacket packet, Vec3 at) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at) <= SEE_RANGE * SEE_RANGE) {
                NetworkManager.sendToPlayer(player, packet);
            }
        }
    }
}
