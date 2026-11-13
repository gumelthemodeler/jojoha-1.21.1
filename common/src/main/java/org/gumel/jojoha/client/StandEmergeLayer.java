package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.stand.StandEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public final class StandEmergeLayer extends GeoRenderLayer<StandEntity> {
    private static final int COPIES = 4;
    private static final int STEP_COPIES = 5;
    private static final float STEP_PEAK_ALPHA = 0.6F;
    private static final float PEAK_ALPHA = 0.5F;
    private static final double LEAST_GAP = 0.05;

    public StandEmergeLayer(GeoRenderer<StandEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, StandEntity animatable, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                       float partialTick, int packedLight, int packedOverlay) {
        float share = SummonFlair.emergeShare(animatable, partialTick);
        Player owner = animatable.getOwner();
        if (share < 0F || owner == null || animatable.isManifesting() || !SummonFlair.ghosts(animatable)) {
            return;
        }
        float view = StandViewAlpha.of(animatable);
        if (view <= 0.001F) {
            return;
        }

        double trail = SummonFlair.trail(animatable, partialTick);
        double dx = (Mth.lerp(partialTick, owner.xo, owner.getX()) - Mth.lerp(partialTick, animatable.xo, animatable.getX())) * trail;
        double dy = (Mth.lerp(partialTick, owner.yo, owner.getY()) - Mth.lerp(partialTick, animatable.yo, animatable.getY())) * trail;
        double dz = (Mth.lerp(partialTick, owner.zo, owner.getZ()) - Mth.lerp(partialTick, animatable.zo, animatable.getZ())) * trail;
        if (dx * dx + dy * dy + dz * dz < LEAST_GAP * LEAST_GAP) {
            return;
        }
        float turn = (180F - SummonFlair.yaw(animatable, partialTick,
                Mth.rotLerp(partialTick, animatable.yBodyRotO, animatable.yBodyRot))) * Mth.DEG_TO_RAD;
        double localX = dx * Mth.cos(turn) - dz * Mth.sin(turn);
        double localZ = dx * Mth.sin(turn) + dz * Mth.cos(turn);

        RenderType ghost = RenderType.entityTranslucent(getTextureResource(animatable));
        int tint = SummonFlair.emergeColour(animatable) & 0xFFFFFF;
        boolean stepping = SummonFlair.leavesShell(animatable);
        int copies = stepping ? STEP_COPIES : COPIES;
        float fade = stepping ? STEP_PEAK_ALPHA * (1F - share * share) : PEAK_ALPHA * (1F - share);
        for (int i = 1; i <= copies; i++) {
            float back = stepping ? (float) i / STEP_COPIES : (float) i / (COPIES + 1);
            float alpha = view * fade * (1F - back * 0.6F);
            int colour = ((int) (Mth.clamp(alpha, 0F, 1F) * 255F) << 24) | tint;

            poseStack.pushPose();
            poseStack.translate(localX * back, dy * back, localZ * back);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, ghost,
                    bufferSource.getBuffer(ghost), partialTick, packedLight, packedOverlay, colour);
            poseStack.popPose();
        }
    }
}
