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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.skill.moves.SabreCycleSkill;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class SabreArcFx {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/sabre_arc.png");
    private static final int SWEEP_TICKS = SabreCycleSkill.SWEEP_TICKS;
    private static final int FRAMES = 4;
    private static final int FRAME_TICKS = 2;
    private static final int SEGMENTS = 64;
    private static final double BLADE_HEIGHT = 1.0;
    private static final double RISE = 0.4;
    private static final double BAND = 0.5;
    private static final double TEXTURE_BLOCKS = 2.0;
    private static final int SPARKS_PER_TICK = 2;
    private static final double SPARK_SPEED = 0.4;

    private static final List<Arc> ACTIVE = new ArrayList<>();

    private SabreArcFx() {
    }

    public static void begin(double x, double y, double z, float radius, float start, float charge) {
        ACTIVE.add(new Arc(new Vec3(x, y, z), radius, start));
    }

    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        Iterator<Arc> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Arc arc = it.next();
            arc.age++;
            if (arc.age > SWEEP_TICKS + FRAMES * FRAME_TICKS || level == null) {
                it.remove();
                continue;
            }
            if (arc.age <= SWEEP_TICKS) {
                sparks(level, arc);
            }
        }
    }

    private static void sparks(ClientLevel level, Arc arc) {
        float u = reached(arc.age);
        double around = arc.start + Math.PI * 2.0 * u;
        Vec3 at = point(arc, u);
        double tx = -Math.sin(around), tz = Math.cos(around);
        for (int i = 0; i < SPARKS_PER_TICK; i++) {
            double speed = SPARK_SPEED * (0.6 + level.random.nextDouble() * 0.8);
            level.addParticle(ParticleTypes.CRIT, at.x, at.y + BAND * level.random.nextDouble(), at.z,
                    tx * speed, 0.05 + level.random.nextDouble() * 0.1, tz * speed);
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        if (ACTIVE.isEmpty() || Minecraft.getInstance().level == null) {
            return;
        }
        Vec3 eye = camera.getPosition();
        RenderType type = RenderType.entityTranslucentEmissive(TEXTURE);
        VertexConsumer out = buffers.getBuffer(type);
        poseStack.pushPose();
        poseStack.translate(-eye.x, -eye.y, -eye.z);
        PoseStack.Pose pose = poseStack.last();
        for (Arc arc : ACTIVE) {
            draw(out, pose, arc);
        }
        poseStack.popPose();
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(type);
        }
    }

    private static void draw(VertexConsumer out, PoseStack.Pose pose, Arc arc) {
        int shown = arc.age + 1;
        float head = reached(shown);
        double around = Math.PI * 2.0 * arc.radius;
        for (int i = 0; i < SEGMENTS; i++) {
            float u0 = i / (float) SEGMENTS;
            if (u0 >= head) {
                break;
            }
            float u1 = Math.min((i + 1) / (float) SEGMENTS, head);
            int frame = (shown - passedAt(u0)) / FRAME_TICKS;
            if (frame >= FRAMES) {
                continue;
            }
            Vec3 p0 = point(arc, u0);
            Vec3 p1 = point(arc, u1);
            float s0 = (float) (u0 * around / TEXTURE_BLOCKS);
            float s1 = (float) (u1 * around / TEXTURE_BLOCKS);
            float top = frame / (float) FRAMES;
            float bottom = (frame + 1) / (float) FRAMES;
            vertex(out, pose, p0.x, p0.y + BAND, p0.z, s0, top);
            vertex(out, pose, p0.x, p0.y, p0.z, s0, bottom);
            vertex(out, pose, p1.x, p1.y, p1.z, s1, bottom);
            vertex(out, pose, p1.x, p1.y + BAND, p1.z, s1, top);
        }
    }

    private static float reached(int age) {
        return Mth.clamp(age / (float) SWEEP_TICKS, 0F, 1F);
    }

    private static int passedAt(float u) {
        return Mth.ceil(u * SWEEP_TICKS);
    }

    private static Vec3 point(Arc arc, float u) {
        double around = arc.start + Math.PI * 2.0 * u;
        return arc.centre.add(Math.cos(around) * arc.radius, BLADE_HEIGHT - BAND * 0.5 + RISE * (u - 0.5),
                Math.sin(around) * arc.radius);
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, double x, double y, double z, float u, float v) {
        out.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0F, 1F, 0F);
    }

    private static final class Arc {
        private final Vec3 centre;
        private final float radius;
        private final float start;
        private int age;

        private Arc(Vec3 centre, float radius, float start) {
            this.centre = centre;
            this.radius = radius;
            this.start = start;
        }
    }
}
