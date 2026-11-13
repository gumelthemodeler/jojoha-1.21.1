package org.gumel.jojoha.client;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.gumel.jojoha.combat.fx.ImpactTier;

public final class ScreenLanguage {
    private static final double NEAR = 6.0;
    private static final long FINISHER_EDGE_MS = 420L;
    private static final long HEAVY_EDGE_MS = 240L;
    private static final long LINES_EXTRA_MS = 160L;
    private static final int BANDS = 3;
    private static final int BAND_DEPTH = 5;
    private static final int[] BAND_ALPHA = {0x70, 0x48, 0x24};
    private static final int LINES = 30;
    private static final float LINE_REACH_MIN = 0.16F;
    private static final float LINE_REACH_MAX = 0.32F;
    private static final int LINE_STEP = 2;
    private static final int LINE_COLOUR = 0xFFFFFF;

    private static long edgeStart;
    private static long edgeMs;
    private static float edgeStrength;
    private static int edgeColour;
    private static long linesUntil;

    private ScreenLanguage() {
    }

    public static void landed(ImpactTier tier, int colour, boolean involved, double distance) {
        long now = Util.getMillis();
        boolean near = involved || distance <= NEAR;
        if (tier == ImpactTier.FINISHER && near) {
            edge(now, FINISHER_EDGE_MS, involved ? 1F : 0.6F, colour);
            linesUntil = Math.max(linesUntil, now + tier.hitStopMs() + LINES_EXTRA_MS);
        } else if ((tier == ImpactTier.HEAVY || tier == ImpactTier.GUARD_BREAK) && involved) {
            edge(now, HEAVY_EDGE_MS, 0.6F, colour);
        }
    }

    private static void edge(long now, long ms, float strength, int colour) {
        if (now - edgeStart < edgeMs && edgeStrength > strength) {
            return;
        }
        edgeStart = now;
        edgeMs = ms;
        edgeStrength = strength;
        edgeColour = colour & 0xFFFFFF;
    }

    public static void clear() {
        edgeMs = 0L;
        linesUntil = 0L;
    }

    public static void render(GuiGraphics graphics) {
        long now = Util.getMillis();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        long age = now - edgeStart;
        if (age < edgeMs) {
            float left = 1F - (float) (age / 50L) / (edgeMs / 50L);
            float strength = edgeStrength * Mth.clamp(left, 0F, 1F);
            for (int band = 0; band < BANDS; band++) {
                int alpha = Math.round(BAND_ALPHA[band] * strength);
                if (alpha <= 0) {
                    continue;
                }
                int colour = alpha << 24 | edgeColour;
                int in = band * BAND_DEPTH;
                int out = in + BAND_DEPTH;
                graphics.fill(in, in, width - in, out, colour);
                graphics.fill(in, height - out, width - in, height - in, colour);
                graphics.fill(in, out, out, height - out, colour);
                graphics.fill(width - out, out, width - in, height - out, colour);
            }
        }

        if (now < linesUntil) {
            lines(graphics, width, height, now / 50L);
        }
    }

    private static void lines(GuiGraphics graphics, int width, int height, long frame) {
        RandomSource random = RandomSource.create(frame * 7919L);
        float cx = width / 2F;
        float cy = height / 2F;
        float half = Math.max(width, height) * 0.75F;
        for (int i = 0; i < LINES; i++) {
            double angle = Mth.TWO_PI * (i + random.nextFloat() * 0.8) / LINES;
            float dx = (float) Math.cos(angle);
            float dy = (float) Math.sin(angle);
            float scale = Math.min(Math.abs(dx) < 1.0E-4F ? Float.MAX_VALUE : cx / Math.abs(dx),
                    Math.abs(dy) < 1.0E-4F ? Float.MAX_VALUE : cy / Math.abs(dy));
            float reach = half * (LINE_REACH_MIN + random.nextFloat() * (LINE_REACH_MAX - LINE_REACH_MIN));
            int alpha = 0x90 + random.nextInt(0x50);
            int colour = alpha << 24 | LINE_COLOUR;
            for (float d = 0F; d < reach; d += LINE_STEP) {
                float x = cx + dx * (scale - d);
                float y = cy + dy * (scale - d);
                int thick = d < reach * 0.45F ? 2 : 1;
                int px = Math.round(x) - thick / 2;
                int py = Math.round(y) - thick / 2;
                graphics.fill(px, py, px + thick, py + thick, colour);
            }
        }
    }
}
