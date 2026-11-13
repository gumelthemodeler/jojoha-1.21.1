package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.gumel.jojoha.client.fx.ImpactDirector;
import org.gumel.jojoha.client.pillar.PillarClient;

public final class WorldVfx {
    private static WorldVfxBuffers buffers;
    private static WorldVfxBuffers afterEntities;

    private WorldVfx() {
    }

    private static WorldVfxBuffers buffers() {
        if (buffers == null) {
            buffers = WorldVfxBuffers.create();
        }
        return buffers;
    }

    public static MultiBufferSource afterEntities(
            MultiBufferSource asked) {
        if (asked != Minecraft.getInstance().renderBuffers().bufferSource()) {
            return asked;
        }
        if (afterEntities == null) {
            afterEntities = WorldVfxBuffers.create();
        }
        return afterEntities;
    }

    public static void solid(PoseStack poseStack, Camera camera, float partialTick) {
        if (afterEntities != null) {
            afterEntities.endBatch();
        }
        WorldVfxBuffers buffers = buffers();
        buffers.hold(true);

        ThornLashFx.render(poseStack, buffers, camera, partialTick);
        DowsingFx.solid(poseStack, buffers, camera, partialTick);
        GroundBreakerFx.render(poseStack, buffers, camera, partialTick);
        ClackerClient.render(poseStack, buffers, camera, partialTick);
        ScarfClient.render(poseStack, buffers, camera, partialTick);
        PillarClient.render(poseStack, buffers, camera, partialTick);

        buffers.endBatch();
        buffers.hold(false);
    }

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        WorldVfxBuffers buffers = buffers();
        buffers.hold(false);

        if (!DreamPost.deferred()) {
            DreamWater.render(poseStack, camera, partialTick);
        }
        StandPlacePreview.render(poseStack, buffers, camera, partialTick);
        GrappleTarget.render(poseStack, buffers, camera, partialTick);
        LockOn.render(poseStack, buffers, camera, partialTick);
        SabreArcFx.render(poseStack, buffers, camera, partialTick);
        ZoomPunchArm.render(poseStack, buffers, camera, partialTick);
        SpaceRipperBeam.render(poseStack, buffers, camera, partialTick);
        HeavenWorldFx.render(poseStack, buffers, camera, partialTick);
        TendrilStrands.render(poseStack, buffers, camera, partialTick);
        LootSense.render(poseStack, buffers, camera, partialTick);
        Clairvoyance.render(poseStack, buffers, camera, partialTick);
        DowsingFx.render(poseStack, buffers, camera, partialTick);
        BarrageFx.render(poseStack, buffers, camera, partialTick);
        Menacing.render(poseStack, buffers, camera, partialTick);
        TrickTell.render(poseStack, buffers, camera, partialTick);
        ImpactWords.render(poseStack, buffers, camera, partialTick);
        Gore.render(poseStack, buffers, camera, partialTick);
        SummonFlair.render(poseStack, buffers, camera, partialTick);
        MagicianFx.render(poseStack, buffers, camera, partialTick);
        FlameFx.render(poseStack, buffers, camera, partialTick);
        ImpactDirector.render(poseStack, buffers, camera, partialTick);
        TimeStopWind.render(poseStack, buffers, camera, partialTick);
        MeteorStreaks.render(poseStack, buffers, camera, partialTick);

        buffers.endBatch();
    }
}
