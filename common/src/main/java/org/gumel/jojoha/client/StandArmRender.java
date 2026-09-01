package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
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

/**
 * Draws one arm of a bound Stand into a frame that is already in vanilla model space.
 *
 * <p>Two callers, and the only difference between them is which frame they hand over. The layer on
 * the player model enters the posed arm first, so the vines rotate with it; the first-person pass
 * hands over the hand-render frame. Everything after that - the two coordinate systems disagreeing,
 * the mirrored bone, neutralising a shared pose, the glow - is the same job, and is written here
 * once rather than twice.
 *
 * <h2>Getting the two coordinate systems to agree</h2>
 *
 * <p>Vanilla's model space and GeckoLib's are not the same space, and both differences were worked
 * out from the actual transforms rather than guessed.
 *
 * <p><b>Y is upside down between them.</b> Vanilla measures downward from a point 24 units above the
 * feet - which is why a player's head cube has negative Y - while GeckoLib measures upward from the
 * feet. A geo point at height {@code g} is a vanilla point at {@code 24 - g}, which is the translate
 * and the Y flip below.
 *
 * <p><b>X is mirrored between them.</b> GeckoLib's {@code RenderUtil.translateMatrixToBone} negates
 * X on its way to every bone and bakes the same negation into the cube vertices. Vanilla does not.
 *
 * <p>Correcting Y alone would leave a transform with a negative determinant - one flipped axis - and
 * that reverses triangle winding, so the arm would be inside out: back faces toward the camera, so
 * culled away on the glow pass and lit off reversed normals on the other. Flipping X as well
 * restores it, and the model gives the flip back for free: the two arms are exact mirrors on
 * identical UVs, with the geo file marking one {@code mirror: true} of the other. Drawing the
 * opposite arm through an X flip reproduces the one asked for exactly, texture included - which is
 * why callers pass the bone from {@link StandModel#BONE_ON_RIGHT} rather than the obvious name.
 */
final class StandArmRender {
    /**
     * Where vanilla's model origin sits above the feet, in blocks.
     *
     * <p>24 units. The same number {@code LivingEntityRenderer} translates by when it puts a model
     * on an entity, and the pivot of the whole Y correction.
     */
    private static final float VANILLA_ORIGIN_HEIGHT = 1.5F;

    /** Additive passes stacked to carry the glow into daylight, brightest first. */
    private static final float[] GLOW_PASSES = {1.0F, 0.7F, 0.45F};

    private StandArmRender() {
    }

    /** The renderer for a Stand, if it is one of GeckoLib's, or null. */
    @SuppressWarnings("unchecked")
    static GeoRenderer<StandEntity> rendererFor(Minecraft minecraft, StandEntity stand) {
        EntityRenderer<?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(stand);
        return renderer instanceof GeoRenderer<?> geo ? (GeoRenderer<StandEntity>) geo : null;
    }

    /** The baked model this Stand is drawn from, or null if resources are still loading. */
    static BakedGeoModel modelFor(GeoRenderer<StandEntity> renderer, StandEntity stand) {
        return renderer.getGeoModel()
                .getBakedModel(renderer.getGeoModel().getModelResource(stand, renderer));
    }

    /**
     * Draws one arm bone, assuming the pose stack is in a vanilla model-space frame.
     *
     * @param boneName the geo bone to draw, which is the opposite-handed one - see the class note
     */
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

    /**
     * The lit pass, from the Stand's own glow mask.
     *
     * <p>A Stand shipping no mask simply gets no pass, and is checked for: GeckoLib's own lookup
     * calls {@code Optional.get} on the missing file and takes the whole draw down with it.
     */
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

    /** White at a given strength, which under an additive pass is how bright it burns. */
    private static int argb(float strength) {
        int alpha = (int) (Math.min(strength, 1F) * 255F) & 0xFF;
        return (alpha << 24) | 0x00FFFFFF;
    }

    /**
     * The bone's pose, borrowed and put back.
     *
     * <p>It has to be neutralised rather than used as found. The bones of a baked model are shared
     * by every Stand using that file, so what is on them is whatever the last one rendered left
     * behind - and the arms are now posed by the player's own model rather than by an animation, so
     * anything still sitting on the bone is a second rotation on top of the right one.
     *
     * <p>Put back afterwards because anything else rendering from this model expects to find what it
     * left.
     */
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
