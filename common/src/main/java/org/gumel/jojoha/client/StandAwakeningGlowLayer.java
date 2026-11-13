package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class StandAwakeningGlowLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private static final float[] HALO_SCALES = {1.07F, 1.16F};
    private static final float[] HALO_ALPHAS = {0.45F, 0.20F};

    private static final float BODY_CENTRE_Y = 0.9F;

    public StandAwakeningGlowLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float flash = StandAwakeningRays.whiteFlash(player, (float) minecraft.level.getGameTime() + partialTick);
        if (flash <= 0F) {
            return;
        }

        float[] tint = StandAwakeningRays.glowTint(player.getUUID());

        if (!isWhite(tint)) {
            return;
        }

        drawGlow(poseStack, bufferSource, 1F, flash, tint);

        if (isWhite(tint)) {
            for (int i = 0; i < HALO_SCALES.length; i++) {
                drawGlow(poseStack, bufferSource, HALO_SCALES[i], flash * HALO_ALPHAS[i], tint);
            }
        }
    }

    private static boolean isWhite(float[] tint) {
        return tint[0] >= 0.99F && tint[1] >= 0.99F && tint[2] >= 0.99F;
    }

    private void drawGlow(PoseStack poseStack, MultiBufferSource bufferSource, float scale, float alpha,
                          float[] tint) {
        if (alpha <= 0F) {
            return;
        }

        poseStack.pushPose();
        if (scale != 1F) {
            poseStack.translate(0F, BODY_CENTRE_Y, 0F);
            poseStack.scale(scale, scale, scale);
            poseStack.translate(0F, -BODY_CENTRE_Y, 0F);
        }

        getParentModel().renderToBuffer(
                poseStack,
                bufferSource.getBuffer(RenderType.eyes(WHITE)),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                tintedWithAlpha(tint, alpha));

        poseStack.popPose();
    }

    private static int tintedWithAlpha(float[] tint, float strength) {
        int r = Mth.clamp(Math.round(tint[0] * strength * 255F), 0, 255);
        int g = Mth.clamp(Math.round(tint[1] * strength * 255F), 0, 255);
        int b = Mth.clamp(Math.round(tint[2] * strength * 255F), 0, 255);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
