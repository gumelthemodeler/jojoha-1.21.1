package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.skill.Inflicts;

import java.util.List;

public final class StatusPips {
    private static final ResourceLocation TAG =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/menu/tooltip/tag_icon.png");
    private static final int TEX = 8;
    private static final int GEM_U = 3, GEM_V = 2;
    private static final int GEM = 4;

    private StatusPips() {
    }

    public static void draw(GuiGraphics guiGraphics, ResourceLocation move, int right, int bottom) {
        List<Inflicts.Status> statuses = Inflicts.of(move);
        int x = right;
        for (int i = statuses.size() - 1; i >= 0; i--) {
            Inflicts.Status status = statuses.get(i);
            int colour = status.colour();
            guiGraphics.setColor((colour >> 16 & 0xFF) / 255F, (colour >> 8 & 0xFF) / 255F, (colour & 0xFF) / 255F, 1F);
            for (int d = 0; d < status.pips(); d++) {
                x -= GEM;
                guiGraphics.blit(TAG, x, bottom - GEM, GEM_U, GEM_V, GEM, GEM, TEX, TEX);
            }
            guiGraphics.setColor(1F, 1F, 1F, 1F);
            x -= 1;
        }
    }
}
