package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.VampireStage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StoneMaskState {
    private static final Map<UUID, Boolean> WORN = new HashMap<>();

    private StoneMaskState() {
    }

    private static final Set<UUID> AJA = new HashSet<>();

    public static void seat(UUID playerId, float clientTimeTicks, boolean aja) {
        WORN.putIfAbsent(playerId, false);
        if (aja) {
            AJA.add(playerId);
        } else {
            AJA.remove(playerId);
        }
    }

    public static boolean ajaOn(Player player) {
        return AJA.contains(player.getUUID());
    }

    public static void activate(UUID playerId, float clientTimeTicks) {
        WORN.put(playerId, true);
        TURNING.put(playerId, clientTimeTicks);
    }

    public static final float TURN_TICKS = 40F;

    private static final Map<UUID, Float> TURNING = new HashMap<>();

    public static float turnProgress(Player player, float clientTimeTicks) {
        Float start = TURNING.get(player.getUUID());
        if (start == null) {
            return activatedOn(player) ? 1F : 0F;
        }
        float linear = Mth.clamp((clientTimeTicks - start) / TURN_TICKS, 0F, 1F);

        return linear * linear;
    }

    public static void remove(UUID playerId) {
        WORN.remove(playerId);
        TURNING.remove(playerId);
        AJA.remove(playerId);
    }

    public static void clear() {
        WORN.clear();
        TURNING.clear();
        AJA.clear();
    }

    public static boolean wornBy(Player player) {
        if (isLocal(player)) {
            return ClientPlayerDataCache.data.stoneMaskWorn;
        }
        return WORN.containsKey(player.getUUID());
    }

    public static boolean activatedOn(Player player) {
        if (isLocal(player)) {
            return ClientPlayerDataCache.data.vampireStage != VampireStage.NONE;
        }
        return Boolean.TRUE.equals(WORN.get(player.getUUID()));
    }

    private static boolean isLocal(Player player) {
        LocalPlayer local = Minecraft.getInstance().player;
        return local != null && local.getUUID().equals(player.getUUID());
    }
}
