package org.gumel.jojoha.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.gumel.jojoha.combat.fx.ImpactTier;
import org.gumel.jojoha.stand.StandEntity;

public final class ComboCounter {
    private static final long KEEP_MS = 2000L;
    private static final long FADE_MS = 400L;
    private static final long POP_MS = 120L;
    private static final int SHOWN_FROM = 2;
    private static final int RIGHT_OF_CROSSHAIR = 14;
    private static final float POP = 0.35F;

    private static int hits;
    private static long lastAt = Long.MIN_VALUE / 2;

    private ComboCounter() {
    }

    public static void landed(ImpactTier tier, int strikerId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (tier == ImpactTier.TAP || minecraft.player == null || minecraft.level == null || strikerId < 0) {
            return;
        }
        Entity striker = minecraft.level.getEntity(strikerId);
        boolean mine = striker == minecraft.player
                || striker instanceof StandEntity stand && stand.getOwner() == minecraft.player;
        if (!mine) {
            return;
        }
        long now = Util.getMillis();
        hits = now - lastAt > KEEP_MS ? 1 : hits + 1;
        lastAt = now;
    }

    public static void clear() {
        hits = 0;
        lastAt = Long.MIN_VALUE / 2;
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || hits < SHOWN_FROM) {
            return;
        }
        long age = Util.getMillis() - lastAt;
        if (age >= KEEP_MS) {
            return;
        }
        float alpha = Mth.clamp((KEEP_MS - age) / (float) FADE_MS, 0F, 1F);
        float scale = 1F + POP * Mth.clamp(1F - age / (float) POP_MS, 0F, 1F);
        Font font = minecraft.font;
        Component text = Component.translatable("hud.jojoha.combo", hits);
        int x = graphics.guiWidth() / 2 + RIGHT_OF_CROSSHAIR;
        int y = graphics.guiHeight() / 2 - font.lineHeight / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y + font.lineHeight / 2F, 0);
        graphics.pose().scale(scale, scale, 1F);
        graphics.drawString(font, text, 0, -font.lineHeight / 2, Math.round(alpha * 255F) << 24 | 0xFFFFFF, true);
        graphics.pose().popPose();
    }
}
