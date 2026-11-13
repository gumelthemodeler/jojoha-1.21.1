package org.gumel.jojoha.stand.ripple;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.data.StandTuning;

public record RippleProfile(float frequency, float speed, float climb,
                            float haloTexels, float coreTexels,
                            float jitterTexels, float breakChance, float glow,
                            String maskSuffix) {
    public static final float MIN_FREQUENCY = 0.01F;
    public static final float MAX_FREQUENCY = 1.5F;
    public static final float MIN_SPEED = 0F;
    public static final float MAX_SPEED = 24F;
    public static final float MIN_CORE_TEXELS = 1F;
    public static final float MAX_CORE_TEXELS = 2F;
    public static final float MIN_HALO_TEXELS = 1F;
    public static final float MAX_HALO_TEXELS = 3.5F;
    public static final float MAX_JITTER_TEXELS = 4F;
    public static final float MAX_BREAK_CHANCE = 0.7F;
    public static final float MAX_GLOW = 1F;

    public RippleProfile bounded() {
        float halo = Mth.clamp(haloTexels, MIN_HALO_TEXELS, MAX_HALO_TEXELS);
        return new RippleProfile(
                Mth.clamp(frequency, MIN_FREQUENCY, MAX_FREQUENCY),
                Mth.clamp(speed, MIN_SPEED, MAX_SPEED),
                Mth.clamp(climb, 0F, 1F),
                halo,
                Mth.clamp(coreTexels, MIN_CORE_TEXELS, Math.min(MAX_CORE_TEXELS, halo)),
                Mth.clamp(jitterTexels, 0F, MAX_JITTER_TEXELS),
                Mth.clamp(breakChance, 0F, MAX_BREAK_CHANCE),
                Mth.clamp(glow, 0F, MAX_GLOW),
                maskSuffix);
    }

    public float spacing() {
        return Mth.TWO_PI / frequency;
    }

    public float travel(float x, float y) {
        return y * climb + x * (1F - climb);
    }

    public RippleProfile as(RippleMode mode, float intensity) {
        float lift = Math.max(0F, intensity);
        return new RippleProfile(
                frequency * mode.frequency(),
                speed * mode.speed(),
                climb,
                haloTexels * mode.width(),
                coreTexels * mode.width(),
                jitterTexels * mode.jitter(),
                breakChance,
                glow * mode.glow() * (0.7F + 0.3F * lift),
                maskSuffix).bounded();
    }

    public RippleProfile tuned(ResourceLocation standId) {
        String prefix = "ripple_" + standId.getPath() + "_";
        return new RippleProfile(
                StandTuning.value(prefix + "frequency", frequency),
                StandTuning.value(prefix + "speed", speed),
                StandTuning.value(prefix + "climb", climb),
                StandTuning.value(prefix + "halo_texels", haloTexels),
                StandTuning.value(prefix + "core_texels", coreTexels),
                StandTuning.value(prefix + "jitter_texels", jitterTexels),
                StandTuning.value(prefix + "break_chance", breakChance),
                StandTuning.value(prefix + "glow", glow),
                maskSuffix).bounded();
    }
}
