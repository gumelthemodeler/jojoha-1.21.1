package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import org.gumel.jojoha.item.ThrownDagger;

/**
 * A thrown dagger, drawn as the dagger it is, pointing where it is going.
 *
 * <p>No model of its own: the daggers are item sprites, and Minecraft builds a real extruded model
 * out of any such sprite, so drawing the item is not a stand-in for a dagger model - it <em>is</em>
 * the dagger's model, with thickness, lit and shaded like the one in your hand.
 *
 * <p>Laid along its own flight path and left there. It used to spin about that axis, which was a
 * guess at what a thrown knife looks like and read as tumbling debris; a blade that holds its line
 * reads as thrown. The pitch and yaw are interpolated because the entity moves in tick-sized steps
 * and the eye does not.
 */
public class ThrownDaggerRenderer extends EntityRenderer<ThrownDagger> {
    /**
     * How far the sprite has to be turned to put its blade along the direction of travel.
     *
     * <p>Item sprites are drawn corner to corner - the handle sits at the bottom left of the square
     * and the tip at the top right - so the blade runs at forty-five degrees across a model whose
     * local {@code +X} is the way it is flying. Turning the sprite back by that much is what points
     * the tip forward instead of up and to the side.
     */
    private static final float SPRITE_DIAGONAL_DEGREES = -45F;

    /**
     * Undoes the shrink the GROUND context applies.
     *
     * <p>GROUND is the transform a dropped item uses, and part of what it means is "half size" -
     * a stack lying in the grass is meant to look smaller than the thing you were holding. A dagger
     * in flight is not a dropped item, it is the weapon, mid-throw, and it should be exactly as big
     * as it was in the hand it left. Doubling cancels the 0.5 the context brings and leaves the
     * centring and the sane orientation it also brings.
     */
    private static final float GROUND_SCALE_CORRECTION = 2F;

    private final ItemRenderer itemRenderer;

    public ThrownDaggerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownDagger dagger, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Vanilla's own arrow convention, and it is a convention rather than an obvious pair of
        // numbers: after these two the local +X axis is the direction of flight. The -90 and the
        // unsigned pitch are what make that true.
        poseStack.mulPose(Axis.YP.rotationDegrees(
                Mth.lerp(partialTick, dagger.yRotO, dagger.getYRot()) - 90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(
                Mth.lerp(partialTick, dagger.xRotO, dagger.getXRot())));

        poseStack.mulPose(Axis.ZP.rotationDegrees(SPRITE_DIAGONAL_DEGREES));
        poseStack.scale(GROUND_SCALE_CORRECTION, GROUND_SCALE_CORRECTION, GROUND_SCALE_CORRECTION);

        // GROUND for its centring and orientation, with its shrink cancelled above.
        itemRenderer.renderStatic(dagger.displayStack(), ItemDisplayContext.GROUND,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, bufferSource,
                dagger.level(), dagger.getId());

        poseStack.popPose();
        super.render(dagger, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /**
     * Unused - the item renderer binds the block atlas itself.
     *
     * <p>Still required: {@link EntityRenderer} declares it abstract, so something has to be
     * returned even though nothing asks for it.
     */
    @Override
    public ResourceLocation getTextureLocation(ThrownDagger dagger) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}
