package org.gumel.jojoha.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.Palette;
import org.gumel.jojoha.network.packet.BarrageStrugglePacket;
import org.gumel.jojoha.stand.BarrageStruggle;

public final class BarrageStruggleHud {
    private static final ResourceLocation METER = ResourceLocation
            .fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/hud/clash/clash_meter.png");
    private static final int METER_W = 170;
    private static final int METER_H = 11;
    private static final int METER_SCALE = 2;

    private static final ResourceLocation METER_FILLED = ResourceLocation
            .fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/hud/clash/clash_meter_filled.png");
    private static final ResourceLocation BAR = ResourceLocation
            .fromNamespaceAndPath(Jojoha.MOD_ID, "textures/gui/hud/clash/clash_bar.png");
    private static final int BAR_W = 4;
    private static final int BAR_H = 11;
    private static final int CHANNEL_LEFT = 4;
    private static final int CHANNEL_RIGHT = 166;
    private static final int ABOVE_CENTRE = 46;
    private static final float PROMPT_SCALE = 2.0F;
    private static final float RESULT_SCALE = 2.5F;
    private static final long RESULT_MS = 1600L;
    private static final long STALE_MS = 1000L;

    private static boolean active;
    private static float target;
    private static float shown;
    private static long heardAt;
    private static int result;
    private static long resultAt;

    private static final int CLASH_PALETTE_TICKS = 4;

    private static final float BEAT_POP = 0.45F;
    private static final long JUDGED_MS = 450L;
    private static final float JUDGED_SCALE = 1.5F;

    private static int beat;
    private static int presses;
    private static boolean judgedOnBeat;
    private static long judgedAt = Long.MIN_VALUE / 2;

    private BarrageStruggleHud() {
    }

    public static void update(BarrageStrugglePacket packet) {
        long now = Util.getMillis();
        if (!active && packet.active()) {
            shown = packet.mine();
        }
        active = packet.active();
        if (active) {
            PaletteSwap.hold(Palette.CLASH, CLASH_PALETTE_TICKS);
        }
        target = packet.mine();
        heardAt = now;
        beat = packet.beat();
        if (packet.active() && packet.presses() != presses && packet.presses() > 0) {
            judgedOnBeat = packet.onBeat();
            judgedAt = now;
        }
        presses = packet.presses();
        if (!packet.active() && packet.result() != BarrageStrugglePacket.NONE) {
            result = packet.result();
            resultAt = now;
        }
    }

    public static boolean active() {
        if (active && Util.getMillis() - heardAt > STALE_MS) {
            active = false;
        }
        return active;
    }

    public static void render(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        Font font = minecraft.font;
        int centreX = graphics.guiWidth() / 2;
        int ropeY = graphics.guiHeight() / 2 - ABOVE_CENTRE;
        long now = Util.getMillis();

        if (active()) {
            shown = Mth.lerp(0.25F, shown, target);
            drawMeter(graphics, centreX, ropeY);
            float pulse = beatPulse(now);
            int prompt = mix(0xFFE066, 0xFFFFFF, pulse);
            drawScaled(graphics, font, Component.translatable("hud.jojoha.barrage_struggle.mash"), centreX,
                    ropeY - 26, PROMPT_SCALE * (1F + BEAT_POP * pulse * 0.25F), prompt);
            if (now - judgedAt < JUDGED_MS) {
                float left = 1F - (float) (now - judgedAt) / JUDGED_MS;
                int alpha = Math.round(Math.min(1F, left * 2F) * 255F);
                drawScaled(graphics, font, Component.translatable(judgedOnBeat
                                ? "hud.jojoha.barrage_struggle.on_beat" : "hud.jojoha.barrage_struggle.off_beat"),
                        centreX, ropeY + 18, JUDGED_SCALE, alpha << 24 | (judgedOnBeat ? 0xFFE066 : 0xAAAAAA));
            }
            return;
        }

        if (result != BarrageStrugglePacket.NONE && now - resultAt < RESULT_MS) {
            float left = 1F - (float) (now - resultAt) / RESULT_MS;
            int alpha = Math.round(Math.min(1F, left * 3F) * 255F);
            boolean won = result == BarrageStrugglePacket.WON;
            Component text = Component.translatable(won ? "hud.jojoha.barrage_struggle.won"
                    : "hud.jojoha.barrage_struggle.lost");
            drawScaled(graphics, font, text, centreX, ropeY - 12, RESULT_SCALE,
                    alpha << 24 | (won ? 0x77FF77 : 0xFF6655));
        }
    }

    private static float beatPulse(long now) {
        float ticks = beat + (now - heardAt) / 50F;
        float phase = (ticks % BarrageStruggle.BEAT_TICKS) / BarrageStruggle.BEAT_TICKS;
        float near = Math.min(phase, 1F - phase) * 2F;
        return Mth.clamp(1F - near * 2.5F, 0F, 1F);
    }

    private static void drawMeter(GuiGraphics graphics, int centreX, int y) {
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(centreX - METER_W * METER_SCALE / 2F, y - METER_H * METER_SCALE / 2F, 0);
        graphics.pose().scale(METER_SCALE, METER_SCALE, 1F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(METER, 0, 0, 0, 0, METER_W, METER_H, METER_W, METER_H);

        int knot = CHANNEL_LEFT + Math.round((Mth.clamp(shown, -1F, 1F) + 1F) / 2F * (CHANNEL_RIGHT - CHANNEL_LEFT));
        if (knot > 0) {
            graphics.blit(METER_FILLED, 0, 0, 0, 0, knot, METER_H, METER_W, METER_H);
        }
        graphics.blit(BAR, knot - BAR_W / 2, (METER_H - BAR_H) / 2, 0, 0, BAR_W, BAR_H, BAR_W, BAR_H);

        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }

    private static void drawScaled(GuiGraphics graphics, Font font, Component text, int centreX, int y, float scale,
                                   int argb) {
        graphics.pose().pushPose();
        graphics.pose().translate(centreX, y, 0);
        graphics.pose().scale(scale, scale, 1F);
        graphics.drawString(font, text, -font.width(text) / 2, -font.lineHeight / 2, argb, true);
        graphics.pose().popPose();
    }

    private static int mix(int from, int to, float share) {
        int r = Math.round(((from >> 16) & 0xFF) * (1F - share) + ((to >> 16) & 0xFF) * share);
        int g = Math.round(((from >> 8) & 0xFF) * (1F - share) + ((to >> 8) & 0xFF) * share);
        int b = Math.round((from & 0xFF) * (1F - share) + (to & 0xFF) * share);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
