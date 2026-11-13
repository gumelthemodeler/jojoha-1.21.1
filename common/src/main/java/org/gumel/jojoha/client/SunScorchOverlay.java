package org.gumel.jojoha.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.VampireStage;

public final class SunScorchOverlay {
    private static final float MAX_DEPTH = 0.26F;

    private static final float MAX_ALPHA = 0.82F;

    private static final int BANDS = 24;

    private static final int RED = 0xFF;
    private static final int GREEN = 0xC4;
    private static final int BLUE = 0x2A;

    private static final int STONE_RED = 0x8E;
    private static final int STONE_GREEN = 0x8A;
    private static final int STONE_BLUE = 0x84;

    private static final float FLOOR = 0.01F;

    private SunScorchOverlay() {
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        float exposure = SunScorch.exposure();
        if (exposure <= FLOOR) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        float weight = exposure * exposure;

        int depth = Math.max(1, Math.round(Math.min(width, height) * MAX_DEPTH * exposure));
        int step = Math.max(1, depth / BANDS);

        boolean stone = ClientPlayerDataCache.data.vampireStage
                == VampireStage.PILLAR_MAN;
        int tint = stone ? (STONE_RED << 16) | (STONE_GREEN << 8) | STONE_BLUE : (RED << 16) | (GREEN << 8) | BLUE;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int inset = 0; inset < depth; inset += step) {
            float along = 1F - (float) inset / depth;
            int alpha = Mth.clamp(Math.round(along * along * MAX_ALPHA * weight * 255F), 0, 255);
            if (alpha <= 0) {
                continue;
            }
            int colour = (alpha << 24) | tint;

            int near = inset;
            int far = inset + step;

            graphics.fill(0, near, width, far, colour);
            graphics.fill(0, height - far, width, height - near, colour);
            graphics.fill(near, far, far, height - far, colour);
            graphics.fill(width - far, far, width - near, height - far, colour);
        }

        RenderSystem.disableBlend();
    }
}
