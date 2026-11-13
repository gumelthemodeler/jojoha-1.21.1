package org.gumel.jojoha.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.stand.StandEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public final class ComboLink {
    public static final int WINDOW_TICKS = 10;
    public static final int HASTE_TICKS = 12;

    private static final Map<UUID, Long> LANDED = new HashMap<>();

    private static ServerPlayer pacing;

    private ComboLink() {
    }

    public static void landed(Player player) {
        if (player instanceof ServerPlayer server) {
            LANDED.put(server.getUUID(), server.level().getGameTime());
        }
    }

    public static boolean linked(ServerPlayer player) {
        Long at = LANDED.get(player.getUUID());
        return at != null && player.level().getGameTime() - at <= WINDOW_TICKS;
    }

    public static boolean link(ServerPlayer player, StandEntity stand, BooleanSupplier move) {
        LANDED.remove(player.getUUID());
        ServerPlayer before = pacing;
        pacing = player;
        boolean fired;
        try {
            fired = move.getAsBoolean();
        } finally {
            pacing = before;
        }
        if (fired) {
            stand.hasten(HASTE_TICKS);
        }
        return fired;
    }

    public static int paced(ServerPlayer player, int ticks) {
        if (pacing == null || pacing != player) {
            return ticks;
        }
        double hastened = HASTE_TICKS * StandEntity.LINK_HASTE;
        if (ticks <= hastened) {
            return Math.max(1, (int) Math.round(ticks / StandEntity.LINK_HASTE));
        }
        return (int) Math.round(ticks - HASTE_TICKS * (StandEntity.LINK_HASTE - 1.0));
    }

    public static void forget(ServerPlayer player) {
        LANDED.remove(player.getUUID());
    }
}
