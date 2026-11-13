package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;

public final class SkullCrushOverlay {
    private static final float FLASH_ALPHA = 0.78F;

    private SkullCrushOverlay() {
    }

    public static void render(GuiGraphics guiGraphics, int width, int height, float partialTick) {
        impactFrame(guiGraphics, width, height, partialTick);
    }

    private static void impactFrame(GuiGraphics guiGraphics, int width, int height,
                                    float partialTick) {
        float flash = SkullFlashFx.flash(partialTick);
        if (flash <= 0.001F) {
            return;
        }

        int white = Math.round(Math.min(1F, flash) * FLASH_ALPHA * 255F) << 24 | 0xFFFFFF;
        guiGraphics.fill(0, 0, width, height, white);
    }

}
