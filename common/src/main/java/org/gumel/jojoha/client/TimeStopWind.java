package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class TimeStopWind {
    private static final int STREAKS = 48;
    private static final int CURLS = 6;
    private static final int SEGMENTS = 8;

    private static final float WIDTH = 1.5F / 16F;
    private static final float MAX_ALPHA = 0.6F;
    private static final float MAX_REACH = 60F;

    private static final float LOW = 0.3F, HIGH = 2.2F;
    private static final float TILT = 0.25F;
    private static final float LENGTH_MIN = 2.5F, LENGTH_MAX = 4.5F;
    private static final float LAG_MIN = 0.8F, LAG_MAX = 1.0F;
    private static final float RIPPLE = 0.12F, RIPPLE_WAVES = 2.5F;

    private static final float CURL_INNER = 0.8F, CURL_OUTER = 2.6F;
    private static final float CURL_TURNS = 0.8F;
    private static final float CURL_RISE = 0.7F;
    private static final float CURL_TAIL = 0.35F;
    private static final float CURL_REACH = 8F;

    private record Streak(Vec3 direction, float height, float length, float lag, float phase) {
    }

    private record Curl(float start, float height, float spin) {
    }

    private static final List<Streak> STREAK_LIST = new ArrayList<>();
    private static final List<Curl> CURL_LIST = new ArrayList<>();
    private static Vec3 centre = Vec3.ZERO;

    private TimeStopWind() {
    }

    public static void begin(Vec3 at) {
        centre = at;
        STREAK_LIST.clear();
        CURL_LIST.clear();
        RandomSource random = RandomSource.create();
        for (int i = 0; i < STREAKS; i++) {
            float around = (i + random.nextFloat()) / STREAKS * Mth.TWO_PI;
            float tilt = (random.nextFloat() * 2F - 1F) * TILT;
            Vec3 direction = new Vec3(Mth.cos(around), tilt, Mth.sin(around)).normalize();
            STREAK_LIST.add(new Streak(direction, Mth.lerp(random.nextFloat(), LOW, HIGH),
                    Mth.lerp(random.nextFloat(), LENGTH_MIN, LENGTH_MAX),
                    Mth.lerp(random.nextFloat(), LAG_MIN, LAG_MAX), random.nextFloat() * Mth.TWO_PI));
        }
        for (int i = 0; i < CURLS; i++) {
            CURL_LIST.add(new Curl((i + random.nextFloat() * 0.5F) / CURLS * Mth.TWO_PI,
                    Mth.lerp(random.nextFloat(), LOW, HIGH - CURL_RISE), random.nextBoolean() ? 1F : -1F));
        }
    }

    public static void clear() {
        STREAK_LIST.clear();
        CURL_LIST.clear();
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        float front = TimeStopShader.frontRadius();
        float fade = TimeStopShader.waveStrength();
        if (STREAK_LIST.isEmpty() || front <= 0.001F || fade <= 0.001F) {
            return;
        }
        Vec3 eye = camera.getPosition();
        RenderType type = RenderType.lightning();
        VertexConsumer out = buffers.getBuffer(type);
        poseStack.pushPose();
        poseStack.translate(-eye.x, -eye.y, -eye.z);
        PoseStack.Pose pose = poseStack.last();
        float alpha = MAX_ALPHA * fade;

        for (Streak streak : STREAK_LIST) {
            float head = Math.min(TimeStopShader.frontAlong(streak.direction()) * streak.lag(), MAX_REACH);
            float tail = Math.max(0F, head - streak.length());
            if (head - tail < 0.05F) {
                continue;
            }
            Vec3[] points = new Vec3[SEGMENTS + 1];
            Vec3 base = centre.add(0, streak.height(), 0);
            for (int i = 0; i <= SEGMENTS; i++) {
                float s = i / (float) SEGMENTS;
                float along = Mth.lerp(s, tail, head);
                float ripple = Mth.sin(s * RIPPLE_WAVES * Mth.TWO_PI + streak.phase() + along) * RIPPLE;
                points[i] = base.add(streak.direction().scale(along)).add(0, ripple, 0);
            }
            ribbon(out, pose, eye, points, alpha);
        }

        float reach = Mth.clamp(front / CURL_REACH, 0F, 1F);
        for (Curl curl : CURL_LIST) {
            float headS = reach;
            float tailS = Math.max(0F, headS - CURL_TAIL);
            if (headS - tailS < 0.02F) {
                continue;
            }
            Vec3[] points = new Vec3[SEGMENTS + 1];
            for (int i = 0; i <= SEGMENTS; i++) {
                float s = Mth.lerp(i / (float) SEGMENTS, tailS, headS);
                float angle = curl.start() + curl.spin() * s * CURL_TURNS * Mth.TWO_PI;
                float radius = Mth.lerp(s, CURL_INNER, CURL_OUTER);
                points[i] = centre.add(Mth.cos(angle) * radius, curl.height() + s * CURL_RISE, Mth.sin(angle) * radius);
            }
            ribbon(out, pose, eye, points, alpha);
        }

        poseStack.popPose();
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(type);
        }
    }

    private static void ribbon(VertexConsumer out, PoseStack.Pose pose, Vec3 eye, Vec3[] points, float alpha) {
        for (int i = 0; i < points.length - 1; i++) {
            Vec3 a = points[i];
            Vec3 b = points[i + 1];
            Vec3 along = b.subtract(a);
            Vec3 side = along.cross(eye.subtract(a)).normalize().scale(WIDTH / 2F);
            float alphaA = alpha * i / (points.length - 1);
            float alphaB = alpha * (i + 1) / (points.length - 1);
            vertex(out, pose, a.add(side), alphaA);
            vertex(out, pose, a.subtract(side), alphaA);
            vertex(out, pose, b.subtract(side), alphaB);
            vertex(out, pose, b.add(side), alphaB);
        }
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Vec3 at, float alpha) {
        out.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor(1F, 1F, 1F, alpha);
    }
}
