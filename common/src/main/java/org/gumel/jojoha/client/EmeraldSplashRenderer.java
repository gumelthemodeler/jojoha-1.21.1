package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.hierophant.EmeraldSplash;
import org.joml.Matrix4f;

public final class EmeraldSplashRenderer extends EntityRenderer<EmeraldSplash> {
    public static final ResourceLocation SPRITE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/emerald_sprite.png");

    private static final float SIZE = 0.4F;
    private static final float FRAGMENT_SCALE = 0.55F;

    private static final float SPIN = 38F;

    private static final float GLOW_SIZE = 0.75F;
    private static final float GLOW_ALPHA = 0.55F;

    private static final float STREAK_TICKS = 2.2F;
    private static final float STREAK_MOST = 3.5F;
    private static final float STREAK_WIDTH = 0.16F;

    public EmeraldSplashRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EmeraldSplash entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        int tint = entity.tint();
        int r = tint == 0 ? 255 : (tint >> 16) & 0xFF;
        int g = tint == 0 ? 255 : (tint >> 8) & 0xFF;
        int b = tint == 0 ? 255 : tint & 0xFF;
        int glow = TendrilStrands.glowOf(tint);
        float age = entity.tickCount + partialTick;
        float scale = entity.isFragment() ? FRAGMENT_SCALE : 1F;

        streak(entity, partialTick, poseStack, buffers, glow, age, scale);

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());

        float breathe = 1F + 0.12F * Mth.sin(age * 0.9F + entity.getId());
        glowDisc(poseStack, buffers.getBuffer(EmeraldGlow.ADDITIVE), GLOW_SIZE * breathe * scale, glow, GLOW_ALPHA);
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(EmeraldGlow.ADDITIVE);
        }

        float turn = (entity.getId() * 47F) + age * SPIN * ((entity.getId() & 1) == 0 ? 1F : -1F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(turn));
        poseStack.scale(SIZE * scale, SIZE * scale, SIZE * scale);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(SPRITE));
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, -0.5F, -0.5F, 0F, 1F, r, g, b);
        vertex(consumer, pose, 0.5F, -0.5F, 1F, 1F, r, g, b);
        vertex(consumer, pose, 0.5F, 0.5F, 1F, 0F, r, g, b);
        vertex(consumer, pose, -0.5F, 0.5F, 0F, 0F, r, g, b);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, light);
    }

    private static void glowDisc(PoseStack poseStack, VertexConsumer consumer, float size, int colour, float alpha) {
        Matrix4f pose = poseStack.last().pose();
        float r = ((colour >> 16) & 0xFF) / 255F;
        float g = ((colour >> 8) & 0xFF) / 255F;
        float b = (colour & 0xFF) / 255F;
        float radius = size * 0.5F;
        for (int i = 0; i < 10; i++) {
            float a0 = i * Mth.TWO_PI / 10F;
            float a1 = (i + 1) * Mth.TWO_PI / 10F;
            consumer.addVertex(pose, 0F, 0F, 0F).setColor(Mth.lerp(0.5F, r, 1F), Mth.lerp(0.5F, g, 1F),
                    Mth.lerp(0.5F, b, 1F), alpha);
            consumer.addVertex(pose, 0F, 0F, 0F).setColor(Mth.lerp(0.5F, r, 1F), Mth.lerp(0.5F, g, 1F),
                    Mth.lerp(0.5F, b, 1F), alpha);
            consumer.addVertex(pose, Mth.cos(a1) * radius, Mth.sin(a1) * radius, 0F).setColor(r, g, b, 0F);
            consumer.addVertex(pose, Mth.cos(a0) * radius, Mth.sin(a0) * radius, 0F).setColor(r, g, b, 0F);
        }
    }

    private void streak(EmeraldSplash entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                        int colour, float age, float scale) {
        Vec3 velocity = entity.getDeltaMovement();
        double speed = velocity.length();
        if (speed < 1.0E-3) {
            return;
        }
        double length = Math.min(STREAK_MOST, speed * STREAK_TICKS) * Mth.clamp(age / 3F, 0F, 1F);
        if (length < 0.05) {
            return;
        }
        Vec3 back = velocity.scale(-length / speed);
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition()
                .subtract(entity.getPosition(partialTick));
        Vec3 side = back.cross(eye);
        if (side.lengthSqr() < 1.0E-8) {
            return;
        }
        side = side.normalize();
        float r = ((colour >> 16) & 0xFF) / 255F;
        float g = ((colour >> 8) & 0xFF) / 255F;
        float b = (colour & 0xFF) / 255F;
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = buffers.getBuffer(EmeraldGlow.ADDITIVE);
        int pieces = 6;
        for (int i = 0; i < pieces; i++) {
            float t0 = i / (float) pieces;
            float t1 = (i + 1) / (float) pieces;
            Vec3 a = back.scale(t0);
            Vec3 c = back.scale(t1);
            float w0 = STREAK_WIDTH * scale * (1F - t0);
            float w1 = STREAK_WIDTH * scale * (1F - t1);
            float s0 = 0.85F * (1F - t0) * (1F - t0);
            float s1 = 0.85F * (1F - t1) * (1F - t1);
            quad(consumer, pose, a.add(side.scale(w0)), a, c, c.add(side.scale(w1)), r, g, b, 0F, s0, s1, 0F);
            quad(consumer, pose, a, a.subtract(side.scale(w0)), c.subtract(side.scale(w1)), c, r, g, b,
                    s0, 0F, 0F, s1);
        }
    }

    private static void quad(VertexConsumer consumer, Matrix4f pose, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                             float r, float g, float b, float a0, float a1, float a2, float a3) {
        consumer.addVertex(pose, (float) p0.x, (float) p0.y, (float) p0.z).setColor(r, g, b, a0);
        consumer.addVertex(pose, (float) p1.x, (float) p1.y, (float) p1.z).setColor(r, g, b, a1);
        consumer.addVertex(pose, (float) p2.x, (float) p2.y, (float) p2.z).setColor(r, g, b, a2);
        consumer.addVertex(pose, (float) p3.x, (float) p3.y, (float) p3.z).setColor(r, g, b, a3);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v,
                               int r, int g, int b) {
        consumer.addVertex(pose, x, y, 0F)
                .setColor(r, g, b, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0F, 1F, 0F);
    }

    @Override
    public ResourceLocation getTextureLocation(EmeraldSplash entity) {
        return SPRITE;
    }
}
