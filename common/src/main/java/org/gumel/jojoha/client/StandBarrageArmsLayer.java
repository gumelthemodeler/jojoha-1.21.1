package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandShadowForm;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.List;

public final class StandBarrageArmsLayer extends GeoRenderLayer<StandEntity> {
    private static final List<String> BARRAGE_ARM_BONES = List.of(
            "barrage_arm_right", "barrage_arm_right2", "barrage_arm_right3",
            "barrage_arm_left", "barrage_arm_left2", "barrage_arm_left3",
            "barrage_arm_right4", "barrage_arm_right5", "barrage_arm_right6",
            "barrage_arm_left4", "barrage_arm_left5", "barrage_arm_left6");

    private static final float BODY_ALPHA = 0.3F;

    private static final float BLOOM_ALPHA = 0.08F;

    private static final float FOREARM_ALPHA = 0.8F;

    private static final int SMEAR_COPIES = 3;

    private static final float SMEAR_PER_COPY = -1F / 2.5F;

    private static final double SMEAR_DEADZONE = 0.2;

    private static final float SMEAR_NEAREST_ALPHA = 0.2F;

    private static final float SHADOW_COAT_ALPHA = 0.15F;

    private static final float[][] SHADOW_GLOW_SHELLS = {{1F, 0.6F}, {1.25F, 0.28F}, {1.55F, 0.12F}};

    private static final float SHADOW_SMEAR_GLOW = 1.6F;

    private static final float SHADOW_SMEAR_SWELL = 0.05F;

    private static final float[] TRAIL_FADES = {0.4F, 0.22F, 0.12F};

    private static final float RIM_GROW = 1.2F;

    private static final float RIM_ALPHA = 0.09F;

    private static final float FLASH_ALPHA = 0.35F;

    private static final float FLASH_WHITEN = 0.5F;

    public StandBarrageArmsLayer(GeoRenderer<StandEntity> renderer) {
        super(renderer);
    }

    @Override
    public void preRender(PoseStack poseStack, StandEntity animatable, BakedGeoModel bakedModel, RenderType renderType,
                          MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight,
                          int packedOverlay) {
        BarrageGhosts.begin(animatable, partialTick);
    }

    @Override
    public void renderForBone(PoseStack poseStack, StandEntity animatable, GeoBone bone,
                              RenderType renderType, MultiBufferSource bufferSource,
                              VertexConsumer buffer, float partialTick, int packedLight,
                              int packedOverlay) {

        BarrageGhosts.rig(bone, poseStack);
        int copy = trailCopy(bone);
        if (!animatable.isBarraging() || (copy == 0 && !partOfArm(bone))) {
            return;
        }

        ResourceLocation texture = getRenderer().getTextureLocation(animatable);
        int colour = StandShadowForm.auraColourFor(animatable);
        float opacity = JojohaClientConfig.BARRAGE_TRAIL_OPACITY.value();
        float viewAlpha = StandViewAlpha.of(animatable)
                * (copy == 0 ? 1F : TRAIL_FADES[Math.min(copy, TRAIL_FADES.length) - 1] * opacity);
        StandShadowForm shadow = StandShadowForm.of(animatable);
        float fade = BarrageGlow.fade(animatable, partialTick);
        float glow = BarrageGlow.glow(animatable, partialTick);

        bone.setHidden(false);
        GeoBone forearm = getRenderer().getGeoModel().getBone(BarrageGhosts.forearmName(bone.getName())).orElse(null);

        if (shadow != null) {
            shadowArm(poseStack, animatable, bone, bufferSource, texture, shadow, packedOverlay, viewAlpha * fade,
                    viewAlpha * glow);
        } else {
            RenderType body = RenderType.entityTranslucentEmissive(texture);
            int bodyTint = argb(colour, BODY_ALPHA * viewAlpha * fade);
            if (!BarrageGhosts.forearm(getRenderer(), poseStack, bone, forearm, bufferSource, body, bodyTint,
                    argb(0xFFFFFF, FOREARM_ALPHA * viewAlpha * fade), packedOverlay)) {
                getRenderer().renderCubesOfBone(poseStack, bone, bufferSource.getBuffer(body),
                        LightTexture.FULL_BRIGHT, packedOverlay, bodyTint);
            }

            RenderType bloom = RenderType.eyes(texture);
            int bloomTint = BarrageGlow.additive(colour, BLOOM_ALPHA * viewAlpha * glow);
            if (!BarrageGhosts.forearm(getRenderer(), poseStack, bone, forearm, bufferSource, bloom, bloomTint,
                    bloomTint, packedOverlay)) {
                getRenderer().renderCubesOfBone(poseStack, bone, bufferSource.getBuffer(bloom),
                        LightTexture.FULL_BRIGHT, packedOverlay, bloomTint);
            }

            if (copy == 0) {
                aura(poseStack, bone, bufferSource, texture, colour, packedOverlay,
                        RIM_ALPHA * opacity * viewAlpha * glow,
                        StandModel.flash(bone) * FLASH_ALPHA * viewAlpha * glow);
            }
        }

        if (copy == 0) {
            BarrageGhosts.record(bone, shadow == null ? forearm : null, poseStack);
        }

        bone.setHidden(true);
        bone.setChildrenHidden(false);
    }

    private static int trailCopy(GeoBone bone) {
        String name = bone.getName();
        int at = name.lastIndexOf(StandModel.TRAIL);
        if (at < 0 || at + StandModel.TRAIL.length() != name.length() - 1) {
            return 0;
        }
        char digit = name.charAt(name.length() - 1);
        return digit >= '1' && digit <= '9' ? digit - '0' : 0;
    }

    private void aura(PoseStack poseStack, GeoBone bone, MultiBufferSource bufferSource, ResourceLocation texture,
                      int colour, int packedOverlay, float rim, float flash) {
        RenderType glow = RenderType.eyes(texture);
        if (rim > 0F) {
            Vec3 centre = centreOf(bone);
            poseStack.pushPose();
            poseStack.translate(centre.x, centre.y, centre.z);
            poseStack.scale(RIM_GROW, RIM_GROW, RIM_GROW);
            poseStack.translate(-centre.x, -centre.y, -centre.z);
            getRenderer().renderCubesOfBone(poseStack, bone, bufferSource.getBuffer(glow),
                    LightTexture.FULL_BRIGHT, packedOverlay, BarrageGlow.additive(colour, rim));
            poseStack.popPose();
        }
        if (flash > 0F) {
            getRenderer().renderCubesOfBone(poseStack, bone, bufferSource.getBuffer(glow),
                    LightTexture.FULL_BRIGHT, packedOverlay, BarrageGlow.additive(whiter(colour), flash));
        }
    }

    private static int whiter(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        red += Math.round((255 - red) * FLASH_WHITEN);
        green += Math.round((255 - green) * FLASH_WHITEN);
        blue += Math.round((255 - blue) * FLASH_WHITEN);
        return (red << 16) | (green << 8) | blue;
    }

    private static boolean partOfArm(GeoBone bone) {
        for (GeoBone at = bone; at != null; at = at.getParent()) {
            if (BARRAGE_ARM_BONES.contains(at.getName())) {
                return true;
            }
        }
        return false;
    }

    private void shadowArm(PoseStack poseStack, StandEntity animatable, GeoBone bone,
                           MultiBufferSource bufferSource, ResourceLocation texture,
                           StandShadowForm shadow, int packedOverlay, float viewAlpha,
                           float glowAlpha) {
        if (!(getRenderer() instanceof StandRenderer stand)) {
            return;
        }
        BakedGeoModel model = getRenderer().getGeoModel().getBakedModel(
                getRenderer().getGeoModel().getModelResource(animatable));
        Matrix4f frame = stand.standFrame();

        RenderType coat = RenderType.entityTranslucentEmissive(texture);
        getRenderer().renderCubesOfBone(poseStack, bone,
                StandShadowGlowLayer.gradient(bufferSource.getBuffer(coat), frame, model, animatable, shadow,
                        SHADOW_COAT_ALPHA * viewAlpha, SHADOW_COAT_ALPHA * viewAlpha, 1F),
                LightTexture.FULL_BRIGHT, packedOverlay, 0xFFFFFFFF);

        Vec3 centre = centreOf(bone);
        RenderType glow = RenderType.eyes(texture);
        for (int shell = 0; shell < SHADOW_GLOW_SHELLS.length; shell++) {
            float grow = SHADOW_GLOW_SHELLS[shell][0];
            poseStack.pushPose();
            poseStack.translate(centre.x, centre.y, centre.z);
            poseStack.scale(grow, grow, grow);
            poseStack.translate(-centre.x, -centre.y, -centre.z);
            getRenderer().renderCubesOfBone(poseStack, bone,
                    StandShadowGlowLayer.gradient(bufferSource.getBuffer(glow), frame, model, animatable, shadow,
                            1F, 1F, SHADOW_GLOW_SHELLS[shell][1] * glowAlpha),
                    LightTexture.FULL_BRIGHT, packedOverlay, 0xFFFFFFFF);
            poseStack.popPose();
        }
    }

    private static Vec3 centreOf(GeoBone bone) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (GeoCube cube : bone.getCubes()) {
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) {
                    continue;
                }
                for (GeoVertex vertex : quad.vertices()) {
                    Vector3f p = vertex.position();
                    minX = Math.min(minX, p.x());
                    minY = Math.min(minY, p.y());
                    minZ = Math.min(minZ, p.z());
                    maxX = Math.max(maxX, p.x());
                    maxY = Math.max(maxY, p.y());
                    maxZ = Math.max(maxZ, p.z());
                }
            }
        }
        if (minX > maxX) {
            return Vec3.ZERO;
        }
        return new Vec3((minX + maxX) / 2F, (minY + maxY) / 2F, (minZ + maxZ) / 2F);
    }

    @Override
    public void render(PoseStack poseStack, StandEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {

        if (!animatable.isBarraging()) {
            return;
        }

        ResourceLocation texture = getRenderer().getTextureLocation(animatable);
        int colour = StandShadowForm.auraColourFor(animatable);
        BarrageGhosts.draw(getRenderer(), poseStack, animatable, bakedModel, bufferSource, texture, colour, partialTick,
                packedOverlay);

        Vec3 travel = new Vec3(
                animatable.getX() - animatable.xo,
                animatable.getY() - animatable.yo,
                animatable.getZ() - animatable.zo);
        if (travel.lengthSqr() < 1.0E-6) {
            return;
        }

        double lift = Math.abs(travel.y) < SMEAR_DEADZONE ? 0 : travel.y;
        float viewAlpha = StandViewAlpha.of(animatable);
        float fade = BarrageGlow.fade(animatable, partialTick);
        float glowing = BarrageGlow.glow(animatable, partialTick);

        StandShadowForm shadow = StandShadowForm.of(animatable);
        if (shadow != null) {
            RenderType glow = RenderType.eyes(getRenderer().getTextureLocation(animatable));
            double middle = animatable.getBbHeight() / 2.0;
            for (int copy = 0; copy < SMEAR_COPIES; copy++) {
                float back = (copy + 1) * SMEAR_PER_COPY;
                float nearness = SMEAR_NEAREST_ALPHA * (SMEAR_COPIES - copy) / SMEAR_COPIES;
                float swell = 1F + SHADOW_SMEAR_SWELL * (copy + 1);

                poseStack.pushPose();
                poseStack.translate(travel.x * back, lift * back, travel.z * back);
                Matrix4f frame = new Matrix4f(poseStack.last().pose());
                poseStack.translate(0, middle, 0);
                poseStack.scale(swell, swell, swell);
                poseStack.translate(0, -middle, 0);
                getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow,
                        StandShadowGlowLayer.gradient(bufferSource.getBuffer(glow), frame, bakedModel,
                                animatable, shadow, 1F, 1F, nearness * SHADOW_SMEAR_GLOW * viewAlpha * glowing),
                        partialTick, LightTexture.FULL_BRIGHT, packedOverlay, 0xFFFFFFFF);
                poseStack.popPose();
            }
            return;
        }

        RenderType ghost = RenderType.entityTranslucent(getRenderer().getTextureLocation(animatable));
        VertexConsumer ghostBuffer = bufferSource.getBuffer(ghost);

        for (int copy = 0; copy < SMEAR_COPIES; copy++) {
            float back = (copy + 1) * SMEAR_PER_COPY;

            float nearness = SMEAR_NEAREST_ALPHA * (SMEAR_COPIES - copy) / SMEAR_COPIES;

            poseStack.pushPose();
            poseStack.translate(travel.x * back, lift * back, travel.z * back);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, ghost,
                    ghostBuffer, partialTick, packedLight, packedOverlay,
                    argb(colour, nearness * viewAlpha * fade));
            poseStack.popPose();
        }
    }

    private static int argb(int rgb, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255F), 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
