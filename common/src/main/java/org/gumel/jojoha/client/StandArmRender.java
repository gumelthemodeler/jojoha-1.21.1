package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.stand.StandEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.texture.AutoGlowingTexture;
import software.bernie.geckolib.renderer.GeoRenderer;

final class StandArmRender {
    private static final float VANILLA_ORIGIN_HEIGHT = 1.5F;

    private static final float[] GLOW_PASSES = {1.0F, 0.7F, 0.45F};

    private StandArmRender() {
    }

    @SuppressWarnings("unchecked")
    static GeoRenderer<StandEntity> rendererFor(Minecraft minecraft, StandEntity stand) {
        EntityRenderer<?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(stand);
        return renderer instanceof GeoRenderer<?> geo ? (GeoRenderer<StandEntity>) geo : null;
    }

    static BakedGeoModel modelFor(GeoRenderer<StandEntity> renderer, StandEntity stand) {
        return renderer.getGeoModel()
                .getBakedModel(renderer.getGeoModel().getModelResource(stand, renderer));
    }

    static void arm(PoseStack poseStack, MultiBufferSource buffers, int light, StandEntity stand,
                    GeoRenderer<StandEntity> renderer, BakedGeoModel model, String boneName,
                    float partialTick) {
        GeoBone bone = model.getBone(boneName).orElse(null);
        if (bone == null) {
            return;
        }

        ResourceLocation texture = renderer.getTextureLocation(stand);

        poseStack.pushPose();
        poseStack.translate(0F, VANILLA_ORIGIN_HEIGHT, 0F);
        poseStack.scale(-1F, -1F, 1F);

        Rest rest = Rest.take(bone);
        try {
            RenderType body = RenderType.entityCutoutNoCull(texture);
            renderer.renderRecursively(poseStack, stand, bone, body, buffers,
                    buffers.getBuffer(body), false, partialTick, light,
                    OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

            glow(poseStack, buffers, light, stand, renderer, bone, texture, partialTick);
        } finally {
            rest.restore(bone);
            poseStack.popPose();
        }
    }

    static void armOn(PoseStack poseStack, MultiBufferSource buffers, int light, StandEntity stand,
                      GeoRenderer<StandEntity> renderer, BakedGeoModel model,
                      ModelPart limb, String boneName,
                      float partialTick) {
        poseStack.pushPose();

        limb.translateAndRotate(poseStack);
        PartPose rest = limb.getInitialPose();
        poseStack.translate(-rest.x / 16F, -rest.y / 16F, -rest.z / 16F);

        arm(poseStack, buffers, light, stand, renderer, model, boneName, partialTick);

        poseStack.popPose();
    }

    private static void glow(PoseStack poseStack, MultiBufferSource buffers, int light,
                             StandEntity stand, GeoRenderer<StandEntity> renderer, GeoBone bone,
                             ResourceLocation texture, float partialTick) {
        if (!StandGlowLayer.hasMask(texture)) {
            return;
        }

        RenderType glowing = AutoGlowingTexture.getRenderType(texture);
        for (float strength : GLOW_PASSES) {
            renderer.renderRecursively(poseStack, stand, bone, glowing, buffers,
                    buffers.getBuffer(glowing), false, partialTick, light,
                    OverlayTexture.NO_OVERLAY, argb(strength));
        }
    }

    private static int argb(float strength) {
        int alpha = (int) (Math.min(strength, 1F) * 255F) & 0xFF;
        return (alpha << 24) | 0x00FFFFFF;
    }

    private record Rest(float rotX, float rotY, float rotZ,
                        float posX, float posY, float posZ, boolean hidden) {
        static Rest take(GeoBone bone) {
            Rest was = new Rest(bone.getRotX(), bone.getRotY(), bone.getRotZ(),
                    bone.getPosX(), bone.getPosY(), bone.getPosZ(), bone.isHidden());

            bone.setRotX(0F);
            bone.setRotY(0F);
            bone.setRotZ(0F);
            bone.setPosX(0F);
            bone.setPosY(0F);
            bone.setPosZ(0F);
            bone.setHidden(false);
            return was;
        }

        void restore(GeoBone bone) {
            bone.setRotX(rotX);
            bone.setRotY(rotY);
            bone.setRotZ(rotZ);
            bone.setPosX(posX);
            bone.setPosY(posY);
            bone.setPosZ(posZ);
            bone.setHidden(hidden);
        }
    }
}
