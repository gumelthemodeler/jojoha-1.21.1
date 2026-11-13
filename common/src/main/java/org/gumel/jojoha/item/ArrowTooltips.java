package org.gumel.jojoha.item;

import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public final class ArrowTooltips {
    private static Supplier<Player> viewer = () -> null;

    private ArrowTooltips() {
    }

    public static void setViewer(Supplier<Player> supplier) {
        viewer = supplier;
    }

    public static Player viewer() {
        return viewer.get();
    }
}
