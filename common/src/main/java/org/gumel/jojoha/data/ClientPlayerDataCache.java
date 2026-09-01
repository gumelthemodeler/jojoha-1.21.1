package org.gumel.jojoha.data;

/**
 * Client-side mirror of the local player's {@link JojohaPlayerData}, kept up to date by
 * {@code SyncPlayerDataPacket}. This is a plain data holder (no client-only Minecraft
 * classes), so it's safe to sit in common code and simply goes unused on a dedicated server.
 */
public final class ClientPlayerDataCache {
    private ClientPlayerDataCache() {
    }

    public static volatile JojohaPlayerData data = JojohaPlayerData.createDefault();
}
