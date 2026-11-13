package org.gumel.jojoha.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;

public final class ImpactFramePost {
    private static final float CONTRAST = 1.22F;
    private static final float PIVOT = 0.38F;
    private static final float SEPIA_MODE = 3F;

    private static final ScreenPass PASS = new ScreenPass();

    private ImpactFramePost() {
    }

    public static void render() {
        ShaderInstance shader = JojohaShaders.impact();
        if (shader == null) {
            return;
        }

        float sepia = StingFx.sepia();
        float strength = sepia > 0.001F ? sepia : ImpactFrame.strength();
        float mode = sepia > 0.001F ? SEPIA_MODE : ImpactFrame.mode();
        if (strength <= 0.001F) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();

        shader.setSampler("DiffuseSampler", main.getColorTextureId());
        shader.safeGetUniform("Impact").set(strength, CONTRAST, PIVOT, mode);

        PASS.draw(shader);
    }
}
