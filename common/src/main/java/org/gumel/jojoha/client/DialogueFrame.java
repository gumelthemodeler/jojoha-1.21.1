package org.gumel.jojoha.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

final class DialogueFrame {
    private static final ResourceLocation FRAME = PlayerMenuScreen.menu("frames/npc_dialogue_screen.png");
    private static final int TEX = 64;
    private static final int BORDER = 6;

    private DialogueFrame() {
    }

    static void draw(GuiGraphics graphics, int x, int y, int w, int h) {
        int b = Math.min(BORDER, Math.min(w, h) / 2);
        int inner = TEX - BORDER * 2;
        int far = TEX - BORDER;
        int iw = Math.max(0, w - b * 2);
        int ih = Math.max(0, h - b * 2);
        graphics.blit(FRAME, x, y, b, b, 0F, 0F, BORDER, BORDER, TEX, TEX);
        graphics.blit(FRAME, x + w - b, y, b, b, far, 0F, BORDER, BORDER, TEX, TEX);
        graphics.blit(FRAME, x, y + h - b, b, b, 0F, far, BORDER, BORDER, TEX, TEX);
        graphics.blit(FRAME, x + w - b, y + h - b, b, b, far, far, BORDER, BORDER, TEX, TEX);
        graphics.blit(FRAME, x + b, y, iw, b, BORDER, 0F, inner, BORDER, TEX, TEX);
        graphics.blit(FRAME, x + b, y + h - b, iw, b, BORDER, far, inner, BORDER, TEX, TEX);
        graphics.blit(FRAME, x, y + b, b, ih, 0F, BORDER, BORDER, inner, TEX, TEX);
        graphics.blit(FRAME, x + w - b, y + b, b, ih, far, BORDER, BORDER, inner, TEX, TEX);
        graphics.blit(FRAME, x + b, y + b, iw, ih, BORDER, BORDER, inner, inner, TEX, TEX);
    }
}
