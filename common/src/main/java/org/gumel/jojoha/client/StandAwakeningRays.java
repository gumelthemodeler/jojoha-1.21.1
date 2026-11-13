package org.gumel.jojoha.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.item.StandArrowRitual;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StandAwakeningRays {
    public static final int DURATION_TICKS = StandArrowRitual.AWAKEN_RAYS_TICKS;
    private static final int GLOW_FADE_TICKS = StandArrowRitual.AWAKEN_GLOW_FADE_TICKS;
    private static final int TOTAL_TICKS = DURATION_TICKS + GLOW_FADE_TICKS;

    private static final int BURN_FADE_TICKS = 40;
    private static final int BURN_TOTAL_TICKS = DURATION_TICKS + BURN_FADE_TICKS;

    private static final int SHAKE_TICKS = 20;
    private static final float SHAKE_DEGREES = 1.6F;

    private static final Map<UUID, Float> ACTIVE = new HashMap<>();
    private static final Map<UUID, float[]> TINT = new HashMap<>();
    private static final Set<UUID> MASKED = new HashSet<>();

    private static final float[] WHITE_TINT = {1F, 1F, 1F};

    private StandAwakeningRays() {
    }

    public static void begin(UUID playerId, float clientTimeTicks) {
        ACTIVE.put(playerId, clientTimeTicks);
        TINT.put(playerId, WHITE_TINT);
        MASKED.remove(playerId);
        AwakeningLight.burst(playerId);
    }

    public static void beginMask(UUID playerId, float clientTimeTicks, float red, float green, float blue) {
        ACTIVE.put(playerId, clientTimeTicks);
        TINT.put(playerId, new float[] {red, green, blue});
        MASKED.add(playerId);
        AwakeningLight.burst(playerId);
    }

    public static float[] glowTint(UUID playerId) {
        return TINT.getOrDefault(playerId, WHITE_TINT);
    }

    public static boolean masked(UUID playerId) {
        return MASKED.contains(playerId);
    }

    public static float raying(UUID playerId, float clientTimeTicks) {
        Float start = ACTIVE.get(playerId);
        if (start == null) {
            return -1F;
        }
        float elapsed = clientTimeTicks - start;
        return elapsed < 0F || elapsed > DURATION_TICKS ? -1F : elapsed;
    }

    public static void tick(float clientTimeTicks) {
        ACTIVE.entrySet().removeIf(entry -> clientTimeTicks - entry.getValue() > BURN_TOTAL_TICKS);
        MASKED.retainAll(ACTIVE.keySet());
    }

    public static void clear() {
        ACTIVE.clear();
        TINT.clear();
        MASKED.clear();
        AwakeningLight.clear();
    }

    public static float whiteFlash(Player player, float clientTimeTicks) {
        Float start = ACTIVE.get(player.getUUID());
        if (start == null) {
            return 0F;
        }

        float elapsed = clientTimeTicks - start;
        if (elapsed < 0F || elapsed > TOTAL_TICKS) {
            return 0F;
        }
        if (elapsed <= DURATION_TICKS) {
            return 1F;
        }
        return Mth.clamp(1F - (elapsed - DURATION_TICKS) / GLOW_FADE_TICKS, 0F, 1F);
    }

    public static float burn(Player player, float clientTimeTicks) {
        Float start = ACTIVE.get(player.getUUID());
        if (start == null) {
            return 0F;
        }

        float elapsed = clientTimeTicks - start;
        if (elapsed < 0F || elapsed > BURN_TOTAL_TICKS) {
            return 0F;
        }
        if (elapsed <= DURATION_TICKS) {
            return 1F;
        }

        float remaining = Mth.clamp(1F - (elapsed - DURATION_TICKS) / BURN_FADE_TICKS, 0F, 1F);
        return remaining * remaining * remaining * (remaining * (remaining * 6F - 15F) + 10F);
    }

    public static float[] cameraShake(Player player, float clientTimeTicks) {
        Float start = ACTIVE.get(player.getUUID());
        if (start == null) {
            return null;
        }

        float elapsed = clientTimeTicks - start;
        if (elapsed < 0F || elapsed > SHAKE_TICKS) {
            return null;
        }

        float falloff = 1F - (elapsed / SHAKE_TICKS);
        float intensity = falloff * falloff * SHAKE_DEGREES;

        float yaw = (float) Math.sin(elapsed * 2.7F) * intensity;
        float pitch = (float) Math.cos(elapsed * 3.9F) * intensity;
        return new float[] {yaw, pitch};
    }
}
