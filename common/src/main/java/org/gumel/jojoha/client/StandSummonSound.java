package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.registry.ModSounds;

/**
 * The caster's own summon sound, ramped in over a few ticks instead of starting at full volume.
 *
 * <p>A fade needs per-tick volume control, which a server-side {@code playSound} can't give -
 * so the server deliberately excludes the caster from its broadcast (see
 * {@code StandSummonHandler.summon}) and this plays their copy locally, the same split already
 * used for the Stand aura. Everyone else still hears the plain, un-faded sound.
 */
public final class StandSummonSound extends AbstractTickableSoundInstance {
    private static final int FADE_IN_TICKS = 6;
    // Must be above zero: SoundEngine.play() drops any instance whose calculated volume is zero
    // outright ("Skipped playing sound {}, volume was zero"), so starting at silence would mean
    // the sound never begins at all rather than fading up from nothing.
    private static final float START_VOLUME = 0.05F;

    private int age;

    private StandSummonSound() {
        super(ModSounds.STAND_SUMMON.get(), SoundSource.PLAYERS, RandomSource.create());
        this.volume = START_VOLUME;
        // Anchored to the listener rather than a world position - it's the caster's own Stand
        // answering, so it shouldn't drift or attenuate as they move during the fade.
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        this.age++;
        this.volume = this.age >= FADE_IN_TICKS
                ? 1F
                : START_VOLUME + (1F - START_VOLUME) * (this.age / (float) FADE_IN_TICKS);
    }

    // --- Cast detection -----------------------------------------------------------------

    private static boolean wasSummoned;
    private static LocalPlayer lastPlayer;

    /**
     * Watches the synced cast flag for a rising edge. Called once per client tick from each
     * platform's tick hook.
     */
    public static void tickClient() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean summoned = player != null && ClientPlayerDataCache.data.standSummoned;

        // On a fresh join or respawn, adopt whatever state the player is already in rather than
        // treating an already-active cast as a brand new one and replaying the sound on login.
        if (player != lastPlayer) {
            lastPlayer = player;
            wasSummoned = summoned;
            return;
        }

        if (summoned && !wasSummoned) {
            mc.getSoundManager().play(new StandSummonSound());
        }
        wasSummoned = summoned;
    }
}
