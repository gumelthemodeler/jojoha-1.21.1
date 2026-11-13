package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.chariot.ChariotBlade;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.GeckoLibCache;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

public final class ChariotBladeRenderer extends EntityRenderer<ChariotBlade> {
    private static final String BLADE_BONE = "Blade";
    private static final float UNIT = 1F / 16F;

    public ChariotBladeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ChariotBlade entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light) {
        StandType chariot = StandTypes.byId(StandTypes.SILVER_CHARIOT_ID);
        BakedGeoModel model = chariot == null ? null : GeckoLibCache.getBakedModels().get(chariot.model());
        GeoBone blade = model == null ? null : model.getBone(BLADE_BONE).orElse(null);
        if (blade == null) {
            super.render(entity, entityYaw, partialTick, poseStack, buffers, light);
            return;
        }

        Vector3f hilt = new Vector3f(blade.getPivotX() * UNIT, blade.getPivotY() * UNIT, blade.getPivotZ() * UNIT);
        Vector3f tip = new Vector3f(hilt);
        float farthest = -1F;
        for (GeoCube cube : blade.getCubes()) {
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) {
                    continue;
                }
                for (GeoVertex vertex : quad.vertices()) {
                    float away = vertex.position().distanceSquared(hilt);
                    if (away > farthest) {
                        farthest = away;
                        tip.set(vertex.position());
                    }
                }
            }
        }
        Vector3f along = new Vector3f(tip).sub(hilt);
        if (along.lengthSquared() < 1.0E-8F) {
            super.render(entity, entityYaw, partialTick, poseStack, buffers, light);
            return;
        }
        along.normalize();
        Vector3f centre = new Vector3f(hilt).add(tip).mul(0.5F);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        poseStack.mulPose(new Quaternionf().rotationTo(along.x, along.y, along.z, 1F, 0F, 0F));
        poseStack.translate(-centre.x, -centre.y, -centre.z);

        ResourceLocation texture = chariot.textureFor(entity.skin());
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose pose = poseStack.last();
        for (GeoCube cube : blade.getCubes()) {
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) {
                    continue;
                }
                Vector3f normal = quad.normal();
                for (GeoVertex vertex : quad.vertices()) {
                    Vector3f at = vertex.position();
                    out.addVertex(pose, at.x(), at.y(), at.z())
                            .setColor(-1)
                            .setUv(vertex.texU(), vertex.texV())
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(pose, normal.x(), normal.y(), normal.z());
                }
            }
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ChariotBlade entity) {
        StandType chariot = StandTypes.byId(StandTypes.SILVER_CHARIOT_ID);
        return chariot == null ? ResourceLocation.withDefaultNamespace("textures/misc/white.png")
                : chariot.textureFor(entity.skin());
    }
}
