package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Menacing {
    private static final ResourceLocation[] TEXTURES = {
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/particle/menacing.png"),
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/particle/tension.png")};

    private static final float LIFE_TICKS = 30F;
    private static final float FADE_TICKS = 8F;
    private static final float POP_TICKS = 4F;
    private static final float OVERSHOOT = 1.6F;
    private static final float SECOND_DELAY_TICKS = 3F;

    private static final double BIG = 0.42;
    private static final double SMALL = 0.28;
    private static final double SMALL_RIGHT = 0.3;
    private static final double SMALL_DOWN = 0.24;

    private static final double SIDE = 0.45;
    private static final double ABOVE = 0.3;
    private static final double RISE_PER_TICK = 0.004;

    private static final double SHIVER = 0.018;
    private static final float SHIVER_SPEED = 2.3F;

    private static final double RANGE = 48.0;

    private static final class Shown {
        private final float born;
        private final int kind;
        private float until;

        private Shown(float born, float until, int kind) {
            this.born = born;
            this.until = until;
            this.kind = kind;
        }
    }

    private static final Map<Integer, Shown> SHOWN = new HashMap<>();

    private Menacing() {
    }

    public static void heard(int entityId, int kind) {
        kind = Mth.clamp(kind, 0, TEXTURES.length - 1);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        float now = level.getGameTime();
        Shown shown = SHOWN.get(entityId);
        if (shown == null || now > shown.until || shown.kind != kind) {
            SHOWN.put(entityId, new Shown(now, now + LIFE_TICKS, kind));
        } else {
            shown.until = now + LIFE_TICKS;
        }
    }

    public static void clear() {
        SHOWN.clear();
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || SHOWN.isEmpty()) {
            return;
        }
        float now = level.getGameTime() + partialTick;
        Vec3 eye = camera.getPosition();
        Vector3f leftF = camera.getLeftVector();
        Vector3f upF = camera.getUpVector();
        Vec3 right = new Vec3(-leftF.x(), -leftF.y(), -leftF.z());
        Vec3 up = new Vec3(upF.x(), upF.y(), upF.z());
        Matrix4f pose = poseStack.last().pose();

        boolean drawn = false;
        Iterator<Map.Entry<Integer, Shown>> it = SHOWN.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Shown> entry = it.next();
            Shown shown = entry.getValue();
            if (now > shown.until + 1F) {
                it.remove();
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null || entity.isInvisible() || entity.distanceToSqr(eye) > RANGE * RANGE) {
                continue;
            }
            float age = now - shown.born;
            float fade = Mth.clamp((shown.until - now) / FADE_TICKS, 0F, 1F);
            if (fade <= 0.01F) {
                continue;
            }
            if (entity == camera.getEntity() && !camera.isDetached()) {
                continue;
            }
            Vec3 anchor = entity.getEyePosition(partialTick).add(0.0, ABOVE, 0.0).add(right.scale(SIDE))
                    .add(up.scale(age * RISE_PER_TICK));

            VertexConsumer out = buffers.getBuffer(RenderType.text(TEXTURES[shown.kind]));
            int argb = (Math.round(fade * 255F) << 24) | 0xFFFFFF;
            glyph(out, pose, eye, anchor.add(shiver(right, up, age, 0F)), right, up, BIG * pop(age), argb);
            Vec3 second = anchor.add(right.scale(SMALL_RIGHT)).subtract(up.scale(SMALL_DOWN));
            glyph(out, pose, eye, second.add(shiver(right, up, age, 1.7F)),
                    right, up, SMALL * pop(age - SECOND_DELAY_TICKS), argb);
            drawn = true;
        }

        if (drawn && buffers instanceof MultiBufferSource.BufferSource source) {
            for (ResourceLocation texture : TEXTURES) {
                source.endBatch(RenderType.text(texture));
            }
        }
    }

    private static Vec3 shiver(Vec3 right, Vec3 up, float age, float phase) {
        double x = Math.sin(age * SHIVER_SPEED + phase) * SHIVER;
        double y = Math.cos(age * SHIVER_SPEED * 1.3F + phase) * SHIVER;
        return right.scale(x).add(up.scale(y));
    }

    private static float pop(float age) {
        if (age <= 0F) {
            return 0F;
        }
        if (age >= POP_TICKS) {
            return 1F;
        }
        float t = age / POP_TICKS - 1F;
        return 1F + (OVERSHOOT + 1F) * t * t * t + OVERSHOOT * t * t;
    }

    private static void glyph(VertexConsumer out, Matrix4f pose, Vec3 eye, Vec3 centre, Vec3 right, Vec3 up,
                              double size, int argb) {
        if (size <= 0.005) {
            return;
        }
        Vec3 wide = right.scale(size * 0.5);
        Vec3 tall = up.scale(size * 0.5);
        Vec3 bl = centre.subtract(wide).subtract(tall).subtract(eye);
        Vec3 br = centre.add(wide).subtract(tall).subtract(eye);
        Vec3 tr = centre.add(wide).add(tall).subtract(eye);
        Vec3 tl = centre.subtract(wide).add(tall).subtract(eye);
        vertex(out, pose, bl, argb, 0F, 1F);
        vertex(out, pose, br, argb, 1F, 1F);
        vertex(out, pose, tr, argb, 1F, 0F);
        vertex(out, pose, tl, argb, 0F, 0F);
        vertex(out, pose, tl, argb, 0F, 0F);
        vertex(out, pose, tr, argb, 1F, 0F);
        vertex(out, pose, br, argb, 1F, 1F);
        vertex(out, pose, bl, argb, 0F, 1F);
    }

    private static void vertex(VertexConsumer out, Matrix4f pose, Vec3 at, int argb, float u, float v) {
        out.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor(argb)
                .setUv(u, v)
                .setLight(LightTexture.FULL_BRIGHT);
    }
}
