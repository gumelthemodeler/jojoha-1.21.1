package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.hierophant.EmeraldSparkle;
import org.gumel.jojoha.stand.hierophant.Tendrils;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TendrilStrands {
    public static final ResourceLocation STRING =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/hierophant_green_string.png");
    public static final ResourceLocation STRING_RECOLOR =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/hierophant_green_skin_recolor.png");

    private static final float SHEET_ACROSS = 2F;
    private static final float SHEET_ALONG = 16F;

    private static final float SHOOT_SPEED = 4.5F;
    private static final float SHOOT_LEAST = 1.5F;
    private static final float SHOOT_MOST = 7F;
    private static final float RETRACT = 5F;

    private static final float GLOW_WIDTH = 4F;
    private static final float GLOW_ALPHA = 0.32F;
    private static final float PULSE_SPACING = 2.6F;
    private static final float PULSE_SPEED = 0.55F;
    private static final float GLOW_PIECE = 0.5F;

    private static final float HEAD_SIZE = 0.45F;

    private static final float MOTES_PER_BLOCK = 0.006F;

    private static final Map<Integer, Strand> ACTIVE = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private TendrilStrands() {
    }

    public static void show(int key, double x, double y, double z, List<Float> offsets, float width, int ticks,
                            int tint) {
        if (ticks <= 0) {
            Strand going = ACTIVE.get(key);
            if (going != null && going.remaining > RETRACT) {
                going.remaining = (int) RETRACT;
            }
            return;
        }
        List<Vec3> points = new ArrayList<>();
        points.add(new Vec3(x, y, z));
        for (int i = 0; i + 2 < offsets.size(); i += 3) {
            points.add(new Vec3(x + offsets.get(i), y + offsets.get(i + 1), z + offsets.get(i + 2)));
        }
        if (points.size() < 2) {
            return;
        }
        Strand existing = ACTIVE.get(key);
        float age = existing == null ? 0F : existing.age;
        ACTIVE.put(key, new Strand(points, width, ticks, tint, age));
    }

    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        ACTIVE.values().removeIf(strand -> {
            strand.age++;
            if (level != null) {
                shed(level, strand);
            }
            return --strand.remaining <= 0;
        });
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static RenderType type() {
        return RenderType.entityTranslucentEmissive(STRING);
    }

    public static RenderType recolor() {
        return RenderType.entityTranslucentEmissive(STRING_RECOLOR);
    }

    private static void string(PoseStack poseStack, MultiBufferSource buffers, Vec3 eye, List<Vec3> points,
                               float width, int tint, float alpha) {
        strip(poseStack, buffers.getBuffer(type()), eye, points, width, 0, alpha);
        if (tint != 0) {
            strip(poseStack, buffers.getBuffer(recolor()), eye, points, width, tint, alpha);
        }
    }

    public static int glowOf(int tint) {
        return Tendrils.glowOf(tint);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        if (ACTIVE.isEmpty() || Minecraft.getInstance().level == null) {
            return;
        }
        Vec3 cameraPos = camera.getPosition();
        float time = (Minecraft.getInstance().level.getGameTime() % 24000L) + partialTick;
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        VertexConsumer glow = buffers.getBuffer(EmeraldGlow.ADDITIVE);
        for (Strand strand : ACTIVE.values()) {
            float left = strand.remaining - partialTick;
            float reach = strand.reach(strand.age + partialTick, left);
            float fade = Mth.clamp(left / RETRACT, 0F, 1F);
            if (reach <= 0.01F || fade <= 0F) {
                continue;
            }
            List<Vec3> shown = strand.upTo(reach);
            glow(poseStack, glow, cameraPos, shown, strand.width, glowOf(strand.tint), fade, time);
            if (reach < strand.length - 0.01F) {
                head(poseStack, glow, cameraPos, shown.get(shown.size() - 1), glowOf(strand.tint), fade);
            }
        }
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(EmeraldGlow.ADDITIVE);
        }
        for (Strand strand : ACTIVE.values()) {
            float left = strand.remaining - partialTick;
            float reach = strand.reach(strand.age + partialTick, left);
            float fade = Mth.clamp(left / RETRACT, 0F, 1F) * Mth.clamp((strand.age + partialTick) / 2F, 0.35F, 1F);
            if (reach <= 0.01F || fade <= 0F) {
                continue;
            }
            string(poseStack, buffers, cameraPos, strand.upTo(reach), strand.width, strand.tint, fade);
        }
        poseStack.popPose();

        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(type());
            source.endBatch(recolor());
        }
    }

    public static void drawLine(PoseStack poseStack, MultiBufferSource buffers, Vec3 eye, Vec3 from, Vec3 to,
                                float width, int tint) {
        float time = Minecraft.getInstance().level == null ? 0F
                : Minecraft.getInstance().level.getGameTime() % 24000L;
        glow(poseStack, buffers.getBuffer(EmeraldGlow.ADDITIVE), eye, List.of(from, to), width, glowOf(tint), 1F,
                time);
        string(poseStack, buffers, eye, List.of(from, to), width, tint, 1F);
    }

    private static void strip(PoseStack poseStack, VertexConsumer consumer, Vec3 eye, List<Vec3> points,
                              float width, int tint, float alpha) {
        int r = tint == 0 ? 255 : (tint >> 16) & 0xFF;
        int g = tint == 0 ? 255 : (tint >> 8) & 0xFF;
        int b = tint == 0 ? 255 : tint & 0xFF;
        int a = Math.round(Mth.clamp(alpha, 0F, 1F) * 255F);
        float repeat = SHEET_ALONG * (width * 2F / SHEET_ACROSS);
        float along = 0F;
        PoseStack.Pose pose = poseStack.last();
        for (int i = 0; i + 1 < points.size(); i++) {
            Vec3 here = points.get(i);
            Vec3 next = points.get(i + 1);
            float length = (float) here.distanceTo(next);
            Vec3 side = across(here, next, eye).scale(width);
            float v0 = along / repeat;
            float v1 = (along + length) / repeat;
            vertex(consumer, pose, here.subtract(side), 0F, v0, r, g, b, a);
            vertex(consumer, pose, here.add(side), 1F, v0, r, g, b, a);
            vertex(consumer, pose, next.add(side), 1F, v1, r, g, b, a);
            vertex(consumer, pose, next.subtract(side), 0F, v1, r, g, b, a);
            along += length;
        }
    }

    private static void glow(PoseStack poseStack, VertexConsumer consumer, Vec3 eye, List<Vec3> points,
                             float width, int colour, float alpha, float time) {
        float r = ((colour >> 16) & 0xFF) / 255F;
        float g = ((colour >> 8) & 0xFF) / 255F;
        float b = (colour & 0xFF) / 255F;
        Matrix4f pose = poseStack.last().pose();
        float wide = width * GLOW_WIDTH;
        float along = 0F;
        for (int i = 0; i + 1 < points.size(); i++) {
            Vec3 here = points.get(i);
            Vec3 next = points.get(i + 1);
            float length = (float) here.distanceTo(next);
            if (length < 1.0E-4F) {
                continue;
            }
            Vec3 side = across(here, next, eye).scale(wide);
            int pieces = Math.max(1, Mth.ceil(length / GLOW_PIECE));
            for (int p = 0; p < pieces; p++) {
                float t0 = p / (float) pieces;
                float t1 = (p + 1) / (float) pieces;
                Vec3 a = here.lerp(next, t0);
                Vec3 c = here.lerp(next, t1);
                float s0 = alpha * GLOW_ALPHA * band(along + length * t0, time);
                float s1 = alpha * GLOW_ALPHA * band(along + length * t1, time);
                glowQuad(consumer, pose, a.subtract(side), a, c, c.subtract(side), r, g, b, 0F, s0, s1, 0F);
                glowQuad(consumer, pose, a, a.add(side), c.add(side), c, r, g, b, s0, 0F, 0F, s1);
            }
            along += length;
        }
    }

    private static float band(float along, float time) {
        float wave = Mth.sin((along / PULSE_SPACING - time * PULSE_SPEED) * Mth.TWO_PI);
        return 0.55F + 0.45F * wave * wave;
    }

    private static void head(PoseStack poseStack, VertexConsumer consumer, Vec3 eye, Vec3 at, int colour,
                             float alpha) {
        Matrix4f pose = poseStack.last().pose();
        Vec3 toEye = eye.subtract(at);
        if (toEye.lengthSqr() < 1.0E-6) {
            return;
        }
        toEye = toEye.normalize();
        Vec3 up = Math.abs(toEye.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = up.cross(toEye).normalize().scale(HEAD_SIZE);
        Vec3 top = toEye.cross(right).normalize().scale(HEAD_SIZE);
        float r = ((colour >> 16) & 0xFF) / 255F;
        float g = ((colour >> 8) & 0xFF) / 255F;
        float b = (colour & 0xFF) / 255F;
        for (int i = 0; i < 8; i++) {
            double a0 = i * Math.PI / 4.0;
            double a1 = (i + 1) * Math.PI / 4.0;
            Vec3 e0 = at.add(right.scale(Math.cos(a0))).add(top.scale(Math.sin(a0)));
            Vec3 e1 = at.add(right.scale(Math.cos(a1))).add(top.scale(Math.sin(a1)));
            put(consumer, pose, at, 1F, 1F, 1F, alpha * 0.9F);
            put(consumer, pose, at, 1F, 1F, 1F, alpha * 0.9F);
            put(consumer, pose, e1, r, g, b, 0F);
            put(consumer, pose, e0, r, g, b, 0F);
        }
    }

    private static void glowQuad(VertexConsumer consumer, Matrix4f pose, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3,
                                 float r, float g, float b, float a0, float a1, float a2, float a3) {
        put(consumer, pose, p0, r, g, b, a0);
        put(consumer, pose, p1, r, g, b, a1);
        put(consumer, pose, p2, r, g, b, a2);
        put(consumer, pose, p3, r, g, b, a3);
    }

    private static void put(VertexConsumer consumer, Matrix4f pose, Vec3 at, float r, float g, float b, float a) {
        consumer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(r, g, b, Mth.clamp(a, 0F, 1F));
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 at, float u, float v,
                               int r, int g, int b, int a) {
        consumer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0F, 1F, 0F);
    }

    private static Vec3 across(Vec3 here, Vec3 next, Vec3 eye) {
        Vec3 side = next.subtract(here).cross(eye.subtract(here));
        return side.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 1.0, 0.0) : side.normalize();
    }

    private static void shed(ClientLevel level, Strand strand) {
        float reach = strand.reach(strand.age, strand.remaining);
        if (reach <= 0.01F) {
            return;
        }
        float chance = reach * MOTES_PER_BLOCK * (strand.width > 0.07F ? 2F : 1F);
        int motes = (int) chance + (RANDOM.nextFloat() < chance - (int) chance ? 1 : 0);
        if (motes == 0) {
            return;
        }
        EmeraldSparkle sparkle =
                Tendrils.sparkle(strand.tint);
        for (int i = 0; i < motes; i++) {
            Vec3 at = strand.at(RANDOM.nextFloat() * reach);
            level.addParticle(sparkle, at.x, at.y, at.z, 0.0, 0.01, 0.0);
        }
    }

    private static final class Strand {
        private final List<Vec3> points;
        private final float[] lengths;
        private final float length;
        private final float width;
        private final int tint;
        private final float shoot;
        private int remaining;
        private float age;

        private Strand(List<Vec3> points, float width, int ticks, int tint, float age) {
            this.points = points;
            this.lengths = new float[points.size() - 1];
            float total = 0F;
            for (int i = 0; i + 1 < points.size(); i++) {
                lengths[i] = (float) points.get(i).distanceTo(points.get(i + 1));
                total += lengths[i];
            }
            this.length = total;
            this.width = width;
            this.remaining = ticks;
            this.tint = tint;
            this.age = age;
            this.shoot = Mth.clamp(total / SHOOT_SPEED, SHOOT_LEAST, SHOOT_MOST);
        }

        float reach(float age, float left) {
            float out = Mth.clamp(age / shoot, 0F, 1F);
            float in = Mth.clamp(left / RETRACT, 0F, 1F);
            float eased = 1F - (1F - out) * (1F - out);
            return length * Math.min(eased, in);
        }

        Vec3 at(float distance) {
            float left = distance;
            for (int i = 0; i < lengths.length; i++) {
                if (left <= lengths[i] || i == lengths.length - 1) {
                    float t = lengths[i] < 1.0E-5F ? 0F : Mth.clamp(left / lengths[i], 0F, 1F);
                    return points.get(i).lerp(points.get(i + 1), t);
                }
                left -= lengths[i];
            }
            return points.get(points.size() - 1);
        }

        List<Vec3> upTo(float distance) {
            if (distance >= length - 1.0E-4F) {
                return points;
            }
            List<Vec3> cut = new ArrayList<>();
            cut.add(points.get(0));
            float left = distance;
            for (int i = 0; i < lengths.length; i++) {
                if (left <= lengths[i]) {
                    float t = lengths[i] < 1.0E-5F ? 0F : left / lengths[i];
                    cut.add(points.get(i).lerp(points.get(i + 1), t));
                    return cut;
                }
                left -= lengths[i];
                cut.add(points.get(i + 1));
            }
            return cut;
        }
    }
}
