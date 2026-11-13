package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.StandEntity;
import org.joml.Matrix4f;

public final class StandGuardPlate {
    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/particle/" + name);
    }

    private static final ResourceLocation CRACKING = texture("break.png");
    private static final ResourceLocation SHATTERED = texture("broken.png");

    private static final float OFFSET = 0.5F;

    private static final float CENTRE_HEIGHT = 1.05F;
    private static final float HALF_WIDTH = 0.8F;
    private static final float HALF_HEIGHT = 0.95F;

    private static final float SHOW_FROM = 0.45F;

    private static final float MAX_ALPHA = 0.72F;

    private static final float PULSE_DEPTH = 0.22F;
    private static final float PULSE_SPEED = 0.9F;

    private static final float[] CRACK_TINT = {0.78F, 0.90F, 1.0F};
    private static final float[] BREAK_TINT = {1.0F, 0.96F, 0.86F};

    private StandGuardPlate() {
    }

    public static void render(StandEntity stand, float partialTick, PoseStack poseStack,
                              MultiBufferSource bufferSource) {
        float viewAlpha = StandViewAlpha.of(stand);
        if (viewAlpha <= 0.01F) {
            return;
        }

        int brokenTicks = stand.guardBrokenTicks();
        if (brokenTicks > 0) {
            float remaining = brokenTicks / (float) StandEntity.guardBrokenFlashTicks();
            draw(stand, partialTick, poseStack, bufferSource, SHATTERED, BREAK_TINT,
                    remaining * viewAlpha);
            return;
        }

        if (!stand.isGuarding()) {
            return;
        }

        float strain = stand.guardStrain();
        if (strain < SHOW_FROM) {
            return;
        }

        float into = (strain - SHOW_FROM) / (1F - SHOW_FROM);
        float wave = Mth.sin((stand.tickCount + partialTick) * PULSE_SPEED);
        float alpha = MAX_ALPHA * into * (1F + wave * PULSE_DEPTH * into);

        draw(stand, partialTick, poseStack, bufferSource, CRACKING, CRACK_TINT,
                Mth.clamp(alpha, 0F, 1F) * viewAlpha);
    }

    private static void draw(StandEntity stand, float partialTick, PoseStack poseStack,
                             MultiBufferSource bufferSource, ResourceLocation sheet,
                             float[] tint, float alpha) {
        float yaw = Mth.rotLerp(partialTick, stand.yRotO, stand.getYRot());
        double radians = Math.toRadians(yaw);

        Vec3 forward = new Vec3(-Math.sin(radians), 0, Math.cos(radians));
        Vec3 right = new Vec3(forward.z, 0, -forward.x);

        Vec3 centre = forward.scale(OFFSET).add(0, CENTRE_HEIGHT, 0);
        Vec3 across = right.scale(HALF_WIDTH);

        RenderType type = RenderType.entityTranslucent(sheet);
        VertexConsumer buffer = bufferSource.getBuffer(type);
        Matrix4f pose = poseStack.last().pose();

        corner(buffer, pose, centre, across, -1, +1, 0F, 0F, tint, alpha, forward);
        corner(buffer, pose, centre, across, -1, -1, 0F, 1F, tint, alpha, forward);
        corner(buffer, pose, centre, across, +1, -1, 1F, 1F, tint, alpha, forward);
        corner(buffer, pose, centre, across, +1, +1, 1F, 0F, tint, alpha, forward);
    }

    private static void corner(VertexConsumer buffer, Matrix4f pose, Vec3 centre, Vec3 across,
                               int side, int vertical, float u, float v, float[] tint, float alpha,
                               Vec3 normal) {
        double x = centre.x + across.x * side;
        double y = centre.y + HALF_HEIGHT * vertical;
        double z = centre.z + across.z * side;

        buffer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(tint[0], tint[1], tint[2], alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }
}
