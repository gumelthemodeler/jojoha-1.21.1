package org.gumel.jojoha.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class FogPanel {
    private static final int FIELD_W = 128;
    private static final int FIELD_H = 96;

    private static final int OCTAVES = 4;
    private static final float[] DRIFT_X = {0.006F, -0.011F, 0.017F, -0.023F};
    private static final float[] DRIFT_Y = {-0.009F, 0.005F, -0.014F, 0.019F};

    private static final float BASE_FREQUENCY = 3.0F;

    private static final float DENSITY_BASE = 0.38F;
    private static final float DENSITY_FLOOR = 0.26F;
    private static final float DENSITY_GAIN = 2.2F;

    private static final float EDGE = 0.22F;

    private static final int DARK_R = 9, DARK_G = 10, DARK_B = 18;
    private static final int PALE_R = 122, PALE_G = 130, PALE_B = 158;

    private static final int MAX_ALPHA = 238;

    private static DynamicTexture texture;
    private static ResourceLocation id;
    private static long builtForTick = Long.MIN_VALUE;

    private FogPanel() {
    }

    public static void draw(GuiGraphics graphics, int x, int y, int width, int height, float time,
                            float alpha) {
        ResourceLocation sheet = field(time);
        if (sheet == null) {
            return;
        }
        graphics.setColor(1F, 1F, 1F, Mth.clamp(alpha, 0F, 1F));
        graphics.blit(sheet, x, y, width, height, 0F, 0F, FIELD_W, FIELD_H, FIELD_W, FIELD_H);
        graphics.setColor(1F, 1F, 1F, 1F);
    }

    private static ResourceLocation field(float time) {
        long tick = (long) time;
        if (texture != null && builtForTick == tick) {
            return id;
        }

        NativeImage image = new NativeImage(NativeImage.Format.RGBA, FIELD_W, FIELD_H, false);
        paint(image, time);

        if (texture == null) {
            texture = new DynamicTexture(image);
            texture.setFilter(true, false);
            id = Minecraft.getInstance().getTextureManager().register("jojoha_dream_fog", texture);
        } else {
            texture.setPixels(image);
            texture.upload();
        }
        builtForTick = tick;
        return id;
    }

    private static void paint(NativeImage image, float time) {
        for (int py = 0; py < FIELD_H; py++) {
            float v = (py + 0.5F) / FIELD_H;
            for (int px = 0; px < FIELD_W; px++) {
                float u = (px + 0.5F) / FIELD_W;

                float wisp = Mth.clamp((fbm(u, v, time) - DENSITY_FLOOR) * DENSITY_GAIN, 0F, 1F);
                float density = DENSITY_BASE + (1F - DENSITY_BASE) * wisp;
                float edge = falloff(u, v);
                int alpha = Math.round(MAX_ALPHA * density * edge);

                float lift = wisp * wisp;
                int r = Math.round(Mth.lerp(lift, DARK_R, PALE_R));
                int g = Math.round(Mth.lerp(lift, DARK_G, PALE_G));
                int b = Math.round(Mth.lerp(lift, DARK_B, PALE_B));

                image.setPixelRGBA(px, py, (alpha << 24) | (b << 16) | (g << 8) | r);
            }
        }
    }

    private static float falloff(float u, float v) {
        float nearest = Math.min(Math.min(u, 1F - u), Math.min(v, 1F - v));
        return smooth(Mth.clamp(nearest / EDGE, 0F, 1F));
    }

    private static float fbm(float u, float v, float time) {
        float sum = 0F;
        float amplitude = 0.5F;
        float frequency = BASE_FREQUENCY;
        for (int octave = 0; octave < OCTAVES; octave++) {
            sum += amplitude * noise(
                    u * frequency + DRIFT_X[octave] * time,
                    v * frequency + DRIFT_Y[octave] * time,
                    octave);
            amplitude *= 0.5F;
            frequency *= 2F;
        }
        return sum;
    }

    private static float noise(float x, float y, int octave) {
        int x0 = Mth.floor(x);
        int y0 = Mth.floor(y);
        float fx = smooth(x - x0);
        float fy = smooth(y - y0);

        float top = Mth.lerp(fx, hash(x0, y0, octave), hash(x0 + 1, y0, octave));
        float bottom = Mth.lerp(fx, hash(x0, y0 + 1, octave), hash(x0 + 1, y0 + 1, octave));
        return Mth.lerp(fy, top, bottom);
    }

    private static float smooth(float t) {
        return t * t * (3F - 2F * t);
    }

    private static float hash(int x, int y, int octave) {
        int n = x * 374761393 + y * 668265263 + octave * 1013904223;
        n = (n ^ (n >>> 13)) * 1274126177;
        n ^= n >>> 16;
        return (n & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
    }
}
