package org.gumel.jojoha.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;

public final class TimeStopChargeBar {
    private static ResourceLocation hud(String name) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/hud/" + name);
    }

    private static final ResourceLocation FRAME = hud("timestop/timestop_charge_bar.png");
    private static final ResourceLocation FILL = hud("timestop/timestop_charge_bar_overlay.png");

    private static final int TEX_W = 192, TEX_H = 22;

    private static final int LEFT_BANK_END = 85;
    private static final int RIGHT_BANK_START = 106;
    private static final int BANK_W = 82;

    private static final int SLICE_W = 2;

    private static final float HUE_SPEED = 0.32F;
    private static final float HUE_SPREAD = 0.7F;

    private static final float SATURATION = 0.82F;

    private static final int BOTTOM_OFFSET = 68;

    private static final int LABEL_GAP = 4;
    private static final int LABEL_RGB = 0xF2D68A;

    private static final float LABEL_MIN_ALPHA = 0.06F;

    private TimeStopChargeBar() {
    }

    public static void render(GuiGraphics graphics) {
        TimeStopChargers.render(graphics, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        float alpha;
        float progress;

        if (TimeStopCharge.charging()) {
            alpha = 1F;
            progress = TimeStopCharge.charge();
        } else if (TimeStopCharge.dispelling()) {
            alpha = TimeStopCharge.dispelFade();
            progress = TimeStopCharge.releasedCharge();
        } else {
            return;
        }

        int x = (graphics.guiWidth() - TEX_W) / 2;
        int y = graphics.guiHeight() - BOTTOM_OFFSET - CentralBarOverlay.statusLift();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.setColor(1F, 1F, 1F, alpha);
        graphics.blit(FRAME, x, y, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);

        int filled = Math.round(BANK_W * Mth.clamp(progress, 0F, 1F));
        if (filled > 0) {
            float seconds = (Util.getMillis() % 1000000L) / 1000F;
            bank(graphics, x, y, filled, seconds, alpha, false);
            bank(graphics, x, y, filled, seconds, alpha, true);
        }

        graphics.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();

        label(graphics, x, y, alpha);
    }

    private static void label(GuiGraphics graphics, int x, int y, float alpha) {
        if (alpha < LABEL_MIN_ALPHA || TimeStopCharge.stopTicks() <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Component text = Component.literal(String.format("%.1fs", TimeStopCharge.stopTicks() / 20F));
        int width = minecraft.font.width(text);

        int argb = (Mth.clamp(Math.round(alpha * 255F), 0, 255) << 24) | LABEL_RGB;
        graphics.drawString(minecraft.font, text,
                x + TEX_W / 2 - width / 2, y - minecraft.font.lineHeight - LABEL_GAP, argb, true);
    }

    private static void bank(GuiGraphics graphics, int x, int y, int filled, float seconds,
                             float alpha, boolean rightward) {
        for (int done = 0; done < filled; done += SLICE_W) {
            int width = Math.min(SLICE_W, filled - done);

            int u = rightward
                    ? RIGHT_BANK_START + done
                    : LEFT_BANK_END + 1 - done - width;

            float outward = (done + width * 0.5F) / BANK_W;
            float hue = seconds * HUE_SPEED + outward * HUE_SPREAD;
            int rgb = Mth.hsvToRgb(hue - Mth.floor(hue), SATURATION, 1F);

            graphics.setColor(
                    ((rgb >> 16) & 0xFF) / 255F,
                    ((rgb >> 8) & 0xFF) / 255F,
                    (rgb & 0xFF) / 255F,
                    alpha);
            graphics.blit(FILL, x + u, y, u, 0, width, TEX_H, TEX_W, TEX_H);
        }

        graphics.setColor(1F, 1F, 1F, 1F);
    }
}
