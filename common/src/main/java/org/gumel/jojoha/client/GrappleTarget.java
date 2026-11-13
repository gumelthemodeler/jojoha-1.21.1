package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.grapple.GrappleAim;
import org.joml.Matrix4f;

public final class GrappleTarget {
    private static final ResourceLocation MARK =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                    "textures/particle/hermit_purple_block_target.png");

    private static final float SIZE = 0.85F;
    private static final double LIFT = 0.04;

    private static final float PULSE_RATE = 3.1F;
    private static final float ALPHA_MIN = 0.55F;
    private static final float ALPHA_MAX = 1.0F;

    private static BlockHitResult aim;

    private GrappleTarget() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;

        aim = null;
        if (player == null || level == null || !holdsHermitPurple(player)) {
            return;
        }

        if (GrappleController.active() != null) {
            return;
        }

        aim = GrappleAim.find(player);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                              float partialTick) {
        BlockHitResult at = aim;
        if (at == null) {
            return;
        }

        Vec3 cameraPos = camera.getPosition();

        Vec3 normal = Vec3.atLowerCornerOf(at.getDirection().getNormal());
        Vec3 spot = at.getLocation().add(normal.scale(LIFT)).subtract(cameraPos);

        float seconds = (System.currentTimeMillis() % 100000L) / 1000F;
        float alpha = Mth.lerp((Mth.sin(seconds * PULSE_RATE) + 1F) * 0.5F, ALPHA_MIN, ALPHA_MAX);

        poseStack.pushPose();
        poseStack.translate(spot.x, spot.y, spot.z);

        poseStack.mulPose(camera.rotation());
        poseStack.scale(SIZE, SIZE, SIZE);

        VertexConsumer buffer = buffers.getBuffer(RenderType.textSeeThrough(MARK));
        Matrix4f pose = poseStack.last().pose();
        quad(buffer, pose, -0.5F, -0.5F, 0F, 1F, alpha);
        quad(buffer, pose, 0.5F, -0.5F, 1F, 1F, alpha);
        quad(buffer, pose, 0.5F, 0.5F, 1F, 0F, alpha);
        quad(buffer, pose, -0.5F, 0.5F, 0F, 0F, alpha);

        poseStack.popPose();

        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
    }

    private static void quad(VertexConsumer buffer, Matrix4f pose,
                             float x, float y, float u, float v, float alpha) {
        buffer.addVertex(pose, x, y, 0F)
                .setColor(1F, 1F, 1F, alpha)
                .setUv(u, v)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    private static boolean holdsHermitPurple(LocalPlayer player) {
        var data = ClientPlayerDataCache.data;
        return data != null && data.standSummoned && data.stand.isPresent()
                && StandTypes.HERMIT_PURPLE_ID.equals(data.stand.standId());
    }
}
