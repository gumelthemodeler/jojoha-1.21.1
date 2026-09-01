package org.gumel.jojoha.hamon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.data.JojohaPlayerData;

/** A single Hamon move granted by a path, triggered from the client via a keybind. */
public interface HamonMove {
    ResourceLocation id();

    int cooldownTicks();

    /** Spec energy consumed on activation - checked and deducted by {@link org.gumel.jojoha.hamon.HamonMoves} before {@link #activate}. */
    float energyCost();

    void activate(ServerPlayer player, JojohaPlayerData data);
}
