package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.block.HeadJarBlock;
import org.gumel.jojoha.block.HeadJarBlockEntity;

import java.util.UUID;

public final class HeadJarRenderer implements BlockEntityRenderer<HeadJarBlockEntity> {
    private static final float HEAD_SCALE = 0.75F;
    private static final float HEAD_LIFT = 0.07F;

    public HeadJarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HeadJarBlockEntity box, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int light, int overlay) {
        UUID head = box.occupant();
        if (head == null) {
            return;
        }
        float yaw = box.getBlockState().getValue(HeadJarBlock.FACING).toYRot();
        Minecraft minecraft = Minecraft.getInstance();
        Player occupant = minecraft.level == null ? null : minecraft.level.getPlayerByUUID(head);
        if (occupant != null) {
            yaw = Mth.rotLerp(partialTick, occupant.yHeadRotO, occupant.yHeadRot);
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, HEAD_LIFT, 0.5F);
        poseStack.scale(HEAD_SCALE, HEAD_SCALE, HEAD_SCALE);
        HeadlessView.drawStill(poseStack, buffers, light, HeadlessView.skin(head), yaw);
        poseStack.popPose();
    }
}
