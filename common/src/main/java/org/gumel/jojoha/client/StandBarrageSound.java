package org.gumel.jojoha.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;

/**
 * The shout that carries a flurry, cut off when the flurry is.
 *
 * <p>The recording runs just under five seconds and a barrage lasts exactly three, so played as a
 * one-shot it went on for the better part of two seconds after the last punch had landed - the Stand
 * standing there in silence while it was still being heard to swing. This follows the Stand's own
 * barraging flag instead and fades out the moment the blows stop.
 *
 * <p>Driven from that flag rather than from a packet, which is also what makes it right for everyone
 * else: the flag is entity data and is already synced to every client that can see the Stand, so a
 * bystander hears the flurry start and stop at the same instants the owner does. A sound played
 * server-side at the moment of the first blow could only ever have been a one-shot, because there is
 * nothing to tell it when to stop.
 */
public final class StandBarrageSound extends AbstractTickableSoundInstance {
    /** How long the tail takes once the punches cease. Short - this is a cut, not a decay. */
    private static final int FADE_TICKS = 5;
    private static final float PEAK = 1.1F;

    private final StandEntity stand;
    private int fade = FADE_TICKS;

    public StandBarrageSound(StandEntity stand) {
        super(ModSounds.SP_BARRAGE.get(), SoundSource.PLAYERS, RandomSource.create());
        this.stand = stand;
        this.looping = false;
        this.volume = PEAK;
        this.x = stand.getX();
        this.y = stand.getY();
        this.z = stand.getZ();
    }

    /** The Stand this is following, so the tracker can tell whether one is already playing. */
    public StandEntity stand() {
        return stand;
    }

    @Override
    public void tick() {
        if (!stand.isAlive()) {
            stop();
            return;
        }

        // Followed rather than fixed at the point it started: a barraging Stand is usually moving,
        // and a shout left behind at the spot the flurry began is a shout coming from nowhere.
        this.x = stand.getX();
        this.y = stand.getY();
        this.z = stand.getZ();

        if (stand.isBarraging()) {
            this.fade = FADE_TICKS;
            this.volume = PEAK;
            return;
        }

        if (--this.fade <= 0) {
            stop();
            return;
        }

        this.volume = PEAK * (this.fade / (float) FADE_TICKS);
    }
}
