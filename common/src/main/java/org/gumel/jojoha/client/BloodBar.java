package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.VampireStage;

public final class BloodBar {
    private static ResourceLocation hud(String name) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/hud/" + name);
    }

    private static final ResourceLocation FULL = hud("status/blood_icon.png");
    private static final ResourceLocation EMPTY = hud("status/blood_icon_empty.png");

    private static final int SLOTS = 10;
    private static final int ICON = 9;
    private static final int PITCH = 8;

    private BloodBar() {
    }

    public static boolean render(GuiGraphics graphics, Player player, int y, int x) {
        if (ClientPlayerDataCache.data.vampireStage == VampireStage.NONE) {
            return false;
        }

        int level = player.getFoodData().getFoodLevel();

        for (int slot = 0; slot < SLOTS; slot++) {
            int slotX = x - slot * PITCH - ICON;

            boolean filled = level >= (slot + 1) * 2;
            graphics.blit(filled ? FULL : EMPTY, slotX, y, 0, 0, ICON, ICON, ICON, ICON);
        }

        return true;
    }
}
