package org.gumel.jojoha.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.registry.ModSounds;

public final class CastDing {
    private static final float VOLUME = 0.3F;
    private static final float PITCH = 1.0F;

    private CastDing() {
    }

    public static void play(ServerPlayer player) {
        player.playNotifySound(ModSounds.MOVE_CAST.get(), SoundSource.PLAYERS, VOLUME, PITCH);
    }
}
