package org.gumel.jojoha.client;

import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.data.JojohaPlayerData;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

final class SkillCooldownView {
    private static final long MILLIS_PER_TICK = 50L;

    private static final Map<ResourceLocation, Long> KNOWN_EXPIRY = new HashMap<>();

    private static final Map<ResourceLocation, Long> ENDS_AT = new HashMap<>();

    private static final Map<ResourceLocation, Long> LASTS = new HashMap<>();

    private static WeakReference<Level> countedIn = new WeakReference<>(null);

    private SkillCooldownView() {
    }

    static boolean cooling(JojohaPlayerData data, ResourceLocation moveId, long gameTime,
                           Level level) {
        if (countedIn.get() != level) {
            countedIn = new WeakReference<>(level);
            forget();
        }

        Long expiry = data.moveCooldowns.get(moveId);
        if (expiry == null) {
            KNOWN_EXPIRY.remove(moveId);
            ENDS_AT.remove(moveId);
            LASTS.remove(moveId);
            return false;
        }

        if (!expiry.equals(KNOWN_EXPIRY.get(moveId))) {
            KNOWN_EXPIRY.put(moveId, expiry);
            long remaining = Math.max(0L, expiry - gameTime);
            ENDS_AT.put(moveId, Util.getMillis() + remaining * MILLIS_PER_TICK);
            LASTS.put(moveId, remaining * MILLIS_PER_TICK);
        }

        Long deadline = ENDS_AT.get(moveId);
        return deadline != null && Util.getMillis() < deadline;
    }

    static long remainingMillis(ResourceLocation moveId) {
        Long deadline = ENDS_AT.get(moveId);
        return deadline == null ? 0L : Math.max(0L, deadline - Util.getMillis());
    }

    static float recovered(ResourceLocation moveId) {
        Long lasts = LASTS.get(moveId);
        if (lasts == null || lasts <= 0L) {
            return 1F;
        }
        return 1F - Math.min(1F, remainingMillis(moveId) / (float) lasts);
    }

    private static void forget() {
        KNOWN_EXPIRY.clear();
        ENDS_AT.clear();
        LASTS.clear();
    }
}
