package org.gumel.jojoha.stand.grapple;

import dev.architectury.event.events.common.TickEvent;
import dev.architectury.utils.GameInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class GrappleGrace {
    private static final int GRACE_TICKS = 40;

    private static final Map<UUID, Integer> GRACE = new HashMap<>();

    private GrappleGrace() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> tick());
    }

    public static void grant(Player player) {
        if (player instanceof ServerPlayer holder) {
            GRACE.put(holder.getUUID(), GRACE_TICKS);
            holder.resetFallDistance();
        }
    }

    private static void tick() {
        if (GRACE.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Integer>> entries = GRACE.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<UUID, Integer> entry = entries.next();

            int left = entry.getValue() - 1;
            if (left <= 0) {
                entries.remove();
                continue;
            }
            entry.setValue(left);

            ServerPlayer player = player(entry.getKey());
            if (player == null) {
                entries.remove();
                continue;
            }

            player.resetFallDistance();
        }
    }

    private static ServerPlayer player(UUID id) {
        MinecraftServer server =
                MinecraftServer.class.cast(
                        GameInstance.getServer());
        return server == null ? null : server.getPlayerList().getPlayer(id);
    }
}
