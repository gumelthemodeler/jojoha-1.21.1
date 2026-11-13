package org.gumel.jojoha.combat;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.stand.skill.moves.GroundSlamSkill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SlamState {
    private static final Map<UUID, Float> SLAMMING = new HashMap<>();

    private SlamState() {
    }

    public static void register() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                tick(serverPlayer);
            }
        });
    }

    public static void begin(ServerPlayer player) {
        SLAMMING.put(player.getUUID(), 0F);
    }

    private static void tick(ServerPlayer player) {
        Float lastFall = SLAMMING.get(player.getUUID());
        if (lastFall == null) {
            return;
        }

        if (!player.onGround()) {
            SLAMMING.put(player.getUUID(), player.fallDistance);
            return;
        }

        SLAMMING.remove(player.getUUID());
        GroundSlamSkill.land(player, lastFall);
    }

    public static void forget(ServerPlayer player) {
        SLAMMING.remove(player.getUUID());
    }
}
