package org.gumel.jojoha.data;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.network.NetworkHandler;

/**
 * Cross-platform access to a player's {@link JojohaPlayerData}, backed by each platform's
 * native attachment API (Fabric's Data Attachment API / NeoForge's AttachmentType). Storage
 * and persistence are platform-specific; client sync is handled uniformly here via
 * {@link NetworkHandler}, so callers never need to think about the platform split.
 */
public final class PlayerDataAccess {
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

    /**
     * Pushes the player's current data to their client. Call after any server-side mutation
     * that the client needs to know about.
     */
    public static void sync(ServerPlayer player) {
        NetworkHandler.sendPlayerData(player, get(player));
    }
}
