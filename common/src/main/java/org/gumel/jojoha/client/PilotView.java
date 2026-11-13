package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import org.gumel.jojoha.stand.StandEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;

public final class PilotView {
    private static final float EYES_ABOVE_NECK = 4F;

    private static StandEntity drawing;

    private PilotView() {
    }

    public static boolean drawing(StandEntity stand) {
        return stand != null && drawing == stand;
    }

    public static boolean render(PoseStack poseStack, MultiBufferSource buffers, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!StandSkillInput.isPiloting() || !minecraft.options.getCameraType().isFirstPerson()
                || !(minecraft.getCameraEntity() instanceof StandEntity stand) || !stand.isAlive()) {
            return false;
        }
        EntityRenderer<? super StandEntity> renderer = minecraft.getEntityRenderDispatcher().getRenderer(stand);
        if (renderer == null) {
            return false;
        }

        float bodyYaw = Mth.rotLerp(partialTick, stand.yBodyRotO, stand.yBodyRot);
        int light = minecraft.getEntityRenderDispatcher().getPackedLightCoords(stand, partialTick);

        poseStack.pushPose();
        poseStack.translate(0F, -eyeHeight(minecraft, stand), 0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw - 180F));
        drawing = stand;
        try {
            renderer.render(stand, bodyYaw, partialTick, poseStack, buffers, light);
        } finally {
            drawing = null;
            poseStack.popPose();
        }
        return true;
    }

    private static float eyeHeight(Minecraft minecraft, StandEntity stand) {
        GeoRenderer<StandEntity> geo = StandArmRender.rendererFor(minecraft, stand);
        BakedGeoModel model = geo == null ? null : StandArmRender.modelFor(geo, stand);
        GeoBone head = model == null ? null : model.getBone("Head").orElse(null);
        if (head == null) {
            return stand.getEyeHeight();
        }
        return (head.getPivotY() + EYES_ABOVE_NECK) / 16F * stand.getScale();
    }
}
