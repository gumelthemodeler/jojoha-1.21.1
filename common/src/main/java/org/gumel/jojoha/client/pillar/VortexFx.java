package org.gumel.jojoha.client.pillar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class VortexFx {
    private static final int FRAMES = 4;
    private static final int RING_SIDES = 10;
    private static final double TEXTURE_BLOCKS = 2.0;
    private static final double TORNADO_HEIGHT = 3.6;
    private static final double TORNADO_STEP = 0.4;
    private static final int TORNADO_LINGER = 4;
    private static final double DRILL_STEP = 0.55;
    private static final int SAND = 0xFFE6D3A8;
    private static final int PALE = 0xFFF2F2EC;

    private static final Map<Integer, Tornado> TORNADOES = new HashMap<>();
    private static final List<Drill> DRILLS = new ArrayList<>();

    private VortexFx() {
    }

    public static void clear() {
        TORNADOES.clear();
        DRILLS.clear();
    }

    public static void tornado(int key, Vec3 at, int left) {
        Tornado tornado = TORNADOES.computeIfAbsent(key, k -> new Tornado(at));
        tornado.prev = tornado.pos;
        tornado.pos = at;
        tornado.silent = 0;
        tornado.left = left;
    }

    public static void drill(Vec3 from, Vec3 heading, double length, int ticks) {
        DRILLS.add(new Drill(from, heading.normalize(), length, Math.max(2, ticks)));
    }

    public static void tick(ClientLevel level) {
        Iterator<Tornado> tornadoes = TORNADOES.values().iterator();
        while (tornadoes.hasNext()) {
            Tornado tornado = tornadoes.next();
            tornado.age++;
            if (++tornado.silent > TORNADO_LINGER) {
                tornadoes.remove();
                continue;
            }
            if (tornado.silent > 1) {
                tornado.prev = tornado.pos;
            }
            for (int i = 0; i < 4; i++) {
                double h = level.random.nextDouble() * TORNADO_HEIGHT;
                double r = radius(h) * (0.7 + level.random.nextDouble() * 0.4);
                double angle = level.random.nextDouble() * Math.PI * 2;
                double swirl = 0.25 + h * 0.05;
                level.addParticle(ModRegistries.PILLAR_SAND.get(), tornado.pos.x + Math.cos(angle) * r,
                        tornado.pos.y + h, tornado.pos.z + Math.sin(angle) * r,
                        -Math.sin(angle) * swirl, 0.08, Math.cos(angle) * swirl);
            }
            if (tornado.age % 3 == 0) {
                level.addParticle(ModRegistries.PILLAR_GUST.get(), tornado.pos.x, tornado.pos.y + 0.2, tornado.pos.z,
                        0, 0.02, 0);
            }
        }
        Iterator<Drill> drills = DRILLS.iterator();
        while (drills.hasNext()) {
            Drill drill = drills.next();
            if (++drill.age > drill.ticks + FRAMES) {
                drills.remove();
                continue;
            }
            if (drill.age <= drill.ticks) {
                for (int i = 0; i < 6; i++) {
                    double along = level.random.nextDouble() * drill.length;
                    Vec3[] basis = PixelDraw.basis(drill.heading);
                    double angle = level.random.nextDouble() * Math.PI * 2;
                    double r = 0.3 + along * 0.08;
                    Vec3 at = drill.from.add(drill.heading.scale(along)).add(basis[1].scale(Math.cos(angle) * r))
                            .add(basis[2].scale(Math.sin(angle) * r));
                    Vec3 velocity = drill.heading.scale(0.45).add(basis[1].scale(-Math.sin(angle) * 0.2))
                            .add(basis[2].scale(Math.cos(angle) * 0.2));
                    level.addParticle(ModRegistries.PILLAR_SAND.get(), at.x, at.y, at.z, velocity.x, velocity.y,
                            velocity.z);
                }
            }
        }
    }

    private static double radius(double height) {
        return 0.3 + height * 0.36;
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick,
                              ClientLevel level) {
        if (TORNADOES.isEmpty() && DRILLS.isEmpty()) {
            return;
        }
        Vec3 eye = camera.getPosition();
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(ArcFx.WIND));
        poseStack.pushPose();
        poseStack.translate(-eye.x, -eye.y, -eye.z);
        PoseStack.Pose pose = poseStack.last();
        for (Tornado tornado : TORNADOES.values()) {
            Vec3 base = tornado.prev.add(tornado.pos.subtract(tornado.prev).scale(partialTick));
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(base.add(0, 1, 0)));
            int rings = (int) Math.ceil(TORNADO_HEIGHT / TORNADO_STEP);
            int fading = tornado.left < rings ? rings - tornado.left : 0;
            for (int i = 0; i < rings - fading; i++) {
                double h = i * TORNADO_STEP;
                double spin = (tornado.age + partialTick) * (0.45 + i * 0.04) + i * 0.6;
                int frame = (tornado.age + i) % FRAMES;
                Vec3 sway = new Vec3(Math.sin(tornado.age * 0.15 + h) * 0.08 * h, 0, Math.cos(tornado.age * 0.12 + h) * 0.08 * h);
                ring(out, pose, base.add(0, h, 0).add(sway), new Vec3(1, 0, 0), new Vec3(0, 0, 1), new Vec3(0, 1, 0),
                        radius(h), TORNADO_STEP + 0.05, spin, frame, i % 3 == 0 ? SAND : PALE, light);
                if (i % 2 == 0) {
                    ring(out, pose, base.add(0, h + 0.1, 0).add(sway), new Vec3(1, 0, 0), new Vec3(0, 0, 1),
                            new Vec3(0, 1, 0), radius(h) * 0.6, TORNADO_STEP * 0.6, -spin * 1.3, (frame + 2) % FRAMES,
                            PALE, light);
                }
            }
        }
        for (Drill drill : DRILLS) {
            Vec3[] basis = PixelDraw.basis(drill.heading);
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(drill.from));
            float reach = Mth.clamp((drill.age + partialTick) / 2F, 0F, 1F);
            int rings = (int) Math.ceil(drill.length * reach / DRILL_STEP);
            int frameBase = Math.max(0, drill.age - drill.ticks);
            for (int i = 0; i < rings; i++) {
                double along = i * DRILL_STEP;
                double spin = (drill.age + partialTick) * 1.4 - i * 0.5;
                int frame = Math.min(FRAMES - 1, frameBase + (i % 2));
                ring(out, pose, drill.from.add(drill.heading.scale(along)), basis[1], basis[2], drill.heading,
                        0.25 + along * 0.09, DRILL_STEP + 0.05, spin, frame, i % 2 == 0 ? SAND : PALE, light);
            }
        }
        poseStack.popPose();
    }

    private static void ring(VertexConsumer out, PoseStack.Pose pose, Vec3 centre, Vec3 e1, Vec3 e2, Vec3 axis,
                             double radius, double height, double spin, int frame, int argb, int light) {
        double around = Math.PI * 2 * radius;
        float top = frame / (float) FRAMES;
        float bottom = (frame + 1) / (float) FRAMES;
        for (int s = 0; s < RING_SIDES; s++) {
            double a0 = spin + s * Math.PI * 2 / RING_SIDES;
            double a1 = spin + (s + 1) * Math.PI * 2 / RING_SIDES;
            Vec3 p0 = centre.add(e1.scale(Math.cos(a0) * radius)).add(e2.scale(Math.sin(a0) * radius));
            Vec3 p1 = centre.add(e1.scale(Math.cos(a1) * radius)).add(e2.scale(Math.sin(a1) * radius));
            float u0 = (float) (s / (double) RING_SIDES * around / TEXTURE_BLOCKS);
            float u1 = (float) ((s + 1) / (double) RING_SIDES * around / TEXTURE_BLOCKS);
            Vec3 lift = axis.scale(height);
            PixelDraw.quad(out, pose, p0.add(lift), p0, p1, p1.add(lift), u0, u1, top, bottom, argb, light);
        }
    }

    private static final class Tornado {
        Vec3 pos;
        Vec3 prev;
        int age;
        int silent;
        int left = Integer.MAX_VALUE;

        Tornado(Vec3 at) {
            this.pos = at;
            this.prev = at;
        }
    }

    private static final class Drill {
        final Vec3 from;
        final Vec3 heading;
        final double length;
        final int ticks;
        int age;

        Drill(Vec3 from, Vec3 heading, double length, int ticks) {
            this.from = from;
            this.heading = heading;
            this.length = length;
            this.ticks = ticks;
        }
    }
}
