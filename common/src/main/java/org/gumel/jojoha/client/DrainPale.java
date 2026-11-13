package org.gumel.jojoha.client;

import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

public final class DrainPale {
    private static final float PEAK = 0.45F;

    private static final float ONSET = 12F;
    private static final float RELEASE = 25F;

    private static final Map<Integer, Pale> DRAINED = new HashMap<>();

    private DrainPale() {
    }

    public static void set(int entityId, int ticks, float now) {
        Pale pale = DRAINED.get(entityId);
        if (pale == null || now >= pale.until) {
            DRAINED.put(entityId, new Pale(now, now + ticks));
            return;
        }
        pale.until = now + ticks;
    }

    public static void clear() {
        DRAINED.clear();
    }

    public static float paleness(int entityId, float now) {
        Pale pale = DRAINED.get(entityId);
        if (pale == null) {
            return 0F;
        }
        if (now >= pale.until) {
            DRAINED.remove(entityId);
            return 0F;
        }

        float rising = Mth.clamp((now - pale.since) / ONSET, 0F, 1F);
        float falling = Mth.clamp((pale.until - now) / RELEASE, 0F, 1F);
        return Math.min(rising, falling) * PEAK;
    }

    private static final class Pale {
        private final float since;
        private float until;

        private Pale(float since, float until) {
            this.since = since;
            this.until = until;
        }
    }
}
