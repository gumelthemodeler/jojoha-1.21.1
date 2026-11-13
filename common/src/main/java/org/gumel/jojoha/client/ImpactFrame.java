package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public final class ImpactFrame {
    private static final long HOLD_MS = 260L;
    private static final long FADE_MS = 740L;

    private static long startedAt = Long.MIN_VALUE;
    private static float peak;

    private ImpactFrame() {
    }

    public static void begin(float strength) {
        if (!JojohaClientConfig.impactFrames()) {
            return;
        }
        float asked = Mth.clamp(strength, 0F, 1F);
        if (startedAt != Long.MIN_VALUE && now() - startedAt < HOLD_MS && peak >= asked) {
            return;
        }
        startedAt = now();
        peak = asked;
    }

    public static void clear() {
        startedAt = Long.MIN_VALUE;
        peak = 0F;
    }

    public static float strength() {
        if (startedAt == Long.MIN_VALUE || Minecraft.getInstance().level == null) {
            return 0F;
        }

        long elapsed = now() - startedAt;
        if (elapsed < 0L) {
            return 0F;
        }
        if (elapsed < HOLD_MS) {
            return peak;
        }

        long fading = elapsed - HOLD_MS;
        if (fading >= FADE_MS) {
            startedAt = Long.MIN_VALUE;
            return 0F;
        }

        float left = 1F - (float) fading / FADE_MS;
        return peak * left * left * left;
    }

    private static final long INVERTED_MS = 100L;

    public static float mode() {
        if (startedAt == Long.MIN_VALUE) {
            return 0F;
        }
        long elapsed = now() - startedAt;
        return elapsed < INVERTED_MS ? 2F : elapsed < HOLD_MS ? 1F : 0F;
    }

    private static long now() {
        return System.nanoTime() / 1_000_000L;
    }
}
