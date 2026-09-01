package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.gumel.jojoha.stand.StandEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * A bound Stand, drawn onto its user rather than beside them.
 *
 * <h2>Why it is a layer and not an entity any more</h2>
 *
 * <p>Hermit Purple is not a figure that follows you about - it is vines growing out of your arms,
 * and the difference is the whole of how it should behave. Drawn as an entity it could only ever
 * <em>imitate</em> being attached: the entity was pinned to the player's position, its yaw copied
 * from the player's body, and its arms swung by re-deriving vanilla's own walk formulas. Every one
 * of those is a separate copy of something the player model already knows, and each is one more
 * thing that can disagree.
 *
 * <p>And they did disagree, because copies always do. Sneaking, riding, swimming, using an item,
 * being hurt, holding a bow - the player's arms move for a dozen reasons, and the entity only knew
 * about walking. Anything else and the vines carried on doing their own thing while the arms they
 * were supposedly growing from went somewhere else.
 *
 * <p>As a layer there is nothing to copy. The vines are drawn <em>inside</em> the arm's own posed
 * frame, so whatever vanilla did to that arm has already happened by the time this runs - every
 * reason an arm moves, including ones added by other mods, and ones nobody has thought of yet. They
 * cannot come loose because they are not being kept in step with anything; they are part of the same
 * pose.
 *
 * <h2>How the frame is handed over</h2>
 *
 * <p>{@code translateAndRotate} puts the stack at the arm's pivot with the arm's rotation applied.
 * Undoing just the translation - not the rotation - leaves a frame that is the model's own origin
 * turned about the shoulder, which is exactly what a sleeve on that arm lives in. The vines are then
 * placed at their authored position relative to the body, and swing because the frame does.
 */
public class StandBoundArmsLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public StandBoundArmsLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }

        StandEntity stand = StandEntityLookup.boundStandOf(player);
        if (stand == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        GeoRenderer<StandEntity> renderer = StandArmRender.rendererFor(minecraft, stand);
        if (renderer == null) {
            return;
        }

        BakedGeoModel model = StandArmRender.modelFor(renderer, stand);
        if (model == null) {
            return;
        }

        PlayerModel<AbstractClientPlayer> body = getParentModel();
        arm(poseStack, buffers, light, stand, renderer, model, body.rightArm,
                StandModel.BONE_ON_RIGHT, partialTick);
        arm(poseStack, buffers, light, stand, renderer, model, body.leftArm,
                StandModel.BONE_ON_LEFT, partialTick);
    }

    /**
     * Enters one posed arm and draws the vine that grows from it.
     *
     * <p>The translation is undone and the rotation is not, deliberately. Undoing both would put the
     * stack back where it started and the vines would hang off the body instead of the arm; undoing
     * neither would place them relative to the shoulder rather than to the model they were authored
     * against. Taking out only the offset leaves the model's frame, rotated about the shoulder -
     * which is what being attached to an arm means.
     *
     * <h2>The rest offset, not the current one</h2>
     *
     * <p>This distinction is the whole of a bug worth keeping. An animation can move an arm as well
     * as turn it - vanilla shifts them for crouching, and the arrow ritual drives real position
     * keyframes through {@code offsetPos} - so a part's x, y and z are not a fixed pivot, they are
     * wherever the arm is this frame.
     *
     * <p>Undoing by that live value cancels the very thing it should be following. Work it through
     * for a point p with the arm moved by d and not turned at all: the frame puts it at
     * {@code (rest + d) + (p - rest - d)}, which is p. Exactly p, every time - the offset subtracts
     * itself out and the vines sit still while the arm walks off without them. Rotations came
     * through fine, which is why walking and punching looked right and only the ritual did not.
     *
     * <p>Against the rest pose it comes to {@code (rest + d) + (p - rest)}, which is p + d, and the
     * vines go where the arm goes. {@code getInitialPose} is the honest source for that: it is the
     * pose the part was built with, so it is also correct for slim arms, whose shoulders sit
     * somewhere different from the default model's.
     */
    private static void arm(PoseStack poseStack, MultiBufferSource buffers, int light,
                            StandEntity stand, GeoRenderer<StandEntity> renderer,
                            BakedGeoModel model, ModelPart limb, String boneName,
                            float partialTick) {
        poseStack.pushPose();

        limb.translateAndRotate(poseStack);
        PartPose rest = limb.getInitialPose();
        poseStack.translate(-rest.x / 16F, -rest.y / 16F, -rest.z / 16F);

        StandArmRender.arm(poseStack, buffers, light, stand, renderer, model, boneName, partialTick);

        poseStack.popPose();
    }
}
