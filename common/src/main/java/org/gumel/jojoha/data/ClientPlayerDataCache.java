package org.gumel.jojoha.data;

public final class ClientPlayerDataCache {
    private ClientPlayerDataCache() {
    }

    public static volatile JojohaPlayerData data = JojohaPlayerData.createDefault();

    private static JojohaPlayerData blank = data;

    public static boolean synced() {
        return data != blank;
    }

    public static void forget() {
        if (data != blank) {
            blank = JojohaPlayerData.createDefault();
            data = blank;
        }
    }
}
