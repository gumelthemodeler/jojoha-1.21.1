package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.level.ModBiomes;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;

import java.util.ArrayList;
import java.util.List;

public final class PhantomAmbience {
    private static final int WIND_MIN_TICKS = 2600;
    private static final int WIND_MAX_TICKS = 7200;
    private static final float WIND_VOLUME = 0.16F;

    private static final int NIGHT_MIN_TICKS = 900;
    private static final int NIGHT_MAX_TICKS = 2400;
    private static final float NIGHT_VOLUME = 0.9F;

    public static final float MIST_START_PULL = 0.95F;
    public static final float MIST_END_PULL = 0.7F;
    private static final float MIST_STEP = 0.02F;

    private static float mist;
    private static float mistBefore;

    public static final float HAZE_PULL = 0.14F;
    private static final float HAZE_STEP = 0.01F;
    private static final int HAZE_SHIMMER_CHANCE = 3;
    private static final int HAZE_SHIMMER_REACH = 12;

    private static float haze;
    private static float hazeBefore;

    private static final int STAR_CHANCE = 45;
    private static final int STAR_MOST = 3;
    private static final int STAR_TICKS = 9;
    private static final double STAR_SPEED = 3.2;
    private static final double STAR_REACH = 70.0;
    private static final double STAR_HEIGHT = 55.0;
    private static final int STAR_TRAIL = 3;

    private record Star(Vec3 start, Vec3 step, int[] age) {
    }

    private static final List<Star> STARS = new ArrayList<>();

    private static final RandomSource RANDOM = RandomSource.create();

    private static int ticksUntilWind = WIND_MIN_TICKS;
    private static int ticksUntilNight = NIGHT_MIN_TICKS;

    private PhantomAmbience() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        boolean here = level.getBiome(player.blockPosition()).is(ModBiomes.IS_MOORS);

        mistBefore = mist;
        float wanted = level.getBiome(player.blockPosition()).is(ModBiomes.MISTY_HOLLOW) ? 1F : 0F;
        mist = Mth.approach(mist, wanted, MIST_STEP);

        hazeBefore = haze;
        boolean dunes = level.getBiome(player.blockPosition()).is(ModBiomes.DUNE_SEA) && level.isDay()
                && level.canSeeSky(player.blockPosition());
        haze = Mth.approach(haze, dunes ? 1F : 0F, HAZE_STEP);
        if (haze > 0.5F && !minecraft.isPaused() && RANDOM.nextInt(HAZE_SHIMMER_CHANCE) == 0) {
            shimmer(level, player);
        }

        stars(level, player, minecraft.isPaused());

        ticksUntilWind = run(level, player, ticksUntilWind, here,
                ModSounds.PHANTOM_WIND.get(), WIND_VOLUME, WIND_MIN_TICKS, WIND_MAX_TICKS);

        ticksUntilNight = run(level, player, ticksUntilNight, here && level.isNight(),
                ModSounds.PHANTOM_AMBIENCE.get(), NIGHT_VOLUME, NIGHT_MIN_TICKS, NIGHT_MAX_TICKS);
    }

    private static int run(ClientLevel level, LocalPlayer player, int ticks, boolean conditions,
                           SoundEvent sound, float volume, int min, int max) {
        if (!conditions) {
            return interval(min, max);
        }
        if (--ticks > 0) {
            return ticks;
        }
        level.playLocalSound(player.blockPosition(), sound, SoundSource.AMBIENT,
                volume, 0.9F + RANDOM.nextFloat() * 0.2F, false);
        return interval(min, max);
    }

    public static float haze(float partialTick) {
        return Mth.lerp(partialTick, hazeBefore, haze);
    }

    private static void shimmer(ClientLevel level, LocalPlayer player) {
        double x = player.getX() + (RANDOM.nextDouble() * 2.0 - 1.0) * HAZE_SHIMMER_REACH;
        double z = player.getZ() + (RANDOM.nextDouble() * 2.0 - 1.0) * HAZE_SHIMMER_REACH;
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x),
                (int) Math.floor(z));
        level.addParticle(ModRegistries.HEAT_SHIMMER.get(), x, ground + 0.3, z, 0.0, 0.0, 0.0);
    }

    private static void stars(ClientLevel level, LocalPlayer player, boolean paused) {
        if (paused) {
            return;
        }
        boolean sky = level.getBiome(player.blockPosition()).is(ModBiomes.METEOR_FIELD) && level.isNight();
        if (sky && STARS.size() < STAR_MOST && RANDOM.nextInt(STAR_CHANCE) == 0) {
            double angle = RANDOM.nextDouble() * Mth.TWO_PI;
            Vec3 start = player.position().add(Math.cos(angle) * STAR_REACH * RANDOM.nextDouble(),
                    STAR_HEIGHT + RANDOM.nextDouble() * 20.0, Math.sin(angle) * STAR_REACH * RANDOM.nextDouble());
            double heading = RANDOM.nextDouble() * Mth.TWO_PI;
            Vec3 step = new Vec3(Math.cos(heading), -0.35 - RANDOM.nextDouble() * 0.3, Math.sin(heading))
                    .normalize().scale(STAR_SPEED);
            STARS.add(new Star(start, step, new int[]{0}));
        }
        STARS.removeIf(star -> {
            int age = star.age()[0]++;
            for (int i = 0; i < STAR_TRAIL; i++) {
                Vec3 at = star.start().add(star.step().scale(age + i / (double) STAR_TRAIL));
                level.addAlwaysVisibleParticle(ParticleTypes.END_ROD, true, at.x, at.y, at.z, 0.0, 0.0, 0.0);
            }
            return age >= STAR_TICKS;
        });
    }

    public static float mist(float partialTick) {
        return Mth.lerp(partialTick, mistBefore, mist);
    }

    private static int interval(int min, int max) {
        return min + RANDOM.nextInt(max - min);
    }
}
