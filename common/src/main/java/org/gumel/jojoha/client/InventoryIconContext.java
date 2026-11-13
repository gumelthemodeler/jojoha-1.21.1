package org.gumel.jojoha.client;

public final class InventoryIconContext {
    private static boolean inSlot;

    private InventoryIconContext() {
    }

    public static boolean push(boolean nowInSlot) {
        boolean previous = inSlot;
        inSlot = nowInSlot;
        return previous;
    }

    public static void restore(boolean previous) {
        inSlot = previous;
    }

    public static boolean inSlot() {
        return inSlot;
    }
}
