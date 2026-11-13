package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.StandShadowForm;
import org.gumel.jojoha.stand.passive.SensoryPerceptionPassive;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public final class LootSense {
    private static double range() {
        return SensoryPerceptionPassive
                .rangeFor(ClientPlayerDataCache.data);
    }

    private static final int SCAN_INTERVAL = 10;

    private static final float GLOW_ALPHA_MIN = 0.16F;
    private static final float GLOW_ALPHA_MAX = 0.38F;

    private static final double GLOW_SWELL = 0.035;

    private static final ResourceLocation EYE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                    "textures/particle/lootable_seen.png");

    private static final float EYE_HEIGHT = 0.5F;

    private static final float EYE_SIZE = 0.72F;

    private static final float EYE_ALPHA_MIN = 0.62F;
    private static final float EYE_ALPHA_MAX = 1.0F;

    private static final float PULSE_RATE = 2.4F;
    private static final float PULSE_SPREAD = 0.9F;

    private static final List<BlockPos> FOUND = new ArrayList<>();
    private static int sinceScan;

    private LootSense() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;

        if (level == null || minecraft.player == null || !active()) {
            FOUND.clear();
            return;
        }

        if (--sinceScan > 0) {
            return;
        }
        sinceScan = SCAN_INTERVAL;

        FOUND.clear();
        Vec3 eye = minecraft.player.position();
        BlockPos centre = minecraft.player.blockPosition();

        int fromX = SectionPos.blockToSectionCoord(centre.getX() - (int) range());
        int toX = SectionPos.blockToSectionCoord(centre.getX() + (int) range());
        int fromZ = SectionPos.blockToSectionCoord(centre.getZ() - (int) range());
        int toZ = SectionPos.blockToSectionCoord(centre.getZ() + (int) range());

        for (int x = fromX; x <= toX; x++) {
            for (int z = fromZ; z <= toZ; z++) {
                LevelChunk chunk = level.getChunkSource().getChunk(x, z, false);
                if (chunk == null) {
                    continue;
                }

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof Container)) {
                        continue;
                    }

                    BlockPos pos = blockEntity.getBlockPos();
                    if (pos.getCenter().distanceToSqr(eye) <= range() * range()) {
                        FOUND.add(pos.immutable());
                    }
                }
            }
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                              float partialTick) {
        if (FOUND.isEmpty() || !active()) {
            return;
        }

        Vec3 cameraPos = camera.getPosition();
        float[] tint = tint();

        float seconds = (System.currentTimeMillis() % 100000L) / 1000F;

        for (BlockPos pos : FOUND) {
            float breath = breathAt(pos, seconds);

            AABB box = new AABB(pos).inflate(GLOW_SWELL)
                    .move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            GlowBox.fill(poseStack, buffers.getBuffer(RenderType.lightning()), box, tint,
                    Mth.lerp(breath, GLOW_ALPHA_MIN, GLOW_ALPHA_MAX));

            drawEye(poseStack, buffers, camera, cameraPos, pos, breath);
        }

        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
    }

    private static void drawEye(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                                Vec3 cameraPos, BlockPos pos, float breath) {
        float alpha = Mth.lerp(breath, EYE_ALPHA_MIN, EYE_ALPHA_MAX);

        double x = pos.getX() + 0.5 - cameraPos.x;
        double y = pos.getY() + EYE_HEIGHT - cameraPos.y;
        double z = pos.getZ() + 0.5 - cameraPos.z;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(EYE_SIZE, EYE_SIZE, EYE_SIZE);

        VertexConsumer buffer = buffers.getBuffer(RenderType.textSeeThrough(EYE));
        Matrix4f pose = poseStack.last().pose();

        quad(buffer, pose, -0.5F, -0.5F, 0F, 1F, alpha);
        quad(buffer, pose, 0.5F, -0.5F, 1F, 1F, alpha);
        quad(buffer, pose, 0.5F, 0.5F, 1F, 0F, alpha);
        quad(buffer, pose, -0.5F, 0.5F, 0F, 0F, alpha);

        poseStack.popPose();
    }

    private static float breathAt(BlockPos pos, float seconds) {
        float phase = (pos.getX() * 0.7F + pos.getY() * 1.3F + pos.getZ() * 0.4F) * PULSE_SPREAD;
        return (Mth.sin(seconds * PULSE_RATE + phase) + 1F) * 0.5F;
    }

    private static void quad(VertexConsumer buffer, Matrix4f pose,
                             float x, float y, float u, float v, float alpha) {
        buffer.addVertex(pose, x, y, 0F)
                .setColor(1F, 1F, 1F, alpha)
                .setUv(u, v)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    private static boolean active() {
        return StandPassives.grants(ClientPlayerDataCache.data, SensoryPerceptionPassive.ID);
    }

    private static float[] tint() {
        int colour = StandShadowForm.auraColourFor(ClientPlayerDataCache.data.stand);
        return new float[]{
                ((colour >> 16) & 0xFF) / 255F,
                ((colour >> 8) & 0xFF) / 255F,
                (colour & 0xFF) / 255F};
    }
}
