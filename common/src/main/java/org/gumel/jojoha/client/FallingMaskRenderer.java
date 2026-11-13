package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.item.FallingMask;

public class FallingMaskRenderer extends EntityRenderer<FallingMask> {
    private static final float HEAD_OFFSET = 0.25F;

    private static final float EYES_OUT_TICKS = 14F;

    private final StoneMaskModel mask;

    public FallingMaskRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.mask = new StoneMaskModel(context.bakeLayer(StoneMaskModel.LAYER));
    }

    @Override
    public void render(FallingMask entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        poseStack.translate(0F, HEAD_OFFSET, 0F);
        poseStack.scale(-1F, -1F, 1F);
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.spinDegrees(partialTick)));
        poseStack.translate(0F, -StoneMaskModel.CENTRE_Y / 16F, -StoneMaskModel.CENTRE_Z / 16F);

        boolean aja = entity.aja();
        mask.setAja(aja);

        mask.renderToBuffer(poseStack,
                bufferSource.getBuffer(RenderType.entityTranslucent(StoneMaskModel.activated(aja))),
                packedLight, OverlayTexture.NO_OVERLAY, -1);

        float dimming = 1F - Mth.clamp((entity.tickCount + partialTick) / EYES_OUT_TICKS, 0F, 1F);
        if (dimming > 0F) {
            mask.renderToBuffer(poseStack,
                    bufferSource.getBuffer(RenderType.eyes(StoneMaskModel.glow(aja))),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, argb(dimming * dimming));
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static int argb(float strength) {
        int c = Mth.clamp(Math.round(strength * 255F), 0, 255);
        return 0xFF000000 | (c << 16) | (c << 8) | c;
    }

    @Override
    public ResourceLocation getTextureLocation(FallingMask entity) {
        return StoneMaskModel.activated(entity.aja());
    }
}
