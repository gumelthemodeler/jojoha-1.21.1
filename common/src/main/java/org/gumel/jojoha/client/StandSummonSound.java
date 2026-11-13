package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.StandVoices;

public final class StandSummonSound extends AbstractTickableSoundInstance {
    private StandSummonSound(SoundEvent sound) {
        super(sound, SoundSource.PLAYERS, RandomSource.create());
        this.volume = 1F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
    }

    private static boolean wasSummoned;
    private static LocalPlayer lastPlayer;

    public static void tickClient() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean summoned = player != null && ClientPlayerDataCache.data.standSummoned;

        if (player != lastPlayer) {
            lastPlayer = player;
            wasSummoned = summoned;
            return;
        }

        if (summoned && !wasSummoned && StandTypes.hasBody(ClientPlayerDataCache.data)) {
            StandType type = StandTypes.byIdOrDefault(ClientPlayerDataCache.data.stand.standId());
            boolean shadow = StandVoices.shadowed(ClientPlayerDataCache.data.stand);
            mc.getSoundManager().play(new StandSummonSound(StandVoices.summon(type, shadow)));
            SoundEvent call = StandVoices.summonCall(type, shadow);
            if (call != null) {
                mc.getSoundManager().play(new StandSummonSound(call));
            }
        }
        wasSummoned = summoned;
    }
}
