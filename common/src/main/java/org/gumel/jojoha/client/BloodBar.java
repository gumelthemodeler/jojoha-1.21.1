package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.VampireStage;

/**
 * The hunger row, for something that does not eat.
 *
 * <p>Same ten slots in the same places as vanilla's, because the row has to keep lining up with the
 * armour above it and the hearts across from it - this is a change of icon, not of layout. The
 * geometry below is vanilla's own: nine pixels a slot, right to left, eight apart.
 */
public final class BloodBar {
    private static ResourceLocation hud(String name) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/hud/" + name);
    }

    private static final ResourceLocation FULL = hud("blood_icon.png");
    private static final ResourceLocation EMPTY = hud("blood_icon_empty.png");

    /** Vanilla's hunger layout: ten slots, nine pixels square, eight apart, laid right to left. */
    private static final int SLOTS = 10;
    private static final int ICON = 9;
    private static final int PITCH = 8;

    private BloodBar() {
    }

    /**
     * Draws the row if this player is a vampire.
     *
     * @return true if it drew, meaning vanilla's own row should not
     */
    public static boolean render(GuiGraphics graphics, Player player, int y, int x) {
        if (ClientPlayerDataCache.data.vampireStage == VampireStage.NONE) {
            return false;
        }

        int level = player.getFoodData().getFoodLevel();

        for (int slot = 0; slot < SLOTS; slot++) {
            int slotX = x - slot * PITCH - ICON;

            // Two icons for a value that moves in halves, so a slot is drawn full only once it is
            // genuinely full. Rounding up instead would show a full slot for a half-empty one,
            // which on a meter you are meant to watch draining is the wrong way to be wrong.
            boolean filled = level >= (slot + 1) * 2;
            graphics.blit(filled ? FULL : EMPTY, slotX, y, 0, 0, ICON, ICON, ICON, ICON);
        }

        return true;
    }
}
