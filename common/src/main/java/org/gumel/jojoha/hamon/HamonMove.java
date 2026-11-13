package org.gumel.jojoha.hamon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.data.JojohaPlayerData;

public interface HamonMove {
    ResourceLocation id();

    int cooldownTicks();

    float energyCost();

    boolean activate(ServerPlayer player, JojohaPlayerData data);

    default int chargeMaxTicks() {
        return 0;
    }

    default boolean activate(ServerPlayer player, JojohaPlayerData data, int chargeTicks) {
        return activate(player, data);
    }

    default boolean isSustained() {
        return false;
    }

    default boolean isSustainActive(ServerPlayer player) {
        return false;
    }

    default void release(ServerPlayer player, JojohaPlayerData data) {
    }
}
