package org.gumel.jojoha.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.StandCombatHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Exposed {
    public static final int TICKS = 16;

    private static final Map<UUID, Long> UNTIL = new HashMap<>();

    private Exposed() {
    }

    public static void open(ServerPlayer player) {
        long until = player.level().getGameTime() + TICKS;
        UNTIL.put(player.getUUID(), until);
        JojohaPlayerData data = PlayerDataAccess.get(player);
        data.punchCooldownExpiry = Math.max(data.punchCooldownExpiry, until);
        PlayerDataAccess.set(player, data);
        if (data.standGuarding) {
            StandCombatHandler.handleSetGuard(player, false);
        }
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, TICKS, 0, false, false, false));
    }

    public static boolean isOpen(ServerPlayer player) {
        Long until = UNTIL.get(player.getUUID());
        if (until == null) {
            return false;
        }
        if (player.level().getGameTime() >= until) {
            UNTIL.remove(player.getUUID());
            return false;
        }
        return true;
    }
}
