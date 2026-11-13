package org.gumel.jojoha.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

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
