package org.gumel.jojoha.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class VampireColourOverlay {
    private VampireColourOverlay() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float now = (float) minecraft.level.getGameTime()
                + minecraft.getTimer().getGameTimeDeltaPartialTick(false);

        if (VampireColour.expired(now)) {
            VampireColour.release();
            return;
        }

        float red = VampireColour.red(now);
        if (red <= 0.004F) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int alpha = Mth.clamp(Math.round(red * 255F), 0, 255);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), (alpha << 24) | 0x00B00000);

        RenderSystem.disableBlend();
    }
}
