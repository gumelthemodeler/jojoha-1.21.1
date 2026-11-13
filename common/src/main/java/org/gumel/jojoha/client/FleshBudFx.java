package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

public final class FleshBudFx {
    private static final float BROW_LIFT = 0.10F;

    private static final float BROW_FORWARD = 0.42F;

    private static final float SCALE = 0.85F;

    private static final float PULSE = 0.06F;
    private static final float PULSE_RATE = 0.18F;

    private static final Map<Integer, Long> BUDDED = new HashMap<>();

    private static ModelPart baked;
    private static FleshBudModel model;

    private FleshBudFx() {
    }

    public static void set(int entityId, int ticks, long now) {
        if (ticks <= 0) {
            BUDDED.remove(entityId);
            return;
        }
        BUDDED.put(entityId, now + ticks);
    }

    public static void clear() {
        BUDDED.clear();
    }

    public static void bake(ModelPart root) {
        baked = root;
        model = new FleshBudModel(root);
    }

    public static void render(LivingEntity entity, PoseStack poseStack, MultiBufferSource buffers,
                              int light, float partialTick) {
        if (model == null || BUDDED.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Long until = BUDDED.get(entity.getId());
        if (until == null) {
            return;
        }

        long now = minecraft.level.getGameTime();
        if (now >= until) {
            BUDDED.remove(entity.getId());
            return;
        }

        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        float breath = 1F + Mth.sin(((float) now + partialTick) * PULSE_RATE) * PULSE;

        poseStack.pushPose();

        poseStack.translate(0.0F, entity.getEyeHeight() + entity.getBbHeight() * BROW_LIFT, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - headYaw));
        poseStack.translate(0.0F, 0.0F, -entity.getBbWidth() * BROW_FORWARD);

        poseStack.scale(-SCALE * breath, -SCALE * breath, SCALE * breath);

        model.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(
                        FleshBudModel.TEXTURE)),
                light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);

        poseStack.popPose();
    }
}
