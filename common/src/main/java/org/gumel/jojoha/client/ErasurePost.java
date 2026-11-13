package org.gumel.jojoha.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class ErasurePost {
    private static final float INVERSION = 0.92F;
    private static final float SKY_DEPTH = 0.99995F;

    private static final ScreenPass PASS = new ScreenPass();

    private ErasurePost() {
    }

    public static void render(Matrix4f modelView, Matrix4f projection, Camera camera) {
        ShaderInstance shader = JojohaShaders.erasure();
        if (shader == null) {
            return;
        }
        float realm = HeavenFx.realm();

        float strength = HeavenFx.erasure();
        float sepia = HeavenFx.sepia();
        float gold = HeavenFx.gold();
        float rewrite = HeavenFx.rewrite();
        float shock = HeavenFx.shock();
        if (strength <= 0.001F && sepia <= 0.001F && gold <= 0.001F && rewrite <= 0.001F && shock >= 1F
                && realm <= 0.001F) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();

        shader.setSampler("DiffuseSampler", main.getColorTextureId());
        shader.setSampler("DepthSampler", main.getDepthTextureId());
        shader.safeGetUniform("Erasure").set(strength, INVERSION, SKY_DEPTH, 0F);
        shader.safeGetUniform("Rewrite").set(rewrite, shock, main.width / (float) Math.max(1, main.height), 0F);
        float seconds = (System.nanoTime() / 1_000_000L % 100_000L) / 1000F;
        shader.safeGetUniform("Tone").set(sepia, gold, seconds, 0F);
        Vec3 eye = camera.getPosition();
        Matrix4f inverse = new Matrix4f(projection).mul(modelView).invert();
        shader.safeGetUniform("InverseTransform").set(inverse);
        Vec3 centre = HeavenFx.realmCentre().subtract(eye);
        shader.safeGetUniform("Realm").set(realm, HeavenFx.realmFront(), seconds, SKY_DEPTH);
        shader.safeGetUniform("RealmCentre").set((float) centre.x, (float) centre.y, (float) centre.z, 0F);
        shader.safeGetUniform("RealmGrid").set((float) (eye.x - Math.floor(eye.x)), (float) (eye.y - Math.floor(eye.y)),
                (float) (eye.z - Math.floor(eye.z)), 0F);

        PASS.draw(shader);
    }
}
