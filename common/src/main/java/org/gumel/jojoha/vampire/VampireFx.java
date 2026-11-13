package org.gumel.jojoha.vampire;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandAuraEffect;

public final class VampireFx {
    public static final float BLOOD_RED = 0.62F;
    public static final float BLOOD_GREEN = 0.05F;
    public static final float BLOOD_BLUE = 0.09F;

    public static final float RIPPER_RED = 1.0F;
    public static final float RIPPER_GREEN = 0.24F;
    public static final float RIPPER_BLUE = 0.62F;

    public static final int BLOOD_ARGB = 0xFF9E0E17;

    private static final double DEFAULT_SPREAD = 0.25;

    private static final int AURA_PER_TICK = 2;

    private VampireFx() {
    }

    public static void blood(ServerLevel level, double x, double y, double z, int count,
                             double spread, double speed) {
        level.sendParticles(ModRegistries.BLOOD_MOTE.get(), x, y, z,
                count, spread, spread, spread, speed);
    }

    public static void ring(ServerLevel level, double x, double y, double z,
                            double scale, int band) {
        level.sendParticles(ModRegistries.VAMPIRE_RING.get(), x, y, z, 0, scale, band, 0.0, 1.0);
    }

    public static void ripperRing(ServerLevel level, double x, double y, double z,
                                  double scale, int band) {
        level.sendParticles(ModRegistries.RIPPER_RING.get(), x, y, z, 0, scale, band, 0.0, 1.0);
    }

    public static void ripperMist(ServerLevel level, double x, double y, double z, int count,
                                  double spread) {
        level.sendParticles(ModRegistries.RIPPER_MIST.get(), x, y, z,
                count, spread, spread, spread, 0.01);
    }

    public static void spray(ServerLevel level, Vec3 at, int count) {
        blood(level, at.x, at.y, at.z, count, DEFAULT_SPREAD, 0.06);
    }

    public static void aura(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < AURA_PER_TICK; i++) {
            Vec3 offset = StandAuraEffect.auraOffset(
                    player.tickCount * AURA_PER_TICK + i, random);
            blood(level, player.getX() + offset.x, player.getY() + offset.y,
                    player.getZ() + offset.z, 1, 0.0, 0.0);
        }
    }

    public static void frost(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < AURA_PER_TICK; i++) {
            Vec3 offset = StandAuraEffect.auraOffset(
                    player.tickCount * AURA_PER_TICK + i, random);
            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    player.getX() + offset.x, player.getY() + offset.y, player.getZ() + offset.z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public static void frost(ServerLevel level, double x, double y, double z, int count) {
        level.sendParticles(ParticleTypes.SNOWFLAKE,
                x, y, z, count, 0.35, 0.35, 0.35, 0.02);
    }

    public static void aura(ServerLevel level, double x, double y, double z, int count) {
        blood(level, x, y, z, count, 0.45, 0.18);
        ring(level, x, y, z, 1.4, 1);
    }

    public static Vec3 eye(ServerPlayer player, double side) {
        float yaw = player.yBodyRot * ((float) Math.PI / 180F);
        Vec3 across = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw));
        return player.getEyePosition().add(across.scale(EYE_SPACING * side));
    }

    private static final double EYE_SPACING = 0.13;
}
