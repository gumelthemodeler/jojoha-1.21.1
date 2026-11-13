package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TangledVines {
    private static final float GROW_TICKS = 5F;
    private static final float WITHER_TICKS = 8F;

    private static final Map<Integer, long[]> TANGLED = new ConcurrentHashMap<>();

    private TangledVines() {
    }

    public static void heard(int entityId, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        long now = minecraft.level.getGameTime();
        TANGLED.entrySet().removeIf(entry -> entry.getValue()[1] < now);
        TANGLED.put(entityId, new long[] {now, now + ticks});
    }

    public static float growth(int entityId, float now) {
        long[] span = TANGLED.get(entityId);
        if (span == null || now > span[1]) {
            return 0F;
        }
        float in = (now - span[0]) / GROW_TICKS;
        float out = (span[1] - now) / WITHER_TICKS;
        return Mth.clamp(Math.min(in, out), 0F, 1F);
    }

    public static void clear() {
        TANGLED.clear();
    }
}
