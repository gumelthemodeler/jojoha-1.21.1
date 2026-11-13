package org.gumel.jojoha.client;

import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import org.gumel.jojoha.registry.ModBlocks;

public final class SandShade {
    public static final float LIFT = 1.0625F;
    private static final float NEUTRAL = 1.0F / LIFT;
    private static final float[] LIGHT = {1.0F, 1.0F, 1.0F};
    private static final float[] DARK = {0.82F, 0.77F, 0.72F};
    private static final double PATCH_BLOCKS = 40.0;
    private static final double DETAIL_BLOCKS = 13.0;
    private static final double DETAIL_WEIGHT = 0.35;
    private static final double SHARPEN = 1.6;
    private static final long SEED = 0x5A4D5D5L;

    private static final SimplexNoise PATCHES = new SimplexNoise(RandomSource.create(SEED));
    private static final SimplexNoise DETAIL = new SimplexNoise(RandomSource.create(SEED + 1));

    private SandShade() {
    }

    public static void register() {
        ColorHandlerRegistry.registerBlockColors(
                (state, view, pos, tint) -> pos == null ? neutral() : color(pos),
                ModBlocks.CRUSADER_SAND.get());
    }

    private static int neutral() {
        int channel = Math.round(NEUTRAL * 255.0F);
        return channel << 16 | channel << 8 | channel;
    }

    public static float shade(BlockPos pos) {
        double patch = PATCHES.getValue(pos.getX() / PATCH_BLOCKS, pos.getZ() / PATCH_BLOCKS);
        double detail = DETAIL.getValue(pos.getX() / DETAIL_BLOCKS, pos.getZ() / DETAIL_BLOCKS);
        double raw = (patch + detail * DETAIL_WEIGHT) / (1.0 + DETAIL_WEIGHT);
        return (float) Mth.clamp(raw * SHARPEN * JojohaClientConfig.SAND_SHADE.value(), -1.0, 1.0);
    }

    private static int color(BlockPos pos) {
        float shade = shade(pos);
        float[] toward = shade >= 0.0F ? LIGHT : DARK;
        float amount = Math.abs(shade);
        int color = 0;
        for (int channel = 0; channel < 3; channel++) {
            float value = Mth.lerp(amount, NEUTRAL, toward[channel]);
            color = color << 8 | Mth.clamp(Math.round(value * 255.0F), 0, 255);
        }
        return color;
    }
}
