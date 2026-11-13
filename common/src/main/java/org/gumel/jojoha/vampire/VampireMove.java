package org.gumel.jojoha.vampire;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.VampireStage;

public interface VampireMove {
    ResourceLocation id();

    int cooldownTicks();

    float energyCost();

    default int cooldownTicks(JojohaPlayerData data) {
        return cooldownTicks();
    }

    default float energyCost(JojohaPlayerData data) {
        return energyCost();
    }

    default String unavailable(ServerPlayer player, JojohaPlayerData data) {
        return null;
    }

    default boolean isSustained() {
        return false;
    }

    default boolean isSustainActive(ServerPlayer player) {
        return false;
    }

    default void release(ServerPlayer player, JojohaPlayerData data) {
    }

    default VampireStage minimumStage() {
        return VampireStage.VAMPIRE;
    }

    default boolean pillar() {
        return false;
    }

    void activate(ServerPlayer player, JojohaPlayerData data);
}
