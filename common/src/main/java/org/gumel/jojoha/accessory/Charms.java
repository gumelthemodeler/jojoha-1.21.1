package org.gumel.jojoha.accessory;

import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModItems;

public final class Charms {
    public static final int STAR_MIRROR_TICKS = 1;

    public static final float SKULL_SCALE = 1.5F;

    public static final float SHOULDER_PLATE_PERCENT = 5F;

    private Charms() {
    }

    public static int parryWindow(Player player, int ticks) {
        if (ticks <= 0 || !Accessories.wearing(player, ModItems.STAR_MIRROR.get())) {
            return ticks;
        }
        return ticks + JojohaServerConfig.STAR_MIRROR.whole();
    }

    public static boolean freeUtility(Player player, JojohaPlayerData data) {
        return data.standMode.handlesItems() && Accessories.wearing(player, ModItems.CREATIVE_CUBE.get());
    }

    public static float drainScale(Player player) {
        return Accessories.wearing(player, ModItems.FRACTURED_SHOULDER_PLATE.get())
                ? Math.max(0F, 1F - JojohaServerConfig.SHOULDER_PLATE.value() / 100F) : 1F;
    }

    public static boolean warded(Player player) {
        return Accessories.wearing(player, ModItems.WARDING_CHARM.get());
    }

    public static float fortune(Player player) {
        return Accessories.wearing(player, ModItems.SKULL_OF_FORTUNE.get())
                ? JojohaServerConfig.SKULL_OF_FORTUNE.value() : 1F;
    }
}
