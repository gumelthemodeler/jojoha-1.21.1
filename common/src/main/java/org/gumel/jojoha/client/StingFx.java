package org.gumel.jojoha.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.FightStings;

public final class StingFx {
    private static final ResourceLocation CONTINUED =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/menu/easteregg/to_be_continued.png");

    private static final long DEFEAT_MS = 11000L;
    private static final long DROP_MS = 5000L;
    private static final long SLIDE_MS = 200L;
    private static final int ARROW_W = 128;
    private static final int ARROW_H = 32;
    private static final float ARROW_SHARE = 0.36F;
    private static final int MARGIN = 10;

    private static long defeatAt = -1L;
    private static boolean preview;
    private static SoundInstance defeatSound;

    private StingFx() {
    }

    public static void heard(int kind) {
        if (kind != FightStings.DEFEAT && kind != FightStings.DEFEAT_PREVIEW) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        defeatAt = Util.getMillis();
        preview = kind == FightStings.DEFEAT_PREVIEW;
        stopDefeat(minecraft);
        defeatSound = SimpleSoundInstance.forUI(ModSounds.STING_DEFEAT.get(), 1F, 1F);
        minecraft.getSoundManager().play(defeatSound);
    }

    public static void clear() {
        defeatAt = -1L;
        preview = false;
        stopDefeat(Minecraft.getInstance());
    }

    private static void stopDefeat(Minecraft minecraft) {
        if (defeatSound != null) {
            minecraft.getSoundManager().stop(defeatSound);
            defeatSound = null;
        }
    }

    public static float sepia() {
        long since = sinceStart();
        return since >= DROP_MS - SLIDE_MS ? 1F : 0F;
    }

    private static long sinceStart() {
        if (defeatAt < 0L) {
            return -1L;
        }
        long age = Util.getMillis() - defeatAt;
        if (age >= DEFEAT_MS) {
            defeatAt = -1L;
            preview = false;
            return -1L;
        }
        return age;
    }

    public static void renderHud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (defeatAt >= 0L && !preview && minecraft.player != null && minecraft.player.isAlive()
                && !(minecraft.screen instanceof DeathScreen)) {
            defeatAt = -1L;
            stopDefeat(minecraft);
        }
        if (preview) {
            drawArrow(graphics);
        }
    }

    public static void renderOverDeath(GuiGraphics graphics) {
        if (!preview) {
            drawArrow(graphics);
        }
    }

    private static void drawArrow(GuiGraphics graphics) {
        long shown = sinceStart() - (DROP_MS - SLIDE_MS);
        if (shown < 0L) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int arrowW = Math.max(ARROW_W, Math.round(width * ARROW_SHARE / ARROW_W) * ARROW_W);
        int arrowH = arrowW * ARROW_H / ARROW_W;
        float slide = Mth.clamp(shown / (float) SLIDE_MS, 0F, 1F);
        float ease = 1F - (1F - slide) * (1F - slide);
        int x = Math.round(width - (arrowW + MARGIN) * ease);
        int y = height - arrowH - MARGIN;

        graphics.pose().pushPose();
        graphics.pose().translate(0F, 0F, 500F);
        RenderSystem.enableBlend();
        graphics.blit(CONTINUED, x, y, arrowW, arrowH, 0F, 0F, ARROW_W, ARROW_H, ARROW_W, ARROW_H);
        graphics.pose().popPose();
    }
}
