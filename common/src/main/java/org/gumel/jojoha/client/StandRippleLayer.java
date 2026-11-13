package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.ripple.RippleProfile;
import org.gumel.jojoha.stand.ripple.RippleProfiles;
import org.gumel.jojoha.supporter.SupporterAuras;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class StandRippleLayer extends GeoRenderLayer<StandEntity> {
    private static final float RESTING_STRENGTH = 0.8F;
    private static final float EFFORT_STRENGTH = 0.2F;

    public StandRippleLayer(GeoRenderer<StandEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, StandEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        if (buffer == null) {
            return;
        }

        RippleProfile own = RippleProfiles.of(animatable.getStandType());
        StandRipples.Live live = own == null ? null : StandRipples.of(animatable);
        if (live == null) {
            return;
        }

        float alpha = StandViewAlpha.of(animatable);
        if (alpha <= 0.001F) {
            return;
        }

        RippleProfile field = own.as(live.mode(), 1F);
        int tint = SupporterAuras.tint(animatable.getOwnerUuid(),
                animatable.getStandType().auraColorFor(animatable.getSkin()));
        ResourceLocation stencil = stencilFor(getRenderer().getTextureLocation(animatable),
                own.maskSuffix());
        ResourceLocation sheet = RippleTexture.of(stencil, field, live.mode(), tint,
                StandRipples.seconds());
        if (sheet == null) {
            return;
        }

        float strength = alpha * Math.min(1F,
                RESTING_STRENGTH + EFFORT_STRENGTH * Math.max(0F, live.settled() - 1F));

        RenderType body = RenderType.entityTranslucent(sheet);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, body,
                bufferSource.getBuffer(body), partialTick, packedLight, packedOverlay,
                white(strength));
    }

    private static int white(float strength) {
        return FastColor.ARGB32.color(Math.round(Mth.clamp(strength, 0F, 1F) * 255F), 255, 255, 255);
    }

    static ResourceLocation stencilFor(ResourceLocation skin, String suffix) {
        return ResourceLocation.fromNamespaceAndPath(skin.getNamespace(),
                skin.getPath().replace(".png", suffix + ".png"));
    }
}
