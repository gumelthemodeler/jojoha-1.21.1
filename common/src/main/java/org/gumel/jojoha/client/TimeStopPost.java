package org.gumel.jojoha.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class TimeStopPost {
    private static final float SKY_DISTANCE = 512F;

    private static final ScreenPass PASS = new ScreenPass();

    private TimeStopPost() {
    }

    public static void render(Matrix4f modelView, Matrix4f projection, Camera camera,
                              float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        ShaderInstance shader = JojohaShaders.timeStop();

        if (level == null || shader == null || modelView == null || projection == null) {
            return;
        }

        float strength = TimeStopShader.strength();
        float globalDrain = TimeStopShader.globalDrain();
        if (strength <= 0.001F && globalDrain <= 0.001F) {
            return;
        }

        RenderTarget main = minecraft.getMainRenderTarget();

        Vec3 cameraPos = camera.getPosition();

        Matrix4f forward = new Matrix4f(projection).mul(modelView);
        Matrix4f inverse = new Matrix4f(forward).invert();
        Vec3 centre = TimeStopView.centre().subtract(cameraPos);

        shader.setSampler("DiffuseSampler", main.getColorTextureId());
        shader.setSampler("DepthSampler", main.getDepthTextureId());

        shader.safeGetUniform("InverseTransform").set(inverse);
        shader.safeGetUniform("ForwardTransform").set(forward);
        shader.safeGetUniform("SphereCentre").set(
                (float) centre.x, (float) centre.y, (float) centre.z);

        shader.safeGetUniform("Field").set(TimeStopShader.greyRadius(), TimeStopShader.greyStrength(),
                TimeStopShader.skyShare(), TimeStopShader.ringWidth());
        shader.safeGetUniform("Inner").set(TimeStopShader.frontRadius(),
                TimeStopShader.waveStrength(), TimeStopShader.hueRoll(),
                TimeStopShader.huePhase());
        shader.safeGetUniform("Grade").set(TimeStopShader.desaturation(), TimeStopShader.darken(),
                TimeStopShader.ringStrength(), globalDrain);
        shader.safeGetUniform("Lens").set(TimeStopShader.warpPush(), TimeStopShader.wakeBlocks(),
                TimeStopShader.warpWidth(), TimeStopShader.releaseStrength());
        shader.safeGetUniform("Shake").set(TimeStopShader.squiggle(), TimeStopShader.waveScale(),
                TimeStopShader.wavePhase(), 0F);
        shader.safeGetUniform("SkyDistance").set(SKY_DISTANCE);

        boolean masked = strength > 0.001F && TimeStopMask.capture(modelView, projection, camera, partialTick);
        shader.setSampler("FreeMaskSampler", TimeStopMask.colourTexture());
        shader.setSampler("FreeDepthSampler", TimeStopMask.depthTexture());
        shader.safeGetUniform("FreeMask").set(masked ? 1F : 0F);

        PASS.draw(shader);
    }
}
