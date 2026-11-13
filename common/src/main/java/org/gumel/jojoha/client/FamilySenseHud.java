package org.gumel.jojoha.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;

public final class FamilySenseHud {
    private static final ResourceLocation STAR =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/hud/status/family_indicator.png");
    private static final int SIZE = 17;
    private static final int MARGIN = 6;
    private static final float RANGE = 100F;
    private static final float SLOWEST_PULSE = 40F;
    private static final float FASTEST_PULSE = 8F;
    private static final float DIMMEST = 0.3F;
    private static final float FADE_PER_TICK = 0.1F;

    private static int distance = -1;
    private static float shown;
    private static float lastTick = -1F;

    private FamilySenseHud() {
    }

    public static void sensed(int blocks) {
        distance = blocks;
    }

    public static void clear() {
        distance = -1;
        shown = 0F;
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        float now = minecraft.level.getGameTime() + partialTick;
        float step = lastTick < 0F ? 0F : Mth.clamp(now - lastTick, 0F, 5F);
        lastTick = now;
        shown = Mth.clamp(shown + (distance >= 0 ? step : -step) * FADE_PER_TICK, 0F, 1F);
        if (shown <= 0F) {
            return;
        }

        float closeness = distance < 0 ? 0F : 1F - Mth.clamp(distance / RANGE, 0F, 1F);
        float period = Mth.lerp(closeness, SLOWEST_PULSE, FASTEST_PULSE);
        float wave = 0.5F + 0.5F * Mth.sin(now / period * Mth.TWO_PI);
        float alpha = shown * Mth.lerp(wave, DIMMEST, 1F);

        RenderSystem.enableBlend();
        graphics.setColor(1F, 1F, 1F, alpha);
        graphics.blit(STAR, graphics.guiWidth() - MARGIN - SIZE, graphics.guiHeight() - MARGIN - SIZE,
                0, 0, SIZE, SIZE, SIZE, SIZE);
        graphics.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }
}
