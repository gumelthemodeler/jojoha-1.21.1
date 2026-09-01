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

/**
 * Lights an awakening player up from the inside - light coming off the body, not paint on it.
 *
 * <p>The distinction matters, and neither obvious approach gives it. Vanilla's white-overlay
 * channel (the creeper flash) ramps only to {@code 1 - 0.75} alpha, so it can never fully cover
 * skin; and drawing the model again in flat white simply replaces the texture with a white
 * silhouette - opaque, but flat and lifeless, a paper cutout rather than something glowing.
 *
 * <p>What reads as a glow is <em>additive</em> light plus spill past the silhouette. So this draws
 * the model three times through {@link RenderType#eyes}, which is additive and writes colour only:
 * once on the body, where adding white saturates every channel and the skin genuinely blows out,
 * and twice more on slightly inflated copies at falling alpha, whose fringes extend beyond the
 * body's outline as a halo. Additive also means the effect brightens whatever is behind those
 * fringes instead of stamping over it, which is the giveaway that it's emitting rather than
 * covering.
 */
public final class StandAwakeningGlowLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    /** Vanilla's own 1x1 white texture - every UV on the model samples pure white from it. */
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    /**
     * The halo shells, outermost last. Kept subtle and few: additive passes stack, so a third
     * heavy shell reads as a solid white blob rather than as light falling off with distance.
     */
    private static final float[] HALO_SCALES = {1.07F, 1.16F};
    private static final float[] HALO_ALPHAS = {0.45F, 0.20F};

    /**
     * Roughly the player's mid-torso, in the render layer's own space.
     *
     * <p>The pose stack arrives with its origin at the top of the head and +Y pointing down (the
     * model-space convention entity rendering flips into), so scaling about the origin would grow
     * the halo downward out of the player's feet. Scaling about this point instead keeps it
     * concentric with the body.
     */
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

        // Whatever colour this awakening is running in. A Stand tearing loose is white; a Stone
        // Mask taking its wearer is red, and the same burst in a different colour is the whole of
        // the difference between the two events.
        float[] tint = StandAwakeningRays.glowTint(player.getUUID());

        // A coloured awakening is the mask's, and the mask is the thing that lights up - the glow
        // belongs on the object rather than on the person wearing it. Lighting the body as well
        // meant the source of it was ambiguous: everything was bright, so nothing was emitting.
        // See StoneMaskLayer, which draws it where it actually comes from.
        if (!isWhite(tint)) {
            return;
        }

        // Body pass. Additive at full strength saturates every channel it touches, so the skin is
        // covered by light rather than by a decal.
        drawGlow(poseStack, bufferSource, 1F, flash, tint);

        // Halo shells, drawn outward - but only for the white awakening they were built for. They
        // work by spilling past the body's silhouette, which is exactly what must not happen when
        // something is being worn on that silhouette: the spill reaches past the face and draws
        // attention to the shape of it, and a mask that has stood aside for the flash is then
        // conspicuously absent from an outline the halo has just traced.
        if (isWhite(tint)) {
            for (int i = 0; i < HALO_SCALES.length; i++) {
                drawGlow(poseStack, bufferSource, HALO_SCALES[i], flash * HALO_ALPHAS[i], tint);
            }
        }
    }

    /** A tint nobody changed. The halo belongs to that case and to no other. */
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

        // Full-bright and no overlay: this pass is a light source, so nothing about the world's
        // lighting or the player's damage tint should be allowed to modulate it.
        getParentModel().renderToBuffer(
                poseStack,
                bufferSource.getBuffer(RenderType.eyes(WHITE)),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                tintedWithAlpha(tint, alpha));

        poseStack.popPose();
    }

    /**
     * The pass colour, as packed ARGB.
     *
     * <p>The tint multiplies the white texture rather than replacing it, which is what lets one
     * additive pass be either a white-out or a red one without a second texture.
     */
    private static int tintedWithAlpha(float[] tint, float strength) {
        // Folded into the colour rather than the alpha. Additive blending may be ONE,ONE, in which
        // case the source alpha is never read and a fade written there does nothing at all.
        int r = Mth.clamp(Math.round(tint[0] * strength * 255F), 0, 255);
        int g = Mth.clamp(Math.round(tint[1] * strength * 255F), 0, 255);
        int b = Mth.clamp(Math.round(tint[2] * strength * 255F), 0, 255);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
