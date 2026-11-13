package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.gumel.jojoha.dream.DreamRealm;
import org.gumel.jojoha.registry.ModSounds;

public final class DreamAmbience {
    private static final int BREATH_MIN = 700, BREATH_MAX = 1600;
    private static final int TICK_MIN = 200, TICK_MAX = 640;
    private static final int CAVE_MIN = 400, CAVE_MAX = 1100;

    private static final float BREATH_VOLUME = 0.5F;
    private static final float TICK_VOLUME = 0.25F;
    private static final float CAVE_VOLUME = 0.3F;

    private static final RandomSource RANDOM = RandomSource.create();

    private static int untilBreath = BREATH_MIN;
    private static int untilTick = TICK_MIN;
    private static int untilCave = CAVE_MIN;

    private DreamAmbience() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        boolean dreaming = level.dimension().equals(DreamRealm.DREAM);

        untilBreath = countdown(level, player, untilBreath, dreaming,
                ModSounds.PHANTOM_AMBIENCE.get(), BREATH_VOLUME, 0.55F, BREATH_MIN, BREATH_MAX);
        untilTick = countdown(level, player, untilTick, dreaming,
                ModSounds.CLOCK_SINGLE.get(), TICK_VOLUME, 0.7F, TICK_MIN, TICK_MAX);
        untilCave = countdown(level, player, untilCave, dreaming,
                SoundEvents.AMBIENT_CAVE.value(), CAVE_VOLUME, 0.8F, CAVE_MIN, CAVE_MAX);
    }

    private static int countdown(ClientLevel level, LocalPlayer player, int ticks, boolean here,
                                 SoundEvent sound, float volume, float pitch, int min, int max) {
        if (!here) {
            return interval(min, max);
        }
        if (--ticks > 0) {
            return ticks;
        }
        level.playLocalSound(player.blockPosition(), sound, SoundSource.AMBIENT, volume,
                pitch + RANDOM.nextFloat() * 0.15F, false);
        return interval(min, max);
    }

    private static int interval(int min, int max) {
        return min + RANDOM.nextInt(Mth.clamp(max - min, 1, Integer.MAX_VALUE));
    }
}
