package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.vampire.VampireFx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SpaceRipperBeam {
    private static final float CORE_WIDTH = 0.09F;
    private static final float SHEATH_WIDTH = 0.30F;

    private static final float SWEEP_TICKS = 3F;

    private static final List<Beam> ACTIVE = new ArrayList<>();

    private SpaceRipperBeam() {
    }

    public static void begin(int playerId, double x, double y, double z, List<Float> offsets,
                             int eye, int fireTicks, int ticks) {
        List<Vec3> points = new ArrayList<>();
        points.add(new Vec3(x, y, z));
        for (int i = 0; i + 2 < offsets.size(); i += 3) {
            points.add(new Vec3(x + offsets.get(i), y + offsets.get(i + 1), z + offsets.get(i + 2)));
        }
        if (points.size() < 2) {
            return;
        }

        for (Beam beam : ACTIVE) {
            if (beam.playerId == playerId && beam.eye == eye && beam.firing()) {
                beam.points = points;
                return;
            }
        }
        ACTIVE.removeIf(beam -> beam.playerId == playerId && beam.eye == eye);
        ACTIVE.add(new Beam(playerId, eye, points, fireTicks, ticks));
    }

    public static void tick() {
        Iterator<Beam> it = ACTIVE.iterator();
        while (it.hasNext()) {
            if (--it.next().remaining <= 0) {
                it.remove();
            }
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                              float partialTick) {
        if (ACTIVE.isEmpty() || Minecraft.getInstance().level == null) {
            return;
        }

        Vec3 cameraPos = camera.getPosition();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());

        for (Beam beam : ACTIVE) {
            float elapsed = beam.ticks - (beam.remaining - partialTick);

            float reached = Mth.clamp(elapsed / SWEEP_TICKS, 0F, 1F);
            float fade = elapsed < beam.fireTicks ? 1F
                    : 1F - (elapsed - beam.fireTicks) / Math.max(1F, beam.ticks - beam.fireTicks);
            if (fade <= 0F || reached <= 0F) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            ribbon(poseStack, consumer, camera, beam.points, reached, SHEATH_WIDTH, fade * 0.28F);
            ribbon(poseStack, consumer, camera, beam.points, reached, CORE_WIDTH, fade);

            poseStack.popPose();
        }

        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(RenderType.lightning());
        }
    }

    private static void ribbon(PoseStack poseStack, VertexConsumer consumer, Camera camera,
                               List<Vec3> points, float reached, float width, float alpha) {
        int last = Math.max(1, Math.round((points.size() - 1) * reached));
        Vec3 eye = camera.getPosition();

        int red = Math.round(VampireFx.RIPPER_RED * 255F);
        int green = Math.round(VampireFx.RIPPER_GREEN * 255F);
        int blue = Math.round(VampireFx.RIPPER_BLUE * 255F);
        int a = Math.round(Mth.clamp(alpha, 0F, 1F) * 255F);

        for (int i = 0; i < last; i++) {
            Vec3 here = points.get(i);
            Vec3 next = points.get(i + 1);

            Vec3 hereSide = across(here, next, eye).scale(width * taper(i, points.size()));
            Vec3 nextSide = across(here, next, eye).scale(width * taper(i + 1, points.size()));

            quad(poseStack, consumer, here.subtract(hereSide), here.add(hereSide),
                    next.add(nextSide), next.subtract(nextSide), red, green, blue, a);
        }
    }

    private static float taper(int index, int count) {
        float along = index / (float) Math.max(1, count - 1);
        return Mth.sin(along * (float) Math.PI) * 0.75F + 0.25F;
    }

    private static Vec3 across(Vec3 here, Vec3 next, Vec3 eye) {
        Vec3 along = next.subtract(here);
        Vec3 toEye = eye.subtract(here);
        Vec3 side = along.cross(toEye);
        return side.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 1.0, 0.0) : side.normalize();
    }

    private static void quad(PoseStack poseStack, VertexConsumer consumer,
                             Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                             int red, int green, int blue, int alpha) {
        PoseStack.Pose pose = poseStack.last();
        for (Vec3 corner : new Vec3[]{a, b, c, d}) {
            consumer.addVertex(pose, (float) corner.x, (float) corner.y, (float) corner.z)
                    .setColor(red, green, blue, alpha);
        }
    }

    private static final class Beam {
        private final int playerId;
        private final int eye;
        private List<Vec3> points;
        private final int fireTicks;
        private final int ticks;
        private int remaining;

        private Beam(int playerId, int eye, List<Vec3> points, int fireTicks, int ticks) {
            this.playerId = playerId;
            this.eye = eye;
            this.points = points;
            this.ticks = Math.max(1, ticks);
            this.fireTicks = Mth.clamp(fireTicks, 1, this.ticks);
            this.remaining = this.ticks;
        }

        private boolean firing() {
            return ticks - remaining < fireTicks;
        }
    }
}
