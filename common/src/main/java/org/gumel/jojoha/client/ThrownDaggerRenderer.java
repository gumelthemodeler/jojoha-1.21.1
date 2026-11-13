package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.entity.StandKnife;
import org.gumel.jojoha.item.ThrownDagger;

public class ThrownDaggerRenderer extends EntityRenderer<ThrownDagger> {
    private static final float SPRITE_DIAGONAL_DEGREES = -45F;

    private static final float GROUND_SCALE_CORRECTION = 2F;

    private final ItemRenderer itemRenderer;

    public ThrownDaggerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownDagger dagger, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        if (dagger instanceof StandKnife knife && knife.gilded()) {
            gilded(knife, partialTick, poseStack, bufferSource);
            super.render(dagger, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            return;
        }
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(
                Mth.lerp(partialTick, dagger.yRotO, dagger.getYRot()) - 90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                Mth.lerp(partialTick, dagger.xRotO, dagger.getXRot())));

        poseStack.mulPose(Axis.ZP.rotationDegrees(SPRITE_DIAGONAL_DEGREES));
        poseStack.scale(GROUND_SCALE_CORRECTION, GROUND_SCALE_CORRECTION, GROUND_SCALE_CORRECTION);

        itemRenderer.renderStatic(dagger.displayStack(), ItemDisplayContext.GROUND,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource,
                dagger.level(), dagger.getId());

        poseStack.popPose();
        super.render(dagger, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static final float[] GILT_BODY = {1.0F, 0.86F, 0.42F, 0.6F};
    private static final float[] GILT_BLOOM = {0.95F, 0.72F, 0.26F, 1.0F};
    private static final float[] GILT_RIM = {0.42F, 0.3F, 0.1F, 1.0F};
    private static final float[] GILT_HAZE = {0.16F, 0.11F, 0.035F, 1.0F};
    private static final float[] GILT_TRAIL = {0.5F, 0.36F, 0.12F, 1.0F};
    private static final float RIM_GROW = 1.3F;
    private static final float HAZE_GROW = 1.65F;
    private static final int TRAIL_COPIES = 3;
    private static final double TRAIL_SPACING = 0.3;
    private static final double TRAIL_MIN_SPEED = 0.08;

    private void gilded(StandKnife knife, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource) {
        RenderType body = RenderType.entityTranslucentEmissive(InventoryMenu.BLOCK_ATLAS);
        RenderType glow = RenderType.eyes(InventoryMenu.BLOCK_ATLAS);
        Vec3 still = Vec3.ZERO;
        gildPass(knife, partialTick, poseStack, bufferSource, still, 1F, body, GILT_BODY, 1F);
        gildPass(knife, partialTick, poseStack, bufferSource, still, 1F, glow, GILT_BLOOM, 1F);
        gildPass(knife, partialTick, poseStack, bufferSource, still, RIM_GROW, glow, GILT_RIM, 1F);
        gildPass(knife, partialTick, poseStack, bufferSource, still, HAZE_GROW, glow, GILT_HAZE, 1F);
        Vec3 motion = knife.getDeltaMovement();
        if (motion.length() < TRAIL_MIN_SPEED) {
            return;
        }
        Vec3 back = motion.normalize().scale(-TRAIL_SPACING * Math.min(2.0, motion.length()));
        for (int copy = 1; copy <= TRAIL_COPIES; copy++) {
            float fade = (TRAIL_COPIES + 1 - copy) / (float) (TRAIL_COPIES + 1);
            gildPass(knife, partialTick, poseStack, bufferSource, back.scale(copy), 1F + 0.08F * copy, glow,
                    GILT_TRAIL, fade);
        }
    }

    private void gildPass(StandKnife knife, float partialTick, PoseStack poseStack,
                          MultiBufferSource bufferSource, Vec3 offset, float grow,
                          RenderType type, float[] tint, float strength) {
        float[] shade = {tint[0] * strength, tint[1] * strength, tint[2] * strength, tint[3]};
        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, knife.yRotO, knife.getYRot()) - 90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, knife.xRotO, knife.getXRot())));
        poseStack.mulPose(Axis.ZP.rotationDegrees(SPRITE_DIAGONAL_DEGREES));
        float size = GROUND_SCALE_CORRECTION * grow;
        poseStack.scale(size, size, size);
        itemRenderer.renderStatic(knife.displayStack(), ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, poseStack, ignored -> new Gilt(bufferSource.getBuffer(type), shade),
                knife.level(), knife.getId());
        poseStack.popPose();
    }

    private static final class Gilt implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float[] tint;

        private Gilt(VertexConsumer delegate, float[] tint) {
            this.delegate = delegate;
            this.tint = tint;
        }

        private int tinted(int colour) {
            int a = Math.round(((colour >>> 24) & 0xFF) * tint[3]);
            int r = Math.round(((colour >> 16) & 0xFF) * tint[0]);
            int g = Math.round(((colour >> 8) & 0xFF) * tint[1]);
            int b = Math.round((colour & 0xFF) * tint[2]);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        @Override
        public void addVertex(float x, float y, float z, int colour, float u, float v, int overlay, int light,
                              float normalX, float normalY, float normalZ) {
            delegate.addVertex(x, y, z, tinted(colour), u, v, overlay, light, normalX, normalY, normalZ);
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            int colour = tinted((alpha << 24) | (red << 16) | (green << 8) | blue);
            delegate.setColor((colour >> 16) & 0xFF, (colour >> 8) & 0xFF, colour & 0xFF, colour >>> 24);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownDagger dagger) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}
