package org.gumel.jojoha.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.gumel.jojoha.stand.StandEntity;

public final class StandBarrageSound extends AbstractTickableSoundInstance {
    private static final int FADE_TICKS = 5;
    private static final float PEAK = 1.1F;
    private static final float QUIET_START = 0.08F;
    private static final float PEAK_PITCH = 1.08F;
    private static final float PITCH_STEP = 0.01F;

    private final StandEntity stand;
    private final int rise;
    private int age;
    private int length = Integer.MAX_VALUE;
    private boolean released;
    private int fade;
    private int fadeLength = FADE_TICKS;

    public StandBarrageSound(StandEntity stand, SoundEvent cry, int rise) {
        super(cry, SoundSource.PLAYERS, RandomSource.create());
        this.stand = stand;
        this.rise = Math.max(0, rise);
        this.looping = false;
        this.delay = 0;
        this.volume = this.rise > 0 ? PEAK * QUIET_START : PEAK;
        this.x = stand.getX();
        this.y = stand.getY();
        this.z = stand.getZ();
    }

    public StandEntity stand() {
        return stand;
    }

    public int age() {
        return age;
    }

    public int length() {
        return length;
    }

    public void length(int ticks) {
        this.length = ticks;
    }

    public boolean released() {
        return released;
    }

    public void release(int ticks) {
        if (released) {
            return;
        }
        released = true;
        fadeLength = Math.max(1, ticks);
        fade = fadeLength;
    }

    @Override
    public void tick() {
        if (!stand.isAlive() || age > length + FADE_TICKS) {
            stop();
            return;
        }

        this.x = stand.getX();
        this.y = stand.getY();
        this.z = stand.getZ();
        age++;
        this.pitch = Mth.approach(this.pitch, stand.atPeak() ? PEAK_PITCH : 1F, PITCH_STEP);

        if (!released && !stand.isBarraging()) {
            release(FADE_TICKS);
        }

        float swell = rise <= 0 ? 1F : Math.min(1F, QUIET_START + (1F - QUIET_START) * age / (float) rise);
        if (!released) {
            this.volume = PEAK * swell;
            return;
        }

        if (--this.fade <= 0) {
            stop();
            return;
        }
        this.volume = PEAK * swell * (this.fade / (float) fadeLength);
    }
}
