package org.gumel.jojoha.stand.ripple;

import net.minecraft.util.Mth;

public final class RippleField {
    private static final int KNOT = 2;

    private static final float JITTER_RATE = 4F;
    private static final float FLICKER_RATE = 9F;

    private static final float CUTOFF = 0.25F;

    private static final float MAX_HALF_PHASE = 1.45F;

    private RippleField() {
    }

    public static float core(RippleProfile profile, float x, float y, double seconds, float phase) {
        return strand(profile, x, y, seconds, phase, profile.coreTexels());
    }

    public static float halo(RippleProfile profile, float x, float y, double seconds, float phase) {
        return strand(profile, x, y, seconds, phase, profile.haloTexels());
    }

    private static float strand(RippleProfile profile, float x, float y, double seconds, float phase,
                                float texels) {
        float base = profile.travel(x, y) * profile.frequency() - turns(seconds, profile.speed()) + phase;
        int strand = strandOf(base);
        float along = along(profile, x, y);
        if (broken(profile, strand, along, seconds)) {
            return 0F;
        }
        float bent = base + jitter(profile, strand, along, seconds) * profile.frequency();
        return shape(Math.max(0F, Mth.sin(bent)), power(profile, texels));
    }

    private static int strandOf(float base) {
        return Math.round((base - Mth.HALF_PI) / Mth.TWO_PI);
    }

    private static float along(RippleProfile profile, float x, float y) {
        float climb = profile.climb();
        float length = Math.max(1.0E-4F, Mth.sqrt(climb * climb + (1F - climb) * (1F - climb)));
        return (x * climb - y * (1F - climb)) / length;
    }

    public static float jitter(RippleProfile profile, int strand, float along, double seconds) {
        if (profile.jitterTexels() <= 0F) {
            return 0F;
        }
        float knots = along / KNOT;
        int knot = Mth.floor(knots);
        int step = Mth.floor(seconds * JITTER_RATE);
        float offset = Mth.lerp(knots - knot, noise(knot, strand, step), noise(knot + 1, strand, step));
        return (offset - 0.5F) * 2F * profile.jitterTexels();
    }

    public static boolean broken(RippleProfile profile, int strand, float along, double seconds) {
        if (profile.breakChance() <= 0F) {
            return false;
        }
        int step = Mth.floor(seconds * FLICKER_RATE);
        return noise(Mth.floor(along / KNOT) + 7919, strand + 104729, step) < profile.breakChance();
    }

    private static float noise(int x, int y, int step) {
        int hash = x * 374761393 + y * 668265263 + step * 1274126177;
        hash = (hash ^ (hash >>> 13)) * 1274126177;
        return ((hash ^ (hash >>> 16)) & 0xFFFF) / 65535F;
    }

    private static float shape(float crest, float power) {
        if (crest <= 0F) {
            return 0F;
        }

        float shaped = (float) Math.pow(crest, power);
        return shaped <= CUTOFF ? 0F : (shaped - CUTOFF) / (1F - CUTOFF);
    }

    public static float power(RippleProfile profile, float texels) {
        float half = Mth.clamp(texels * profile.frequency() * 0.5F, 0.05F, MAX_HALF_PHASE);
        return (float) (Math.log(CUTOFF) / Math.log(Math.cos(half)));
    }

    private static float turns(double seconds, float rate) {
        return (float) ((seconds * rate) % Mth.TWO_PI);
    }
}
