package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class PilotBody {
    private static boolean drawnByLevel;

    private PilotBody() {
    }

    public static void markDrawnByLevel() {
        drawnByLevel = true;
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                              float partialTick) {
        boolean already = drawnByLevel;
        drawnByLevel = false;

        if (already || PossessionClient.merged() || !(StandSkillInput.isPiloting() || PossessionClient.possessing())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.getCameraEntity() == player || player.isSpectator()) {
            return;
        }

        Vec3 eye = camera.getPosition();
        double x = Mth.lerp(partialTick, player.xo, player.getX()) - eye.x;
        double y = Mth.lerp(partialTick, player.yo, player.getY()) - eye.y;
        double z = Mth.lerp(partialTick, player.zo, player.getZ()) - eye.z;
        float yaw = Mth.rotLerp(partialTick, player.yRotO, player.getYRot());

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();

        poseStack.pushPose();
        dispatcher.render(player, x, y, z, yaw, partialTick, poseStack, buffers,
                dispatcher.getPackedLightCoords(player, partialTick));
        poseStack.popPose();
    }
}
