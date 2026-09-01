package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.gumel.jojoha.registry.ModSounds;

/**
 * The clock, running for exactly as long as time is stopped.
 *
 * <p>One continuous recording rather than a beat struck over and over, and that is the whole of the
 * fix. Every spacing problem the clock ever had came from the same place: beats were scheduled from
 * the client tick loop, which can only place them on fifty-millisecond boundaries, so a nominal
 * six-hundred-millisecond interval came out as an alternating five-fifty and six-hundred. On a
 * metronomic sound that is audible, and no amount of tuning the rate could remove it because the
 * quantisation was in the scheduler, not the numbers. A recording of a clock has its own timing
 * baked in and nothing here can smear it.
 *
 * <p>It also disposes of the three-act shape the clock used to have - wind down, silence, wind up.
 * It ticks, from the instant the world freezes until the instant it does not.
 *
 * <p>Looped, because the clip is a little over eight seconds and a stop runs anywhere from about
 * four to twenty-five; and stopped off {@link TimeStopView}, which is the same flag the fold and the
 * colour read, so the sound cannot outlive the effect it belongs to.
 */
public final class TimeStopClockSound extends AbstractTickableSoundInstance {
    /** Quiet enough to sit under the stop rather than over it. */
    private static final float VOLUME = 0.55F;

    public TimeStopClockSound() {
        super(ModSounds.CLOCK_TICK.get(), SoundSource.PLAYERS, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.volume = VOLUME;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            this.x = minecraft.player.getX();
            this.y = minecraft.player.getY();
            this.z = minecraft.player.getZ();
        }
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || !TimeStopView.active()) {
            // A hard stop on the tick time resumes, which is what was asked for - the clock is the
            // stop, so it has no business still being audible once there is nothing to stop.
            stop();
            return;
        }

        // Carried with the listener rather than left at the point the stop began. It is a world
        // sound, so it goes through attenuation and the player's own category volume - which is
        // what stops it sounding like it is being piped into their head - but the world it is
        // filling is wherever they are standing in it.
        this.x = minecraft.player.getX();
        this.y = minecraft.player.getY();
        this.z = minecraft.player.getZ();
    }
}
