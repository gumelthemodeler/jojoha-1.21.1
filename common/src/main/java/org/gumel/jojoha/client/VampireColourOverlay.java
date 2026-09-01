package org.gumel.jojoha.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * The red half of the turn's colour grade.
 *
 * <p>A single quad over the whole screen. The grey it crosses with is done in the shaders, because
 * desaturating is something only they can do; tinting is not, so this is the cheap half and there
 * was no reason to make it the expensive one - see {@link VampireColour}.
 *
 * <p>Drawn after the rest of the HUD so it washes the interface too. A grade that stopped at the
 * edge of the world and left the hotbar its normal colour would read as a filter laid over the game
 * rather than as something happening to the person playing it.
 */
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

        // The backstop, not the ending. The ritual releases the grade itself; this only catches a
        // client that never heard it finish.
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
