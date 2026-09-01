package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.grapple.HermitGrappleHook;

/**
 * Draws the hook, and hands the vine itself to {@link ThornRope}.
 *
 * <p>The curve solving and segment walking used to live here and now do not, because the lasso needs
 * the same vine and two copies would be two places to fix. Given this particular code has already
 * been wrong three separate ways - backwards segments, no segments at all, and every segment at a
 * sixteenth scale - one copy is the only sensible number.
 *
 * <p>What is left is the part that really is about this entity: where the vine leaves the thrower,
 * and which way the barb points.
 */
public class HermitGrappleRenderer extends EntityRenderer<HermitGrappleHook> {

    /** Which parts of the barb are lit - see ThornRope for why this is painted rather than computed. */

    /** How far to either side of the hold the paired vines of a zip leave from. */
    private static final double ARM_SPREAD = 0.34;

    private final ModelPart hook;

    public HermitGrappleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.hook = context.bakeLayer(HermitGrappleModels.HOOK);
    }

    @Override
    public ResourceLocation getTextureLocation(HermitGrappleHook entity) {
        return HermitSkins.hook(HermitSkins.of(entity.holder()));
    }

    @Override
    public void render(HermitGrappleHook entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int light) {
        Player owner = entity.holder();
        if (owner == null) {
            return;
        }

        // Everything is worked out relative to the hook, because that is where the pose already is.
        Vec3 hookAt = entity.getPosition(partialTick);
        Vec3 hand = hand(entity, owner, partialTick).subtract(hookAt);

        double span = hand.length();
        if (span < 1.0E-4) {
            return;
        }

        double ropeLength = Math.max(span, GrappleController.ropeLengthFor(entity, span));
        int skin = HermitSkins.of(owner);
        ThornRope.draw(poseStack, buffers, entity.level(), hand, hookAt, ropeLength, skin);

        drawHook(poseStack, buffers, light, skin);
        super.render(entity, entityYaw, partialTick, poseStack, buffers, light);
    }

    /**
     * The barbed end, pointed back along the vine.
     *
     * <p>Taken from the first chord of the curve rather than from the straight line to the hand, so
     * on a slack rope the hook still sits in line with the vine actually touching it.
     */
    private void drawHook(PoseStack poseStack, MultiBufferSource buffers, int light, int skin) {
        Vec3 first = ThornRope.firstChord();
        if (first.lengthSqr() < 1.0E-8) {
            first = new Vec3(0, 1, 0);
        }

        poseStack.pushPose();
        ThornRope.aim(poseStack, first.normalize());

        // As the world lights it, then the lit parts on top. Safe to take the two buffers one after
        // the other here, unlike in the vine, because the first render finishes before the second
        // buffer is asked for - nothing is holding a consumer across the switch.
        hook.render(poseStack,
                buffers.getBuffer(RenderType.entityCutoutNoCull(HermitSkins.hook(skin))),
                ThornRope.glow(light), OverlayTexture.NO_OVERLAY);
        var lit = buffers.getBuffer(
                RenderType.entityTranslucentEmissive(HermitSkins.hookGlow(skin)));
        for (int i = 0; i < ThornRope.GLOW_PASSES; i++) {
            hook.render(poseStack, lit,
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY);
        }

        poseStack.popPose();
    }

    /**
     * Where the vine leaves the thrower.
     *
     * <p>Vanilla's own answer for where a held rope attaches, which already accounts for crouching,
     * swimming and elytra, and which the client's own player overrides with the on-screen hand - all
     * of which a hand-tuned offset off the eye position gets wrong in some pose.
     *
     * <p>A zip is pushed out to one shoulder or the other, across the body rather than along it, so
     * its two vines leave from two places and the pair reads as a pair.
     */
    private static Vec3 hand(HermitGrappleHook entity, Player owner, float partialTick) {
        Vec3 hold = owner.getRopeHoldPosition(partialTick);
        if (!entity.isZip()) {
            return hold;
        }

        float yaw = Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot)
                * ((float) Math.PI / 180F);
        Vec3 across = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
        return hold.add(across.scale(entity.isLeftArm() ? ARM_SPREAD : -ARM_SPREAD));
    }
}
