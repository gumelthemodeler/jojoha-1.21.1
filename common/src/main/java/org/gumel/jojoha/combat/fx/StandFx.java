package org.gumel.jojoha.combat.fx;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.registry.ModRegistries;
import org.joml.Vector3f;

public final class StandFx {
    private static final Vector3f SPENT = new Vector3f(1F, 1F, 1F);

    private static final float MOTE_SIZE = 0.9F;
    private static final float TRAIL_SIZE = 1.1F;

    private static final double TRAIL_DENSITY = 2.5;

    private static final double TRAIL_WANDER = 0.16;

    private StandFx() {
    }

    public static void trail(ServerLevel level, Vec3 from, Vec3 to, int colour) {
        Vec3 along = to.subtract(from);
        double length = along.length();
        if (length < 1.0E-4) {
            return;
        }

        RandomSource random = level.getRandom();
        DustColorTransitionOptions dust =
                new DustColorTransitionOptions(rgb(colour), SPENT, TRAIL_SIZE);

        int steps = Math.max(2, (int) (length * TRAIL_DENSITY));
        for (int i = 0; i <= steps; i++) {
            Vec3 at = from.add(along.scale((double) i / steps)).add(
                    (random.nextDouble() - 0.5) * TRAIL_WANDER,
                    (random.nextDouble() - 0.5) * TRAIL_WANDER,
                    (random.nextDouble() - 0.5) * TRAIL_WANDER);
            level.sendParticles(dust, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    public static void burst(ServerLevel level, Vec3 at, int colour, int count, double speed) {
        RandomSource random = level.getRandom();
        DustColorTransitionOptions dust =
                new DustColorTransitionOptions(rgb(colour), SPENT, MOTE_SIZE);

        for (int i = 0; i < count; i++) {
            Vec3 way = new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5,
                    random.nextDouble() - 0.5);
            if (way.lengthSqr() < 1.0E-8) {
                continue;
            }
            Vec3 velocity = way.normalize().scale(speed * (0.4 + random.nextDouble() * 0.8));
            level.sendParticles(dust, at.x, at.y, at.z, 0,
                    velocity.x, velocity.y, velocity.z, 1.0);
        }
    }

    public static void kick(ServerLevel level, Vec3 at, Vec3 heading, int colour, int count,
                            double speed) {
        if (heading.lengthSqr() < 1.0E-8) {
            burst(level, at, colour, count, speed);
            return;
        }

        RandomSource random = level.getRandom();
        DustColorTransitionOptions dust =
                new DustColorTransitionOptions(rgb(colour), SPENT, MOTE_SIZE);

        for (Vec3 way : ImpactShape.cone(heading.normalize(), 42F, count, random)) {
            Vec3 velocity = way.scale(speed * (0.5 + random.nextDouble() * 0.7));
            level.sendParticles(dust, at.x, at.y, at.z, 0,
                    velocity.x, velocity.y, velocity.z, 1.0);
        }
    }

    public static void ring(ServerLevel level, Vec3 at, double scale, int band) {
        level.sendParticles(ModRegistries.IMPACT_RING.get(), at.x, at.y, at.z, 0,
                scale, band, 0.0, 1.0);
    }

    public static void haze(ServerLevel level, Vec3 at, int colour, int count, double spread) {
        RandomSource random = level.getRandom();
        DustColorTransitionOptions dust =
                new DustColorTransitionOptions(rgb(colour), SPENT, MOTE_SIZE);

        for (int i = 0; i < count; i++) {
            Vec3 point = at.add(
                    (random.nextDouble() - 0.5) * spread * 2,
                    (random.nextDouble() - 0.5) * spread * 2,
                    (random.nextDouble() - 0.5) * spread * 2);
            level.sendParticles(dust, point.x, point.y, point.z, 1, 0.0, 0.02, 0.0, 0.0);
        }
    }

    private static Vector3f rgb(int colour) {
        return new Vector3f(
                ((colour >> 16) & 0xFF) / 255F,
                ((colour >> 8) & 0xFF) / 255F,
                (colour & 0xFF) / 255F);
    }
}
