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

/**
 * The mask on its way down.
 *
 * <p>The same mesh the face wears, so there is no seam between the mask being worn and the mask
 * falling - it is the identical model, handed from one renderer to the other at the instant it
 * comes off.
 *
 * <p>Turned about its own X as it goes, because a mask coming off a face pitches forward off the
 * brow rather than spinning on the spot; the angle comes from the entity's age so every client
 * watching sees the same tumble without anything being sent about it.
 */
public class FallingMaskRenderer extends EntityRenderer<FallingMask> {
    /**
     * The mask is authored around a head, so its origin is where a head would be, not where the
     * mask is. Dropping it by that much puts the thing itself at the entity's position.
     */
    private static final float HEAD_OFFSET = 0.25F;

    /**
     * How long the eyes take to go out once it is falling, in ticks.
     *
     * <p>Shorter than the fall usually is, on purpose - the mask should land dark. Squared on top,
     * so it dims quickly and then finishes gently instead of the other way round.
     */
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

        // Model space is Y-down and twice the scale of the world, which is what every entity model
        // in the game is drawn under - this is vanilla's own convention, not a correction.
        poseStack.translate(0F, HEAD_OFFSET, 0F);
        poseStack.scale(-1F, -1F, 1F);
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.spinDegrees(partialTick)));

        mask.renderToBuffer(poseStack,
                bufferSource.getBuffer(RenderType.entityTranslucent(StoneMaskModel.TEXTURE_ACTIVATED)),
                packedLight, OverlayTexture.NO_OVERLAY, -1);

        // The eyes go out on the way down rather than at the instant it lands. Holding them at full
        // and then discarding the entity put the whole of the extinguishing into one frame, which
        // is the same cliff the burn had: the light has to be most of the way gone before the thing
        // carrying it breaks, or the break is what appears to have switched it off.
        // Interpolated, or the dimming would step once per tick - which is the one thing a fade
        // meant to remove a stutter must not do.
        float dimming = 1F - Mth.clamp((entity.tickCount + partialTick) / EYES_OUT_TICKS, 0F, 1F);
        if (dimming > 0F) {
            mask.renderToBuffer(poseStack,
                    bufferSource.getBuffer(RenderType.eyes(StoneMaskModel.TEXTURE_GLOW)),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, argb(dimming * dimming));
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /** Strength in the colour channels - see StoneMaskLayer.argb for why not the alpha. */
    private static int argb(float strength) {
        int c = Mth.clamp(Math.round(strength * 255F), 0, 255);
        return 0xFF000000 | (c << 16) | (c << 8) | c;
    }

    @Override
    public ResourceLocation getTextureLocation(FallingMask entity) {
        return StoneMaskModel.TEXTURE_ACTIVATED;
    }
}
