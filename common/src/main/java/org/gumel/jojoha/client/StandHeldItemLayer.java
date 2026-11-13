package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.stand.StandEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

import java.util.HashMap;
import java.util.Map;

public final class StandHeldItemLayer extends BlockAndItemGeoLayer<StandEntity> {
    private static final String HAND_BONE = "bone2";

    static final float HAND_OFFSET = 6.5F / 16F;

    static final float HAND_REACH_OUT = 1.4F / 16F;

    private static final float HAND_SIDE = 1F / 16F;

    private static final float HAND_FORWARD = 2F / 16F;

    private static final float HAND_DOWN = 10F / 16F;

    private static final float CAUGHT_YAW = 90F;

    private static final float CAUGHT_PITCH = 90F;

    private static final Map<Integer, Held> CAUGHT = new HashMap<>();

    private static final int CAUGHT_KEPT = 32;

    private record Held(CompoundTag tag, Entity entity) {
    }

    public StandHeldItemLayer(GeoRenderer<StandEntity> renderer) {
        super(renderer);
    }

    @Override
    protected ItemStack getStackForBone(GeoBone bone, StandEntity stand) {
        return HAND_BONE.equals(bone.getName()) ? stand.getItemBySlot(EquipmentSlot.MAINHAND) : null;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack,
                                                          StandEntity stand) {
        return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    @Override
    public void renderForBone(PoseStack poseStack, StandEntity stand, GeoBone bone, RenderType renderType,
                              MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                              int packedLight, int packedOverlay) {
        if (!HAND_BONE.equals(bone.getName())) {
            return;
        }
        Entity caught = caught(stand);
        ItemStack stack = getStackForBone(bone, stand);
        if (caught == null && (stack == null || stack.isEmpty())) {
            return;
        }
        GeoBone shoulder = bone.getParent() != null ? bone.getParent() : bone;
        poseStack.pushPose();
        poseStack.translate(shoulder.getPivotX() / 16F, shoulder.getPivotY() / 16F, shoulder.getPivotZ() / 16F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180F));
        poseStack.translate(HAND_SIDE, HAND_FORWARD, -HAND_DOWN);
        if (caught != null) {
            Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(caught)
                    .render(caught, CAUGHT_YAW, 0F, poseStack, bufferSource, packedLight);
        } else {
            renderStackForBone(poseStack, bone, stack, stand, bufferSource, partialTick, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static Entity caught(StandEntity stand) {
        CompoundTag tag = stand.caughtProjectile();
        if (tag.isEmpty()) {
            CAUGHT.remove(stand.getId());
            return null;
        }
        Held held = CAUGHT.get(stand.getId());
        if (held == null || !held.tag().equals(tag)) {
            Entity made = EntityType.create(tag, stand.level()).orElse(null);
            if (made != null) {
                made.setYRot(CAUGHT_YAW);
                made.yRotO = CAUGHT_YAW;
                made.setXRot(CAUGHT_PITCH);
                made.xRotO = CAUGHT_PITCH;
                if (made instanceof AbstractArrow arrow) {
                    arrow.shakeTime = 0;
                }
            }
            held = new Held(tag, made);
            if (CAUGHT.size() > CAUGHT_KEPT) {
                CAUGHT.clear();
            }
            CAUGHT.put(stand.getId(), held);
        }
        return held.entity();
    }
}
