package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.stand.StandEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public final class StandKnivesLayer extends BlockAndItemGeoLayer<StandEntity> {
    private static final String RIGHT_PIVOT = "knife_pivot_right";
    private static final String LEFT_PIVOT = "knife_pivot_left";

    private static final String RIGHT_HAND = "bone2";
    private static final String LEFT_HAND = "bone3";

    private static final float HANDFUL_WIDTH = 2.5F / 16F;

    private static final float FAN_DEGREES = 8F;

    private static final float SCALE = 0.5F;

    private static final float FALLBACK_TILT_DEGREES = 0F;

    public StandKnivesLayer(GeoRenderer<StandEntity> renderer) {
        super(renderer);
    }

    private boolean hasPivots() {
        return this.getRenderer().getGeoModel().getBone(RIGHT_PIVOT).isPresent()
                || this.getRenderer().getGeoModel().getBone(LEFT_PIVOT).isPresent();
    }

    private boolean holds(GeoBone bone) {
        String name = bone.getName();
        return hasPivots()
                ? RIGHT_PIVOT.equals(name) || LEFT_PIVOT.equals(name)
                : RIGHT_HAND.equals(name) || LEFT_HAND.equals(name);
    }

    private static boolean isRight(GeoBone bone) {
        return RIGHT_PIVOT.equals(bone.getName()) || RIGHT_HAND.equals(bone.getName());
    }

    @Override
    protected ItemStack getStackForBone(GeoBone bone, StandEntity stand) {
        if (stand.knivesHeld() <= 0 || !holds(bone)) {
            return null;
        }
        return stand.knifeHeld(0);
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack,
                                                          StandEntity stand) {
        return isRight(bone)
                ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack,
                                      StandEntity stand, MultiBufferSource bufferSource,
                                      float partialTick, int packedLight, int packedOverlay) {
        boolean right = isRight(bone);
        boolean authored = hasPivots();

        int held = stand.knivesHeld();
        int inThisHand = right ? (held + 1) / 2 : held / 2;
        int first = right ? 0 : (held + 1) / 2;
        if (inThisHand <= 0) {
            return;
        }

        for (int i = 0; i < inThisHand; i++) {
            float across = inThisHand == 1 ? 0F : i / (float) (inThisHand - 1) - 0.5F;

            poseStack.pushPose();

            if (!authored) {
                poseStack.translate(0F, StandHeldItemLayer.HAND_OFFSET,
                        StandHeldItemLayer.HAND_REACH_OUT);
                if (FALLBACK_TILT_DEGREES != 0F) {
                    poseStack.mulPose(Axis.XP.rotationDegrees(FALLBACK_TILT_DEGREES));
                }
            }

            poseStack.translate(across * HANDFUL_WIDTH, 0F, 0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(across * FAN_DEGREES));
            poseStack.scale(SCALE, SCALE, SCALE);

            super.renderStackForBone(poseStack, bone, stand.knifeHeld(first + i), stand, bufferSource, partialTick,
                    packedLight, packedOverlay);

            poseStack.popPose();
        }
    }
}
