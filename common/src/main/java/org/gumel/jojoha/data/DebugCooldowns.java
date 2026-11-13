package org.gumel.jojoha.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.combat.Defence;

public final class DebugCooldowns {
    private static boolean none;

    private DebugCooldowns() {
    }

    public static void setNone(boolean value) {
        none = value;
    }

    public static boolean none() {
        return none;
    }

    static boolean kept(ResourceLocation id) {
        return Defence.PARRY_PERFECT.equals(id);
    }

    public static int reset(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        long now = player.level().getGameTime();
        int cleared = (int) data.moveCooldowns.values().stream().filter(expiry -> expiry > now).count();
        data.moveCooldowns.clear();
        Defence.forgetParryWait(player);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        return cleared;
    }
}
