package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.stand.StandEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class TimeStopChargeGlowLayer extends GeoRenderLayer<StandEntity> {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private static final float BURST_GROWTH = 0.9F;
    private static final float HALO_SCALE = 1.06F;
    private static final float HALO_SHARE = 0.35F;

    public TimeStopChargeGlowLayer(GeoRenderer<StandEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, StandEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        Player owner = animatable.getOwner();
        if (owner == null) {
            return;
        }
        float seen = StandViewAlpha.of(animatable);
        float glow = TimeStopChargers.glow(owner.getId(), partialTick) * seen;
        float burst = TimeStopChargers.burst(owner.getId(), partialTick) * seen;
        if (glow <= 0.001F && burst <= 0.001F) {
            return;
        }
        float middle = animatable.getBbHeight() * 0.5F;
        if (glow > 0.001F) {
            shell(poseStack, animatable, bakedModel, bufferSource, partialTick, packedOverlay, middle, 1F, glow);
            shell(poseStack, animatable, bakedModel, bufferSource, partialTick, packedOverlay, middle,
                    HALO_SCALE, glow * glow * HALO_SHARE);
        }
        if (burst > 0.001F) {
            shell(poseStack, animatable, bakedModel, bufferSource, partialTick, packedOverlay, middle, 1F, burst);
            float spread = 1F + (1F - burst) * BURST_GROWTH;
            shell(poseStack, animatable, bakedModel, bufferSource, partialTick, packedOverlay, middle,
                    spread, burst * burst);
        }
    }

    private void shell(PoseStack poseStack, StandEntity animatable, BakedGeoModel bakedModel,
                       MultiBufferSource bufferSource, float partialTick, int packedOverlay,
                       float middle, float scale, float strength) {
        if (strength <= 0.001F) {
            return;
        }
        poseStack.pushPose();
        if (scale != 1F) {
            poseStack.translate(0F, middle, 0F);
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0F, -middle, 0F);
        }
        int shade = Mth.clamp(Math.round(strength * 255F), 0, 255);
        RenderType glow = RenderType.eyes(WHITE);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow,
                bufferSource.getBuffer(glow), partialTick, LightTexture.FULL_BRIGHT, packedOverlay,
                0xFF000000 | (shade << 16) | (shade << 8) | shade);
        poseStack.popPose();
    }
}
