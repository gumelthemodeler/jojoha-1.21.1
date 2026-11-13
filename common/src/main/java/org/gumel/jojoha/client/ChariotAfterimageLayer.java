package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.chariot.Chariot;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class ChariotAfterimageLayer extends GeoRenderLayer<StandEntity> {
    private static final double RING = 1.15;
    private static final double SWAY = 0.25;
    private static final float TURN_PER_TICK = 0.035F;
    private static final float PEAK_ALPHA = 0.38F;
    private static final float FLICKER = 0.12F;

    public ChariotAfterimageLayer(GeoRenderer<StandEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, StandEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        int count = animatable.afterimages();
        if (count <= 0 || !Chariot.is(animatable) || !animatable.armorOff()) {
            return;
        }
        float view = StandViewAlpha.of(animatable);
        if (view <= 0.001F) {
            return;
        }
        ResourceLocation texture = getTextureResource(animatable);
        RenderType ghost = RenderType.entityTranslucent(texture);
        float age = animatable.tickCount + partialTick;
        for (int i = 0; i < count; i++) {
            double around = age * TURN_PER_TICK + i * Math.PI * 2.0 / count;
            double reach = RING + SWAY * Mth.sin(age * 0.21F + i * 1.7F);
            float alpha = view * (PEAK_ALPHA + FLICKER * Mth.sin(age * 0.9F + i * 2.3F));
            int colour = ((int) (Mth.clamp(alpha, 0F, 1F) * 255F) << 24) | 0xFFFFFF;

            poseStack.pushPose();
            poseStack.translate(Math.cos(around) * reach, 0.0, Math.sin(around) * reach);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, ghost,
                    bufferSource.getBuffer(ghost), partialTick, packedLight, packedOverlay, colour);
            poseStack.popPose();
        }
    }
}
