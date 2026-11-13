package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.heaven.Ascension;
import org.gumel.jojoha.stand.heaven.GreatestHigh;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.passive.HeavenTierPassive;

import java.util.Locale;

public final class HeavenHud {
    private static final int BAR_W = 82;
    private static final int BAR_H = 5;
    private static final int BOTTOM_OFFSET = 80;
    private static final int LABEL_GAP = 2;
    private static final float LABEL_SCALE = 0.75F;

    private static final int BORDER = 0xFF140F1C;
    private static final int TRACK = 0xFF2E2638;
    private static final int GOLD = 0xFFE8C547;
    private static final int PALE = 0xFFFFF4D2;
    private static final int VIOLET = 0xFF7B4FD6;
    private static final int TICK = 0xFF8A7A50;
    private static final int LABEL = 0xF2D68A;
    private static final float FLASH_ALPHA = 0.95F;
    private static final int EDGE_DEPTH = 28;
    private static final float EDGE_ALPHA = 150F;
    private static final int EDGE_VIOLET = 0x7A3CFF;
    private static final int EDGE_GOLD = 0xFFD45E;

    private static float shown;

    private HeavenHud() {
    }

    public static void render(GuiGraphics graphics) {
        JojohaPlayerData data = ClientPlayerDataCache.data;
        Minecraft minecraft = Minecraft.getInstance();
        edges(graphics, data, minecraft);
        flash(graphics);
        SummonFlair.screen(graphics);
        BlindSense.hud(graphics);

        if (data == null || minecraft.player == null || minecraft.player.isSpectator() || !Heaven.is(data)
                || (!data.standSummoned && data.ascension <= 0F)) {
            shown = 0F;
            return;
        }

        float target = Ascension.fraction(data);
        shown = Mth.lerp(0.2F, shown, target);

        int x = (graphics.guiWidth() - BAR_W) / 2;
        int y = graphics.guiHeight() - BOTTOM_OFFSET - CentralBarOverlay.statusLift();

        graphics.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, BORDER);
        graphics.fill(x, y, x + BAR_W, y + BAR_H, TRACK);

        boolean high = data.greatestHighTicks > 0;
        int filled = Math.round(BAR_W * shown);
        if (filled > 0) {
            graphics.fillGradient(x, y, x + filled, y + BAR_H, high ? VIOLET : PALE, high ? VIOLET : GOLD);
        }
        if (target >= 1F) {
            float pulse = 0.5F + 0.5F * Mth.sin((minecraft.player.tickCount + minecraft.getTimer().getGameTimeDeltaPartialTick(false)) * 0.25F);
            int alpha = Math.round(pulse * 120F) << 24;
            graphics.fill(x, y, x + BAR_W, y + BAR_H, alpha | 0xFFFFFF);
        }
        for (HeavenTierPassive tier : new HeavenTierPassive[]{HeavenTierPassive.ASCENDED, HeavenTierPassive.TRANSCENDENT}) {
            int tickX = x + Math.round(BAR_W * tier.threshold() / Ascension.MAX);
            graphics.fill(tickX, y, tickX + 1, y + BAR_H, TICK);
        }

        label(graphics, data, x, y);
    }

    private static void label(GuiGraphics graphics, JojohaPlayerData data, int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        Component text;
        if (data.greatestHighTicks > 0) {
            text = Component.translatable("hud.jojoha.greatest_high",
                    String.format(Locale.ROOT, "%.1f", GreatestHigh.multiplier(data)));
        } else {
            HeavenTierPassive tier = HeavenTierPassive.highest(data.ascension);
            text = tier == null
                    ? Component.translatable("hud.jojoha.ascension", Math.round(data.ascension))
                    : Component.translatable("hud.jojoha.ascension_tier",
                            Component.translatable(tier.translationKey()), Math.round(data.ascension));
        }
        int width = Math.round(minecraft.font.width(text) * LABEL_SCALE);
        float lineH = minecraft.font.lineHeight * LABEL_SCALE;
        graphics.pose().pushPose();
        graphics.pose().translate(x + (BAR_W - width) / 2F, y - LABEL_GAP - lineH, 0F);
        graphics.pose().scale(LABEL_SCALE, LABEL_SCALE, 1F);
        graphics.drawString(minecraft.font, text, 0, 0, LABEL, true);
        graphics.pose().popPose();
    }

    private static void edges(GuiGraphics graphics, JojohaPlayerData data, Minecraft minecraft) {
        float pulse = HeavenFx.pulse();
        if (pulse > 0.01F) {
            edge(graphics, EDGE_GOLD, pulse);
        }
        if (data == null || minecraft.player == null || data.greatestHighTicks <= 0 || !Heaven.is(data)) {
            return;
        }
        float time = minecraft.player.tickCount + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        float climb = Mth.clamp((GreatestHigh.multiplier(data) - GreatestHigh.START)
                / Math.max(0.01F, GreatestHigh.CAP - GreatestHigh.START), 0F, 1F);
        float strength = (0.45F + 0.4F * climb) * (0.85F + 0.15F * Mth.sin(time * 0.3F));
        edge(graphics, EDGE_VIOLET, Math.min(1F, data.greatestHighTicks / 20F) * strength);
    }

    private static void edge(GuiGraphics graphics, int rgb, float strength) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        float glow = Mth.clamp(strength, 0F, 1F);
        int clear = rgb & 0xFFFFFF;
        int full = Math.round(glow * EDGE_ALPHA) << 24 | clear;
        graphics.fillGradient(0, 0, w, EDGE_DEPTH, full, clear);
        graphics.fillGradient(0, h - EDGE_DEPTH, w, h, clear, full);
        for (int i = 0; i < EDGE_DEPTH; i++) {
            float falloff = 1F - i / (float) EDGE_DEPTH;
            int alpha = Math.round(glow * falloff * falloff * EDGE_ALPHA);
            if (alpha <= 0) {
                continue;
            }
            int colour = alpha << 24 | clear;
            graphics.fill(i, 0, i + 1, h, colour);
            graphics.fill(w - i - 1, 0, w - i, h, colour);
        }
    }

    private static void flash(GuiGraphics graphics) {
        float flash = HeavenFx.flash();
        if (flash <= 0.001F) {
            return;
        }
        float peak = HeavenFx.firstPerson() ? FLASH_ALPHA * HeavenFx.FIRST_PERSON_FLASH : FLASH_ALPHA;
        int white = Math.round(Math.min(1F, flash) * peak * 255F) << 24 | 0xFFFFFF;
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), white);
    }
}
