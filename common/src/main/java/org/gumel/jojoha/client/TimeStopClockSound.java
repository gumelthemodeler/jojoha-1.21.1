package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.gumel.jojoha.registry.ModSounds;

public final class TimeStopClockSound extends AbstractTickableSoundInstance {
    private static final float VOLUME = 0.55F;

    public TimeStopClockSound() {
        super(ModSounds.CLOCK_TICK.get(), SoundSource.PLAYERS, RandomSource.create());
        this.looping = true;
        this.volume = VOLUME;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            this.x = minecraft.player.getX();
            this.y = minecraft.player.getY();
            this.z = minecraft.player.getZ();
        }
    }

    @Override
    public boolean canPlaySound() {
        return TimeStopView.active();
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || !TimeStopView.active()) {
            stop();
            return;
        }

        this.x = minecraft.player.getX();
        this.y = minecraft.player.getY();
        this.z = minecraft.player.getZ();
    }
}
