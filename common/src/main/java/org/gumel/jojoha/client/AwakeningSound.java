package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandManifestation;

public final class AwakeningSound extends AbstractTickableSoundInstance {
    public static final int SWELL_TICKS = 152;
    public static final int START_TICK = StraySoul.FLIGHT_TICKS + StandManifestation.ARMOR_AT - SWELL_TICKS;

    private static final float VOLUME = 0.8F;
    private static final int FADE_TICKS = 10;
    private static final int CATCH_TICKS = 3;
    private static final int VISION_GRACE_TICKS = 10;

    private static AwakeningSound playing;
    private static boolean cued;

    private boolean fading;

    private AwakeningSound() {
        super(ModSounds.AWAKEN_STAND_SOUND.get(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.volume = VOLUME;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (!fading) {
            return;
        }
        volume -= VOLUME / FADE_TICKS;
        if (volume <= 0F) {
            volume = 0F;
            stop();
        }
    }

    public static void tick(Minecraft minecraft, float now) {
        if (playing != null && (playing.isStopped() || !minecraft.getSoundManager().isActive(playing))) {
            playing = null;
        }

        int t = clock(minecraft, now);
        if (t < 0) {
            cued = false;
            if (playing != null) {
                playing.fading = true;
            }
            return;
        }

        if (!cued && t >= START_TICK && t < START_TICK + CATCH_TICKS) {
            cued = true;
            minecraft.getMusicManager().stopPlaying();
            playing = new AwakeningSound();
            minecraft.getSoundManager().play(playing);
        }
    }

    public static void clear() {
        if (playing != null) {
            Minecraft.getInstance().getSoundManager().stop(playing);
        }
        playing = null;
        cued = false;
    }

    private static int clock(Minecraft minecraft, float now) {
        Player player = minecraft.player;
        if (player == null) {
            return -1;
        }
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof StandEntity vision && vision.isManifesting() && vision.manifestUser() != null
                    && vision.manifestUser().getUUID().equals(player.getUUID())) {
                return StraySoul.FLIGHT_TICKS + vision.tickCount;
            }
        }
        StraySoul.Flight flight = StraySoul.flightOf(player.getUUID());
        if (flight != null && now - flight.startTick() <= StraySoul.FLIGHT_TICKS + VISION_GRACE_TICKS) {
            return (int) (now - flight.startTick());
        }
        return -1;
    }
}
