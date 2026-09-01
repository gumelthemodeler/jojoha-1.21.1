package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import org.gumel.jojoha.client.anim.BedrockAnimation;
import org.joml.Vector3f;

/**
 * Draws the Stone Mask on a player's face once it has been seated there.
 *
 * <p>Parented to the head rather than placed by hand. {@code translateAndRotate} puts the matrix
 * into the head's own space, so the mask inherits every rotation the head has - looking around, and
 * whatever the equip and awakening animations are doing to it - without any of that being restated
 * here. Render layers run after the model has been posed, so by the time this draws, the head it is
 * following already holds the animated pose rather than the rest one.
 *
 * <p>Three passes, and only the first is always there:
 *
 * <ol>
 *   <li>The mask itself, cut out and unculled - see {@link #maskType}.</li>
 *   <li>The glow map added on top once it has woken - {@code stone_mask_activated_e}, which is ten
 *   lit pixels on an otherwise empty sheet. Adding a dedicated map rather than the skin again means
 *   only the eyes contribute light; adding the skin to itself lifted the stone along with them.</li>
 *   <li>White, while the wearer is whiting out. The glow layer next door does this to the player's
 *   own model and knows nothing about a mask, so a mask left out of it would sit dark on a face
 *   that had gone completely white.</li>
 * </ol>
 */
public final class StoneMaskLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    /** Vanilla's 1x1 white texture: every UV on the mask samples pure white out of it. */
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    /**
     * How hard the woken eyes burn.
     *
     * <p>Short of full. Additive at 1.0 takes the red to pure white and loses the colour entirely -
     * the point is that they glow red, not that they are bright.
     */
    private static final float EYE_GLOW = 0.75F;

    private final StoneMaskModel mask;

    public StoneMaskLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                          ModelPart root) {
        super(parent);
        this.mask = new StoneMaskModel(root);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        if (player.isInvisible() || player.isSpectator()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        float now = minecraft.level == null ? 0F : (float) minecraft.level.getGameTime() + partialTick;

        // Nothing here draws it falling any more. Once it comes off it is a real entity with its
        // own position - see FallingMask - so this layer's job ends the moment it is no longer worn.
        if (!StoneMaskState.wornBy(player)) {
            return;
        }

        // The mask stays, and burns. The light comes from the mask, so the body's own glow has been
        // taken away to make room for it - see StandAwakeningGlowLayer. Its own curve rather than
        // the body's, because the mask is the light and has nothing behind it to fade back to.
        float flash = StandAwakeningRays.burn(player, now);

        // Only a fed mask ever reaches a face, so the unwoken state on a head is the bloodied one -
        // the clean sheet is for the mask sitting in an inventory having never been used.
        float turn = StoneMaskState.turnProgress(player, now);

        poseStack.pushPose();
        getParentModel().head.translateAndRotate(poseStack);


        // One sheet, at full opacity, throughout. Crossing between two sheets faded the whole mask
        // to make two small red patches appear, which is a great deal of movement to buy a detail -
        // and while it crossed, neither sheet was fully drawn, so the stone itself went thin. The
        // turn belongs to the eyes and nothing else, and the eyes are a separate additive pass
        // below that can be brought up on its own.
        mask.renderToBuffer(poseStack, bufferSource.getBuffer(maskType(StoneMaskModel.TEXTURE_BLOODIED)),
                packedLight, OverlayTexture.NO_OVERLAY, -1);

        // What the light actually is. Additive over the mask's own shape, tinted the colour the
        // awakening is running in, and driven by the same curve the rays are - so the mask brightens
        // and dies back in step with the burst coming off it rather than on a clock of its own.
        if (flash > 0F) {
            float[] tint = StandAwakeningRays.glowTint(player.getUUID());
            mask.renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.eyes(WHITE)),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    tinted(tint, flash * MASK_BURN));
        }

        // The turn, and the whole of it. The glow map is ten lit pixels on an otherwise empty
        // sheet, so bringing it up from nothing lights the eyes and touches not one texel of the
        // stone around them - which is what "the eyes fade into red" actually asks for.
        if (turn > 0F) {
            mask.renderToBuffer(poseStack, bufferSource.getBuffer(RenderType.eyes(StoneMaskModel.TEXTURE_GLOW)),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, argb(EYE_GLOW * turn));
        }

        poseStack.popPose();
    }

    /**
     * Cut out, and explicitly unculled.
     *
     * <p>No-cull because a mask is a shell: look at it through an eye socket and what should be
     * behind the hole is the inside of the far wall, which is a back face. Culled, that face is
     * dropped and the socket becomes a window straight through the head.
     */
    private static RenderType maskType(ResourceLocation sheet) {
        // Translucent rather than cutout, and only because the mask has to be able to fade. Cutout
        // is a binary test - a texel is drawn or discarded - so it cannot express a half-present
        // mask at all. The sheets carry no partial alpha of their own (measured: every pixel is
        // either fully opaque or fully clear), so at full opacity this draws exactly what cutout
        // did. Still no-cull, for the same reason as before.
        return RenderType.entityTranslucent(sheet);
    }

    /**
     * How hard the mask itself burns at the peak of the awakening.
     *
     * <p>Just short of full. At 1.0 the additive pass takes every texel to white and the mask
     * becomes a featureless silhouette - which is the same mistake the body glow was making, only
     * on a smaller shape. Short of it, the stone stays legible inside the light.
     */
    private static final float MASK_BURN = 0.85F;

    /** The pass colour as packed ARGB, tinted and at the given strength. */
    /** The tint at a given strength, folded into the colour for the reason given on {@link #argb}. */
    private static int tinted(float[] tint, float strength) {
        int r = Mth.clamp(Math.round(tint[0] * strength * 255F), 0, 255);
        int g = Mth.clamp(Math.round(tint[1] * strength * 255F), 0, 255);
        int b = Mth.clamp(Math.round(tint[2] * strength * 255F), 0, 255);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /**
     * A strength for an additive pass, encoded where it cannot be ignored.
     *
     * <p>In the RGB channels, not the alpha. Additive blending comes in two forms - {@code SRC_ALPHA,
     * ONE} and {@code ONE, ONE} - and under the second the source alpha is never consulted at all,
     * so a fade written into alpha is silently dropped and the pass draws at full strength until the
     * moment it is skipped entirely. That is exactly what an abrupt shut-off looks like. Scaling the
     * colour instead dims the contribution under either form, because the shader multiplies the
     * texture by this before anything blends.
     */
    private static int argb(float strength) {
        int c = Mth.clamp(Math.round(strength * 255F), 0, 255);
        return 0xFF000000 | (c << 16) | (c << 8) | c;
    }
}
