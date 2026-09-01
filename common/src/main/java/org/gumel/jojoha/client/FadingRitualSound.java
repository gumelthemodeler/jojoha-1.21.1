package org.gumel.jojoha.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * A one-shot sound that eases in and out instead of snapping on at full volume.
 *
 * <p>{@code Level.playSound} has no volume envelope - a sound is handed to the engine at one fixed
 * volume and keeps it until it ends - so a fade has to be driven a tick at a time. That is what
 * {@link AbstractTickableSoundInstance} is for: the engine re-reads volume, pitch and position from
 * the instance on every tick and pushes them into the live channel, so writing to {@link #volume}
 * here changes a sound that is already playing.
 *
 * <p>It also follows the player it belongs to, which matters for a clip this long: several seconds
 * is easily enough time to walk away from where a fixed-position sound was stamped.
 */
public final class FadingRitualSound extends AbstractTickableSoundInstance {
    private final Player target;
    private final float peakVolume;
    private final int durationTicks;
    private final int fadeInTicks;
    private final int fadeOutTicks;

    private int age;

    public FadingRitualSound(SoundEvent event, Player target, float peakVolume,
                             int durationTicks, int fadeInTicks, int fadeOutTicks) {
        super(event, SoundSource.PLAYERS, target.getRandom());
        this.target = target;
        this.peakVolume = peakVolume;
        this.durationTicks = durationTicks;
        this.fadeInTicks = fadeInTicks;
        this.fadeOutTicks = fadeOutTicks;

        this.looping = false;
        this.volume = 0F;
        this.x = target.getX();
        this.y = target.getY();
        this.z = target.getZ();
    }

    /**
     * Lets the sound start from silence.
     *
     * <p>Without this the fade-in would never be heard at all: {@code SoundEngine.play} throws away
     * any sound whose volume works out to zero at the moment it is submitted, and a fade-in starts
     * at exactly that. The default is to refuse, on the assumption that a silent sound is a bug.
     */
    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (age++ >= durationTicks || target.isRemoved()) {
            stop();
            return;
        }

        this.x = target.getX();
        this.y = target.getY();
        this.z = target.getZ();
        this.volume = peakVolume * envelope();
    }

    /**
     * Ramps up, holds, ramps down - squared at both ends.
     *
     * <p>Squared rather than linear because loudness is perceived roughly logarithmically: a
     * straight ramp in amplitude is heard as rushing in almost immediately and then sitting still,
     * whereas the curve reads as an even fade.
     */
    private float envelope() {
        if (fadeInTicks > 0 && age < fadeInTicks) {
            float t = age / (float) fadeInTicks;
            return t * t;
        }

        int remaining = durationTicks - age;
        if (fadeOutTicks > 0 && remaining < fadeOutTicks) {
            float t = remaining / (float) fadeOutTicks;
            return t * t;
        }

        return 1F;
    }
}
