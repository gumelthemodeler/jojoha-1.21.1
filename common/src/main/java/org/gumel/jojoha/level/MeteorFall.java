package org.gumel.jojoha.level;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.fortune.Bizarre;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MeteorFall {
    private static final int START_HEIGHT = 200;
    private static final double SPEED = 0.75;

    private static final int SMOKE_TICKS = 1800;
    private static final int SMOKE_INTERVAL = 2;

    private static final double IMPACT_SOUND_RANGE = 220.0;

    private static final double TRAIL_RANGE = 320.0;

    private static final int SIGNAL_EVERY = 10;
    private static final double SMOKE_RANGE = 160.0;

    private static final int TRAIL_SAMPLES = 3;

    private static final double FIREBALL_RADIUS = 1.6;
    private static final int FIREBALL_POINTS = 7;

    private static final int RUMBLE_BEFORE = 40;
    private static final float RUMBLE_SCALE = 0.4F;

    private static final int ROAR_EVERY = 16;

    private static final int BLAST_TICKS = 60;
    private static final double RING_SPEED = 1.25;
    private static final double RING_MAX = 34.0;

    private static final int RING_POINTS = 20;

    private static final int EJECTA_TICKS = 14;
    private static final int EJECTA_PER_TICK = 8;

    private static void farParticles(ServerLevel level, ParticleOptions type,
                                     double x, double y, double z, int count,
                                     double dx, double dy, double dz, double speed, double range) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= range * range) {
                level.sendParticles(player, type, true, x, y, z, count, dx, dy, dz, speed);
            }
        }
    }

    private static final class Falling {
        private final ServerLevel level;
        private final BlockPos target;
        private Vec3 position;

        private int ticks;

        private boolean rumbled;

        private Falling(ServerLevel level, BlockPos target, Vec3 position) {
            this.level = level;
            this.target = target;
            this.position = position;
        }
    }

    private record Smouldering(ServerLevel level, BlockPos at, int radius, long until) {
    }

    private record Blast(ServerLevel level, BlockPos at, long until) {
    }

    private static final List<Falling> IN_FLIGHT = new ArrayList<>();
    private static final List<Blast> BLASTS = new ArrayList<>();
    private static final List<Smouldering> SMOULDERING = new ArrayList<>();

    private MeteorFall() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    public static void begin(ServerLevel level, BlockPos target) {
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                target.getX(), target.getZ());
        BlockPos landing = new BlockPos(target.getX(), ground, target.getZ());

        IN_FLIGHT.add(new Falling(level, landing,
                new Vec3(landing.getX() + 0.5, landing.getY() + START_HEIGHT, landing.getZ() + 0.5)));

        level.playSound(null, landing, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER,
                8.0F, 0.4F);
    }

    public static boolean isFalling(BlockPos target) {
        return IN_FLIGHT.stream().anyMatch(falling -> falling.target.closerThan(target, 64));
    }

    private static void tick() {
        IN_FLIGHT.removeIf(MeteorFall::advance);
        BLASTS.removeIf(MeteorFall::blast);
        SMOULDERING.removeIf(MeteorFall::smoke);
    }

    private static boolean advance(Falling falling) {
        ServerLevel level = falling.level;
        Vec3 from = falling.position;
        falling.position = falling.position.subtract(0, SPEED, 0);

        for (int sample = 0; sample < TRAIL_SAMPLES; sample++) {
            double along = (sample + 1) / (double) TRAIL_SAMPLES;
            double y = Mth.lerp(along, from.y, falling.position.y);
            fireball(level, falling.position.x, y, falling.position.z, sample == TRAIL_SAMPLES - 1);
        }

        farParticles(level, ParticleTypes.LARGE_SMOKE, falling.position.x, falling.position.y + 2.5,
                falling.position.z, 10, 0.9, 0.9, 0.9, 0.01, TRAIL_RANGE);
        farParticles(level, ParticleTypes.SMOKE, falling.position.x, falling.position.y + 1.0,
                falling.position.z, 8, 0.5, 0.5, 0.5, 0.0, TRAIL_RANGE);

        farParticles(level, ParticleTypes.WHITE_SMOKE, falling.position.x,
                falling.position.y - 1.5, falling.position.z, 4, 0.7, 0.2, 0.7, 0.02, TRAIL_RANGE);

        if (falling.ticks++ % SIGNAL_EVERY == 0) {
            farParticles(level, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, falling.position.x,
                    falling.position.y + 2.0, falling.position.z, 1, 0.0, 0.0, 0.0, 0.0, TRAIL_RANGE);
        }

        double toGo = falling.position.y - falling.target.getY();

        if (falling.ticks % ROAR_EVERY == 0) {
            float near = (float) Mth.clamp(1.0 - toGo / START_HEIGHT, 0.15, 1.0);
            level.playSound(null, BlockPos.containing(falling.position),
                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.WEATHER,
                    3.0F + 5.0F * near, 0.35F + 0.25F * near);
        }

        if (!falling.rumbled && toGo <= RUMBLE_BEFORE * SPEED) {
            falling.rumbled = true;
            NetworkHandler.broadcastMeteorImpact(level, falling.target,
                    IMPACT_SOUND_RANGE, RUMBLE_SCALE);
        }

        if (falling.position.y > falling.target.getY() + 1) {
            return false;
        }

        land(level, falling.target);
        return true;
    }

    private static void fireball(ServerLevel level, double x, double y, double z, boolean leading) {
        for (int point = 0; point < FIREBALL_POINTS; point++) {
            double angle = point * (Math.PI * 2 / FIREBALL_POINTS);
            double lift = ((point % 3) - 1) * FIREBALL_RADIUS * 0.6;

            farParticles(level, ParticleTypes.FLAME,
                    x + Math.cos(angle) * FIREBALL_RADIUS, y + lift,
                    z + Math.sin(angle) * FIREBALL_RADIUS,
                    3, 0.2, 0.2, 0.2, 0.01, TRAIL_RANGE);
        }

        if (!leading) {
            return;
        }

        farParticles(level, ParticleTypes.LAVA, x, y, z, 6, 0.15, 0.15, 0.15, 0.0, TRAIL_RANGE);
        farParticles(level, ParticleTypes.FLAME, x, y, z, 24, 0.35, 0.35, 0.35, 0.0, TRAIL_RANGE);
        farParticles(level, ParticleTypes.SMALL_FLAME, x, y, z, 16, 0.6, 0.6, 0.6, 0.02,
                TRAIL_RANGE);
    }

    private static void land(ServerLevel level, BlockPos at) {
        RandomSource random = level.random;
        Bizarre.meteor(level, at);

        ModRegistries.METEOR_CRASH.get().place(new FeaturePlaceContext<>(
                Optional.empty(), level, level.getChunkSource().getGenerator(), random, at,
                NoneFeatureConfiguration.INSTANCE));

        level.playSound(null, at, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 12.0F, 0.5F);
        farParticles(level, ParticleTypes.EXPLOSION, at.getX() + 0.5, at.getY() + 1,
                at.getZ() + 0.5, 40, 5.0, 2.0, 5.0, 0.0, TRAIL_RANGE);
        farParticles(level, ParticleTypes.FLAME, at.getX() + 0.5, at.getY() + 1,
                at.getZ() + 0.5, 60, 4.0, 1.5, 4.0, 0.35, TRAIL_RANGE);
        farParticles(level, ParticleTypes.EXPLOSION_EMITTER, at.getX() + 0.5, at.getY() + 1,
                at.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0, TRAIL_RANGE);

        farParticles(level, ParticleTypes.FLAME, at.getX() + 0.5, at.getY() + 2,
                at.getZ() + 0.5, 90, 1.2, 6.0, 1.2, 0.9, TRAIL_RANGE);
        farParticles(level, ParticleTypes.LAVA, at.getX() + 0.5, at.getY() + 1,
                at.getZ() + 0.5, 40, 1.5, 1.0, 1.5, 0.0, TRAIL_RANGE);

        BLASTS.add(new Blast(level, at, level.getGameTime() + BLAST_TICKS));
        SMOULDERING.add(new Smouldering(level, at, 12, level.getGameTime() + SMOKE_TICKS));
        NetworkHandler.broadcastMeteorImpact(level, at, IMPACT_SOUND_RANGE,
                1.0F);
    }

    private static boolean blast(Blast blast) {
        ServerLevel level = blast.level();
        long remaining = blast.until() - level.getGameTime();
        if (remaining <= 0) {
            return true;
        }

        int elapsed = (int) (BLAST_TICKS - remaining);

        if (elapsed % 2 == 0) {
            ring(level, blast.at(), Math.min(RING_MAX, elapsed * RING_SPEED),
                    1F - elapsed / (float) BLAST_TICKS);
        }

        if (elapsed < EJECTA_TICKS) {
            ejecta(level, blast.at());
        }

        return false;
    }

    private static void ring(ServerLevel level, BlockPos at, double radius, float strength) {
        for (int point = 0; point < RING_POINTS; point++) {
            double angle = point * (Math.PI * 2 / RING_POINTS);
            int x = at.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = at.getZ() + (int) Math.round(Math.sin(angle) * radius);
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

            farParticles(level, ParticleTypes.LARGE_SMOKE, x + 0.5, ground + 0.6, z + 0.5,
                    3, 0.5, 0.3, 0.5, 0.03, TRAIL_RANGE);
            farParticles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, x + 0.5, ground + 0.3, z + 0.5,
                    1, 0.4, 0.1, 0.4, 0.01, TRAIL_RANGE);

            if (strength > 0.6F && radius < RING_MAX * 0.5) {
                farParticles(level, ParticleTypes.FLAME, x + 0.5, ground + 0.4, z + 0.5,
                        2, 0.3, 0.2, 0.3, 0.05, TRAIL_RANGE);
            }
        }
    }

    private static void ejecta(ServerLevel level, BlockPos at) {
        for (int shot = 0; shot < EJECTA_PER_TICK; shot++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double out = 2.0 + level.random.nextDouble() * 6.0;
            double x = at.getX() + 0.5 + Math.cos(angle) * out;
            double z = at.getZ() + 0.5 + Math.sin(angle) * out;

            farParticles(level, ParticleTypes.LAVA, x, at.getY() + 1.0, z,
                    2, 0.0, 0.0, 0.0, 0.0, TRAIL_RANGE);
            farParticles(level, ParticleTypes.LARGE_SMOKE, x, at.getY() + 1.5, z,
                    4, 0.3, 0.8, 0.3, 0.35, TRAIL_RANGE);
        }
    }

    private static boolean smoke(Smouldering crater) {
        ServerLevel level = crater.level();
        long remaining = crater.until() - level.getGameTime();
        if (remaining <= 0) {
            return true;
        }
        if (level.getGameTime() % SMOKE_INTERVAL != 0) {
            return false;
        }

        float left = remaining / (float) SMOKE_TICKS;
        int columns = Math.max(2, Math.round(6 * left));

        for (int i = 0; i < columns; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double out = level.random.nextDouble() * crater.radius();
            int x = crater.at().getX() + (int) (Math.cos(angle) * out);
            int z = crater.at().getZ() + (int) (Math.sin(angle) * out);
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

            farParticles(level, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE,
                    x + 0.5, ground + 0.5, z + 0.5, 0, 0.0, 0.08, 0.0, 1.0, SMOKE_RANGE);
            farParticles(level, ParticleTypes.LARGE_SMOKE,
                    x + 0.5, ground + 1.0, z + 0.5, 2, 0.6, 0.4, 0.6, 0.01, SMOKE_RANGE);

            if (left > 0.5F) {
                farParticles(level, ParticleTypes.FLAME, x + 0.5, ground + 0.3, z + 0.5,
                        1, 0.3, 0.1, 0.3, 0.0, SMOKE_RANGE);
            }
        }
        return false;
    }
}
