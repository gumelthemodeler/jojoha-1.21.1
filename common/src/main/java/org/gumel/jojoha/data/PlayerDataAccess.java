package org.gumel.jojoha.data;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.network.NetworkHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerDataAccess {
    public static final int SOON_TICKS = 4;

    private static final Set<UUID> PENDING = new HashSet<>();

    private PlayerDataAccess() {
    }

    @ExpectPlatform
    public static JojohaPlayerData get(Player player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void set(Player player, JojohaPlayerData data) {
        throw new AssertionError();
    }

    public static void sync(ServerPlayer player) {
        PENDING.remove(player.getUUID());
        NetworkHandler.sendPlayerData(player, get(player));
    }

    public static void syncSoon(ServerPlayer player) {
        PENDING.add(player.getUUID());
    }

    public static void flushSoon(ServerPlayer player) {
        if (player.tickCount % SOON_TICKS == 0 && PENDING.contains(player.getUUID())) {
            sync(player);
        }
    }

    public static void forget(UUID player) {
        PENDING.remove(player);
    }
}
