package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.stand.StandEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Draws whatever the Stand is holding, in the hand it is holding it with.
 *
 * <p>Only ever visible for the moment around a use - see {@code StandEntity.showHeldItem}. A Stand
 * is not a creature that carries things about; it is a pair of hands that appears, does one thing
 * and lets go, and the item should behave the same way. Holding it permanently would also raise a
 * question nobody wants answered, which is what happens to it when the Stand is dismissed.
 *
 * <p>Attached to {@code bone2}, the lower right arm, because that is where the hand is - the model
 * has no hand bone of its own, and {@code finger} is a single cube of the index finger rather than
 * a grip. The offset below carries the item from the elbow, which is where that bone's origin sits,
 * down to where a fist would be.
 */
public final class StandHeldItemLayer extends BlockAndItemGeoLayer<StandEntity> {
    /** The lower right arm. Its pivot is the elbow; the fist sits five units down it. */
    private static final String HAND_BONE = "bone2";

    /**
     * Elbow to fist, in bone-local units.
     *
     * <p>Measured off the model rather than guessed. {@code bone2} pivots at y=18 and its forearm
     * cube ends at y=12, with the finger cube occupying y=12 to y=14 - so the hand is the very end
     * of that run and anything less than six units along the arm is still resting on the limb. Six
     * and a half puts the item just past the fist, where a hand closed around it would be. Model
     * space is a sixteenth of a block and the renderer inverts Y, which is why travelling down the
     * arm is a positive number here.
     */
    private static final float HAND_OFFSET = 6.5F / 16F;

    /**
     * Out of the forearm, so the item rests against the front of the fist rather than inside it.
     *
     * <p>The forearm cube is four units deep about z=0 and the finger sits at z=-1.5 to -0.5, so a
     * little under a unit forward clears the wrist without the item appearing to float free of it.
     */
    private static final float HAND_REACH_OUT = 1.4F / 16F;

    /**
     * How the item lies in the hand.
     *
     * <p>The quarter turn is the difference between two conventions, not a taste: the arm hangs
     * down the model's Y while a held item's own display transform was authored to lie along Z on a
     * vanilla arm. This is the number to nudge if the item reads as held at the wrong angle - it is
     * the only one that changes the pose rather than the position.
     */
    private static final float HAND_TILT_DEGREES = -90F;

    /**
     * Slightly under full size.
     *
     * <p>A Stand is taller than a player and its hands are proportionally larger, so an item drawn
     * at the size a player would hold it reads as a toy in them. Held back from the difference in
     * scale rather than matched to it: an item is a real object of a fixed size, and the hand
     * holding it is the thing that should look big.
     */
    private static final float SCALE = 0.85F;

    public StandHeldItemLayer(GeoRenderer<StandEntity> renderer) {
        super(renderer);
    }

    @Override
    protected ItemStack getStackForBone(GeoBone bone, StandEntity stand) {
        // Asked for every bone in the model, so the name check is the first thing and the entity is
        // only consulted for the one bone that could possibly be holding anything.
        return HAND_BONE.equals(bone.getName()) ? stand.getItemBySlot(EquipmentSlot.MAINHAND) : null;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack,
                                                          StandEntity stand) {
        return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                                      StandEntity stand, MultiBufferSource bufferSource,
                                      float partialTick, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        poseStack.translate(0F, HAND_OFFSET, HAND_REACH_OUT);

        poseStack.mulPose(Axis.XP.rotationDegrees(HAND_TILT_DEGREES));
        poseStack.scale(SCALE, SCALE, SCALE);

        super.renderStackForBone(poseStack, bone, stack, stand, bufferSource, partialTick,
                packedLight, packedOverlay);

        poseStack.popPose();
    }
}
