package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.grapple.HermitGrappleHook;

public class HermitGrappleRenderer extends EntityRenderer<HermitGrappleHook> {
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

        Vec3 hookAt = entity.getPosition(partialTick);
        Vec3 hand = hand(entity, owner, partialTick).subtract(hookAt);

        double span = hand.length();
        if (span < 1.0E-4) {
            return;
        }

        if (entity.isTendril()) {
            Vec3 eye = this.entityRenderDispatcher.camera.getPosition().subtract(hookAt);
            TendrilStrands.drawLine(poseStack, buffers, eye, hand, Vec3.ZERO, 0.07F, entity.tint());
            super.render(entity, entityYaw, partialTick, poseStack, buffers, light);
            return;
        }

        double ropeLength = Math.max(span, GrappleController.ropeLengthFor(entity, span));
        int skin = HermitSkins.of(owner);
        ThornRope.draw(poseStack, buffers, entity.level(), hand, hookAt, ropeLength, skin);

        drawHook(poseStack, buffers, light, skin);
        super.render(entity, entityYaw, partialTick, poseStack, buffers, light);
    }

    private void drawHook(PoseStack poseStack, MultiBufferSource buffers, int light, int skin) {
        Vec3 first = ThornRope.firstChord();
        if (first.lengthSqr() < 1.0E-8) {
            first = new Vec3(0, 1, 0);
        }

        poseStack.pushPose();
        ThornRope.aim(poseStack, first.normalize());

        hook.render(poseStack,
                buffers.getBuffer(RenderType.entityCutoutNoCull(HermitSkins.hook(skin))),
                ThornRope.glow(light), OverlayTexture.NO_OVERLAY);
        var lit = buffers.getBuffer(
                RenderType.entityTranslucentEmissive(HermitSkins.hookGlow(skin)));
        for (int i = 0; i < ThornRope.GLOW_PASSES; i++) {
            hook.render(poseStack, lit,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY);
        }

        poseStack.popPose();
    }

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
