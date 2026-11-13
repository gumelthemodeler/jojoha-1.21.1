package org.gumel.jojoha.client;

import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SubzeroChill {
    private static final float ONSET = 20F;
    private static final float RELEASE = 12F;

    private static final Map<UUID, Chill> COLD = new HashMap<>();

    private SubzeroChill() {
    }

    public static void begin(UUID player, int ticks, float now) {
        Chill chill = COLD.get(player);
        if (chill == null || now >= chill.until) {
            COLD.put(player, new Chill(now, now + ticks));
            return;
        }
        chill.until = now + ticks;
    }

    public static void end(UUID player, float now) {
        Chill chill = COLD.get(player);
        if (chill != null) {
            chill.until = Math.min(chill.until, now + RELEASE);
        }
    }

    public static void clear() {
        COLD.clear();
    }

    public static float strength(UUID player, float now) {
        Chill chill = COLD.get(player);
        if (chill == null) {
            return 0F;
        }
        if (now >= chill.until) {
            COLD.remove(player);
            return 0F;
        }

        float rising = Mth.clamp((now - chill.since) / ONSET, 0F, 1F);
        float falling = Mth.clamp((chill.until - now) / RELEASE, 0F, 1F);
        return Math.min(rising, falling);
    }

    private static final class Chill {
        private final float since;
        private float until;

        private Chill(float since, float until) {
            this.since = since;
            this.until = until;
        }
    }
}
