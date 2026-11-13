package org.gumel.jojoha.client.fp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.client.ParryFeel;
import org.gumel.jojoha.client.anim.BedrockAnimation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class FirstPersonArms {
    private static final float EYE_Y = -1.92F;

    private static boolean drawing;

    private FirstPersonArms() {
    }

    public static boolean drawing() {
        return drawing;
    }

    public static boolean render(PoseStack poseStack, MultiBufferSource buffers, int light,
                                 AbstractClientPlayer player,
                                 ItemInHandRenderer items) {
        if (!FirstPersonPoses.active()) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return false;
        }

        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        drawArm(poseStack, buffers, light, player, renderer, model, HumanoidArm.RIGHT, items);
        drawArm(poseStack, buffers, light, player, renderer, model, HumanoidArm.LEFT, items);
        return true;
    }

    private static void drawArm(PoseStack poseStack, MultiBufferSource buffers, int light,
                                AbstractClientPlayer player, PlayerRenderer renderer,
                                PlayerModel<AbstractClientPlayer> model, HumanoidArm arm,
                                ItemInHandRenderer items) {
        boolean right = arm == HumanoidArm.RIGHT;
        String bone = right ? FirstPersonPose.RIGHT_ARM : FirstPersonPose.LEFT_ARM;

        poseStack.pushPose();

        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, -EYE_Y / 16.0F, 0.0F);

        ModelPart part = right ? model.rightArm : model.leftArm;
        part.resetPose();
        Vector3f offset = FirstPersonPoses.sample(bone, BedrockAnimation.Channel.POSITION);
        Vector3f rotation = FirstPersonPoses.sample(bone, BedrockAnimation.Channel.ROTATION);

        float px = part.x + (offset == null ? 0F : offset.x);
        float py = part.y + (offset == null ? 0F : offset.y);
        float pz = part.z + (offset == null ? 0F : offset.z);
        poseStack.translate(px / 16F, py / 16F, pz / 16F);

        if (rotation != null) {
            poseStack.mulPose(new Quaternionf().rotationZYX(rotation.z, rotation.y, rotation.x));
        }
        Vector3f kick = ParryFeel.firstPerson(bone);
        if (kick != null) {
            poseStack.mulPose(new Quaternionf().rotationZYX(kick.z, kick.y, kick.x));
        }

        Vector3f scale = FirstPersonPoses.sample(bone, BedrockAnimation.Channel.SCALE);
        if (scale != null) {
            poseStack.scale(scale.x, scale.y, scale.z);
        }

        poseStack.translate(-part.x / 16F, -part.y / 16F, -part.z / 16F);

        drawing = true;
        try {
            if (right) {
                renderer.renderRightHand(poseStack, buffers, light, player);
            } else {
                renderer.renderLeftHand(poseStack, buffers, light, player);
            }
        } finally {
            drawing = false;
        }

        drawHeldItem(poseStack, buffers, light, player, arm, items, part);

        poseStack.popPose();
    }

    private static void drawHeldItem(PoseStack poseStack, MultiBufferSource buffers, int light,
                                     AbstractClientPlayer player, HumanoidArm arm,
                                     ItemInHandRenderer items, ModelPart part) {
        FirstPersonPose pose = FirstPersonPoses.playing();
        if (items == null || pose == null || !pose.item() || arm != player.getMainArm()) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        part.translateAndRotate(poseStack);

        boolean left = arm == HumanoidArm.LEFT;
        poseStack.mulPose(Axis.XP.rotationDegrees(-90F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
        poseStack.translate((left ? -1F : 1F) / 16F, 0.125F, -0.625F);

        items.renderItem(player, held, left
                        ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                        : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                left, poseStack, buffers, light);

        poseStack.popPose();
    }
}
