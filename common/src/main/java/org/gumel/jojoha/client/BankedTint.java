package org.gumel.jojoha.client;

import net.minecraft.Util;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

public final class BankedTint {
    private static final float DEEPEST = 0.6F;
    private static final long LINGER_MS = 1500L;
    private static final long FADE_MS = 250L;

    private static final Map<Integer, Banked> BANKED = new HashMap<>();

    private record Banked(float share, long at) {
    }

    private BankedTint() {
    }

    public static void heard(int entityId, int percent) {
        if (percent <= 0) {
            BANKED.remove(entityId);
            return;
        }
        BANKED.put(entityId, new Banked(Mth.clamp(percent / 100F, 0F, 1F), Util.getMillis()));
    }

    public static void clear() {
        BANKED.clear();
    }

    public static float depth(int entityId) {
        Banked banked = BANKED.get(entityId);
        if (banked == null) {
            return 0F;
        }
        long age = Util.getMillis() - banked.at();
        if (age >= LINGER_MS) {
            BANKED.remove(entityId);
            return 0F;
        }
        float fading = Mth.clamp((LINGER_MS - age) / (float) FADE_MS, 0F, 1F);
        return banked.share() * DEEPEST * fading;
    }

    public static int tint(int entityId, int argb) {
        float depth = depth(entityId);
        if (depth <= 0F) {
            return argb;
        }
        int a = argb >>> 24, r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        g = Math.round(g * (1F - depth));
        b = Math.round(b * (1F - depth));
        return a << 24 | r << 16 | g << 8 | b;
    }
}
