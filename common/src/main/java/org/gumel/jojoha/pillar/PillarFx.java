package org.gumel.jojoha.pillar;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModRegistries;

public final class PillarFx {
    public static final int WIND_STYLE = 0;
    public static final int HEAT_STYLE = 1;
    public static final int LIGHT_STYLE = 2;
    public static final int FLESH_STYLE = 3;

    private PillarFx() {
    }

    public static void charge(ServerPlayer player, int style, int ticks) {
        NetworkHandler.sendPillarFx(player.serverLevel(), PillarFxKind.CHARGE, player.getId(),
                style, ticks);
    }

    public static void flare(ServerPlayer player, int style, boolean on) {
        NetworkHandler.sendPillarFx(player.serverLevel(), PillarFxKind.FLARE, player.getId(),
                style, on ? 1 : 0);
    }

    public static ParticleOptions flesh() {
        return ModRegistries.PILLAR_FLESH.get();
    }

    public static ParticleOptions bone() {
        return ModRegistries.PILLAR_BONE.get();
    }

    public static ParticleOptions gust() {
        return ModRegistries.PILLAR_GUST.get();
    }

    public static ParticleOptions sand() {
        return ModRegistries.PILLAR_SAND.get();
    }

    public static ParticleOptions steam() {
        return ModRegistries.PILLAR_STEAM.get();
    }

    public static ParticleOptions bubble() {
        return ModRegistries.PILLAR_BUBBLE.get();
    }

    public static ParticleOptions glint() {
        return ModRegistries.PILLAR_GLINT.get();
    }

    public static void spray(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread,
                             double speed) {
        level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    public static void fling(ServerLevel level, ParticleOptions particle, Vec3 at, Vec3 velocity) {
        level.sendParticles(particle, at.x, at.y, at.z, 0, velocity.x, velocity.y, velocity.z, 1.0);
    }

    public static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, double every) {
        Vec3 span = to.subtract(from);
        double length = span.length();
        if (length < 1.0E-4) {
            return;
        }
        int steps = Math.max(1, (int) Math.ceil(length / every));
        for (int i = 0; i <= steps; i++) {
            Vec3 at = from.add(span.scale(i / (double) steps));
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public static void stone(ServerLevel level, double x, double y, double z, int count) {
        level.sendParticles(ModRegistries.PILLAR_STONE.get(), x, y, z, count, 0.3, 0.5, 0.3, 0.12);
    }
}
