package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.gumel.jojoha.level.ModBiomes;
import org.gumel.jojoha.registry.ModSounds;

/**
 * What the Phantom Highlands sound like: wind through the day, and something else after dark.
 *
 * <h2>Why neither of these is a biome effect any more</h2>
 *
 * <p>The biome format offers exactly two hooks and both are the wrong shape for what is wanted here.
 *
 * <p>{@code ambient_sound} is a loop with no dial at all - {@code BiomeAmbientSoundsHandler} builds
 * its {@code LoopSoundInstance} at a hardcoded volume of 1.0 and restarts it the moment it ends. It
 * cannot be made quieter and it cannot be made to leave a gap, because it was never meant to: it is
 * for a bed of noise you stop hearing, and this wind is loud enough to be a presence.
 *
 * <p>{@code additions_sound} has one dial, a per-tick chance, and no notion of time of day. Rolling
 * a chance every tick is also a poor way to say "rarely" - two howls can land a second apart and
 * then nothing for a minute, which reads as broken rather than as sparse.
 *
 * <p>So both are countdowns instead. A sound plays, then a fresh wait passes before the next is even
 * considered, which guarantees the silence rather than hoping for it.
 */
public final class PhantomAmbience {

    /**
     * The wind: quiet, and with real gaps.
     *
     * <p>The clip itself runs 25 seconds, so the wait is counted long enough to leave silence after
     * it rather than butting one gust against the next - a shortest wait of 65 seconds is 40 seconds
     * of nothing, and the longest is over two minutes of it.
     */
    private static final int WIND_MIN_TICKS = 1300;
    private static final int WIND_MAX_TICKS = 3000;
    private static final float WIND_VOLUME = 0.3F;

    /** The howls and breaths: night only, and rarer still. */
    private static final int NIGHT_MIN_TICKS = 900;
    private static final int NIGHT_MAX_TICKS = 2400;
    private static final float NIGHT_VOLUME = 0.9F;

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

        boolean here = level.getBiome(player.blockPosition()).is(ModBiomes.PHANTOM_HIGHLANDS);

        ticksUntilWind = run(level, player, ticksUntilWind, here,
                ModSounds.PHANTOM_WIND.get(), WIND_VOLUME, WIND_MIN_TICKS, WIND_MAX_TICKS);

        ticksUntilNight = run(level, player, ticksUntilNight, here && level.isNight(),
                ModSounds.PHANTOM_AMBIENCE.get(), NIGHT_VOLUME, NIGHT_MIN_TICKS, NIGHT_MAX_TICKS);
    }

    /**
     * Run one countdown down a tick, and play its sound when it lands.
     *
     * <p>While the conditions do not hold the clock is reset rather than paused. Otherwise time spent
     * away would bank a sound that fires the instant you walk back in - or, for the night one, the
     * instant dusk arrives.
     *
     * @return what the countdown should be next tick
     */
    private static int run(ClientLevel level, LocalPlayer player, int ticks, boolean conditions,
                           SoundEvent sound, float volume, int min, int max) {
        if (!conditions) {
            return interval(min, max);
        }
        if (--ticks > 0) {
            return ticks;
        }
        // Played at the player rather than from a point in the world: it is the place making a noise,
        // not a thing standing somewhere, and a source you could walk up to would invite looking for
        // it.
        level.playLocalSound(player.blockPosition(), sound, SoundSource.AMBIENT,
                volume, 0.9F + RANDOM.nextFloat() * 0.2F, false);
        return interval(min, max);
    }

    private static int interval(int min, int max) {
        return min + RANDOM.nextInt(max - min);
    }
}
