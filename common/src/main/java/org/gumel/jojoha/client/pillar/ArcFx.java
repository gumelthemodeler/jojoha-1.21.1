package org.gumel.jojoha.client.pillar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayList;
import java.util.List;

public final class ArcFx {
    public static final ResourceLocation WIND =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/pillar_wind_band.png");
    public static final ResourceLocation LIGHT =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/pillar_light_arc.png");

    private static final int FRAMES = 4;
    private static final int SEGMENTS = 24;
    private static final double TEXTURE_BLOCKS = 2.0;
    private static final double GALE_SPEED = 2.0;
    private static final int SAW_EVERY = 2;
    private static final int SAW_BLADES = 4;
    private static final double SAW_STEP = Math.PI / 8;
    private static final double SAW_LENGTH = 14.0 / 16.0;
    private static final double SAW_WIDTH = 3.0 / 16.0;
    private static final float SAW_U0 = 9F / 32F;
    private static final float SAW_U1 = 12F / 32F;
    private static final float SAW_TIP_V = 3F / 32F;
    private static final float SAW_HILT_V = 17F / 32F;
    private static final double[] SAW_RINGS = {0.15, -0.2};
    private static final int LIGHT_HALO = 0xFFFFF0B0;
    private static final int WIND_HALO = 0xFFDDE8EA;
    private static final float LIGHT_HALO_STRENGTH = 0.6F;
    private static final float WIND_HALO_STRENGTH = 0.22F;
    private static final double HALO_SPREAD = 2.4;

    private static final List<Arc> ARCS = new ArrayList<>();
    private static final List<Streak> STREAKS = new ArrayList<>();
    private static final List<Gale> GALES = new ArrayList<>();
    private static final List<Saw> SAWS = new ArrayList<>();

    private ArcFx() {
    }

    public static void clear() {
        ARCS.clear();
        STREAKS.clear();
        GALES.clear();
        SAWS.clear();
    }

    public static void arc(ResourceLocation texture, boolean lit, Vec3 centre, Vec3 e1, Vec3 e2, double radius,
                           double band, double start, double sweep, int sweepTicks, int frameTicks) {
        ARCS.add(new Arc(texture, lit, centre, e1.normalize(), e2.normalize(), radius, band, start, sweep,
                Math.max(1, sweepTicks), Math.max(1, frameTicks)));
    }

    public static void streak(ResourceLocation texture, boolean lit, Vec3 from, Vec3 to, double width, int revealTicks,
                              int delay) {
        STREAKS.add(new Streak(texture, lit, from, to, width, Math.max(1, revealTicks), delay));
    }

    public static void gale(Vec3 from, Vec3 to) {
        GALES.add(new Gale(from, to));
    }

    public static void slash(Vec3 from, Vec3 path) {
        Vec3 to = from.add(path);
        streak(LIGHT, true, from, to, 0.55, 2, 0);
        Vec3 flat = new Vec3(path.x, 0, path.z);
        Vec3 heading = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        Vec3 side = new Vec3(-heading.z, 0, heading.x).add(0, -0.4, 0).normalize();
        arc(LIGHT, true, to.subtract(heading.scale(0.5)), heading, side, 1.5, 0.45, -1.6, 3.2, 3, 2);
    }

    public static void saw(int entity, int ticks) {
        SAWS.add(new Saw(entity, ticks));
    }

    public static void rift(Vec3 from, Vec3 look, int blades, double fanDegrees, double range) {
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 heading = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
        for (int blade = 0; blade < blades; blade++) {
            double turn = Math.toRadians(-fanDegrees / 2 + fanDegrees * blade / (blades - 1));
            double cos = Math.cos(turn);
            double sin = Math.sin(turn);
            Vec3 ray = new Vec3(look.x * cos - look.z * sin, look.y, look.x * sin + look.z * cos).normalize();
            streak(WIND, false, from.add(ray.scale(0.8)), from.add(ray.scale(range)), 0.4, 3,
                    Math.abs(blade - blades / 2));
        }
        Vec3 side = new Vec3(-heading.z, 0, heading.x);
        double half = Math.toRadians(fanDegrees / 2);
        for (int ring = 1; ring <= 3; ring++) {
            arc(WIND, false, from, heading, side, range * ring / 3.5, 0.5, -half, half * 2, 3, 2);
        }
    }

    public static void tick(ClientLevel level) {
        ARCS.removeIf(arc -> ++arc.age > arc.sweepTicks + FRAMES * arc.frameTicks);
        STREAKS.removeIf(streak -> ++streak.age > streak.delay + streak.reveal + FRAMES * 2);
        GALES.removeIf(gale -> !gale.tick(level));
        SAWS.removeIf(saw -> !saw.tick(level));
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick,
                              ClientLevel level) {
        if (ARCS.isEmpty() && STREAKS.isEmpty() && SAWS.isEmpty()) {
            return;
        }
        Vec3 eye = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-eye.x, -eye.y, -eye.z);
        PoseStack.Pose pose = poseStack.last();
        for (Arc arc : ARCS) {
            arc.draw(buffers.getBuffer(RenderType.entityCutoutNoCull(arc.texture)), pose, level);
        }
        for (Streak streak : STREAKS) {
            streak.draw(buffers.getBuffer(RenderType.entityCutoutNoCull(streak.texture)), pose, eye, level);
        }
        for (Saw saw : SAWS) {
            saw.draw(buffers.getBuffer(RenderType.entityCutoutNoCull(PillarModels.LIGHT_BLADE_TEXTURE)), pose, level,
                    partialTick);
        }
        VertexConsumer beam = buffers.getBuffer(RenderType.eyes(FlairFx.BEAM_TEXTURE));
        for (Arc arc : ARCS) {
            arc.halo(beam, pose);
        }
        for (Streak streak : STREAKS) {
            streak.halo(beam, pose, eye);
        }
        poseStack.popPose();
    }

    private static int haloColour(boolean lit, int frame) {
        float fade = 1F - frame / (float) FRAMES;
        return lit ? FlairFx.dim(LIGHT_HALO, LIGHT_HALO_STRENGTH * fade) : FlairFx.dim(WIND_HALO, WIND_HALO_STRENGTH * fade);
    }

    private static int light(boolean lit, ClientLevel level, Vec3 at) {
        return lit ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(level, BlockPos.containing(at));
    }

    private static final class Arc {
        final ResourceLocation texture;
        final boolean lit;
        final Vec3 centre;
        final Vec3 e1;
        final Vec3 e2;
        final double radius;
        final double band;
        final double start;
        final double sweep;
        final int sweepTicks;
        final int frameTicks;
        int age;

        Arc(ResourceLocation texture, boolean lit, Vec3 centre, Vec3 e1, Vec3 e2, double radius, double band,
            double start, double sweep, int sweepTicks, int frameTicks) {
            this.texture = texture;
            this.lit = lit;
            this.centre = centre;
            this.e1 = e1;
            this.e2 = e2;
            this.radius = radius;
            this.band = band;
            this.start = start;
            this.sweep = sweep;
            this.sweepTicks = sweepTicks;
            this.frameTicks = frameTicks;
        }

        void halo(VertexConsumer out, PoseStack.Pose pose) {
            int shown = age + 1;
            float head = Mth.clamp(shown / (float) sweepTicks, 0F, 1F);
            double reach = band * HALO_SPREAD / 2;
            for (int i = 0; i < SEGMENTS; i++) {
                float u0 = i / (float) SEGMENTS;
                if (u0 >= head) {
                    break;
                }
                float u1 = Math.min((i + 1) / (float) SEGMENTS, head);
                int frame = (shown - Mth.ceil(u0 * sweepTicks)) / frameTicks;
                if (frame >= FRAMES) {
                    continue;
                }
                double a0 = start + sweep * u0;
                double a1 = start + sweep * u1;
                PixelDraw.quad(out, pose, point(a0, radius + reach), point(a0, Math.max(0, radius - reach)),
                        point(a1, Math.max(0, radius - reach)), point(a1, radius + reach), 0F, 1F, 0F, 1F,
                        haloColour(lit, frame), LightTexture.FULL_BRIGHT);
            }
        }

        Vec3 point(double angle, double r) {
            return centre.add(e1.scale(Math.cos(angle) * r)).add(e2.scale(Math.sin(angle) * r));
        }

        void draw(VertexConsumer out, PoseStack.Pose pose, ClientLevel level) {
            int shown = age + 1;
            float head = Mth.clamp(shown / (float) sweepTicks, 0F, 1F);
            int light = light(lit, level, centre);
            double length = Math.abs(sweep) * radius;
            for (int i = 0; i < SEGMENTS; i++) {
                float u0 = i / (float) SEGMENTS;
                if (u0 >= head) {
                    break;
                }
                float u1 = Math.min((i + 1) / (float) SEGMENTS, head);
                int passed = Mth.ceil(u0 * sweepTicks);
                int frame = (shown - passed) / frameTicks;
                if (frame >= FRAMES) {
                    continue;
                }
                double a0 = start + sweep * u0;
                double a1 = start + sweep * u1;
                float s0 = (float) (u0 * length / TEXTURE_BLOCKS);
                float s1 = (float) (u1 * length / TEXTURE_BLOCKS);
                float top = frame / (float) FRAMES;
                float bottom = (frame + 1) / (float) FRAMES;
                PixelDraw.quad(out, pose, point(a0, radius + band / 2), point(a0, radius - band / 2),
                        point(a1, radius - band / 2), point(a1, radius + band / 2), s0, s1, top, bottom,
                        0xFFFFFFFF, light);
            }
        }
    }

    private static final class Streak {
        void halo(VertexConsumer out, PoseStack.Pose pose, Vec3 eye) {
            int shown = age - delay + 1;
            if (shown <= 0) {
                return;
            }
            int pieces = 8;
            float head = Mth.clamp(shown / (float) reveal, 0F, 1F);
            for (int i = 0; i < pieces; i++) {
                float u0 = i / (float) pieces;
                if (u0 >= head) {
                    break;
                }
                float u1 = Math.min((i + 1) / (float) pieces, head);
                int frame = (shown - Mth.ceil(u0 * reveal)) / 2;
                if (frame >= FRAMES) {
                    continue;
                }
                Vec3 a = from.add(to.subtract(from).scale(u0));
                Vec3 b = from.add(to.subtract(from).scale(u1));
                PixelDraw.ribbon(out, pose, a, b, eye, (float) (width * HALO_SPREAD), 0F, 1F, 0F, 1F,
                        haloColour(lit, frame), LightTexture.FULL_BRIGHT);
            }
        }

        final ResourceLocation texture;
        final boolean lit;
        final Vec3 from;
        final Vec3 to;
        final double width;
        final int reveal;
        final int delay;
        int age;

        Streak(ResourceLocation texture, boolean lit, Vec3 from, Vec3 to, double width, int reveal, int delay) {
            this.texture = texture;
            this.lit = lit;
            this.from = from;
            this.to = to;
            this.width = width;
            this.reveal = reveal;
            this.delay = delay;
        }

        void draw(VertexConsumer out, PoseStack.Pose pose, Vec3 eye, ClientLevel level) {
            int shown = age - delay + 1;
            if (shown <= 0) {
                return;
            }
            int pieces = 8;
            float head = Mth.clamp(shown / (float) reveal, 0F, 1F);
            int light = light(lit, level, from);
            double length = from.distanceTo(to);
            for (int i = 0; i < pieces; i++) {
                float u0 = i / (float) pieces;
                if (u0 >= head) {
                    break;
                }
                float u1 = Math.min((i + 1) / (float) pieces, head);
                int frame = (shown - Mth.ceil(u0 * reveal)) / 2;
                if (frame >= FRAMES) {
                    continue;
                }
                Vec3 a = from.add(to.subtract(from).scale(u0));
                Vec3 b = from.add(to.subtract(from).scale(u1));
                PixelDraw.ribbon(out, pose, a, b, eye, (float) width,
                        (float) (u0 * length / TEXTURE_BLOCKS), (float) (u1 * length / TEXTURE_BLOCKS),
                        frame / (float) FRAMES, (frame + 1) / (float) FRAMES, 0xFFFFFFFF, light);
            }
        }
    }

    private static final class Gale {
        final Vec3 from;
        final Vec3 heading;
        final double distance;
        final Vec3 side;
        int age;

        Gale(Vec3 from, Vec3 to) {
            this.from = from;
            Vec3 span = to.subtract(from);
            this.distance = span.length();
            this.heading = distance < 1.0E-4 ? new Vec3(0, 0, 1) : span.normalize();
            this.side = PixelDraw.basis(heading)[1];
        }

        boolean tick(ClientLevel level) {
            age++;
            double travelled = Math.min(distance, age * GALE_SPEED);
            Vec3 at = from.add(heading.scale(travelled));
            arc(WIND, false, at.subtract(heading.scale(0.7)), heading, side, 0.8, 0.35, -1.1, 2.2, 1, 1);
            for (int i = 0; i < 3; i++) {
                level.addParticle(ModRegistries.PILLAR_GUST.get(), at.x + (level.random.nextDouble() - 0.5) * 0.6,
                        at.y + (level.random.nextDouble() - 0.5) * 0.6, at.z + (level.random.nextDouble() - 0.5) * 0.6,
                        heading.x * 0.1, heading.y * 0.1, heading.z * 0.1);
            }
            return travelled < distance;
        }
    }

    private static final class Saw {
        final int entity;
        final int ticks;
        int age;

        Saw(int entity, int ticks) {
            this.entity = entity;
            this.ticks = ticks;
        }

        boolean tick(ClientLevel level) {
            Entity target = level.getEntity(entity);
            if (target == null || ++age > ticks) {
                return false;
            }
            if (age % SAW_EVERY == 0) {
                Vec3 centre = target.getBoundingBox().getCenter();
                double width = target.getBbWidth() * 0.5 + 0.35;
                for (int i = 0; i < 2; i++) {
                    double angle = level.random.nextDouble() * Math.PI * 2;
                    level.addParticle(ModRegistries.PILLAR_GLINT.get(), centre.x + Math.cos(angle) * width,
                            centre.y + (level.random.nextDouble() - 0.5) * target.getBbHeight() * 0.6,
                            centre.z + Math.sin(angle) * width, Math.cos(angle) * 0.15, 0.05, Math.sin(angle) * 0.15);
                }
            }
            return true;
        }

        void draw(VertexConsumer out, PoseStack.Pose pose, ClientLevel level, float partialTick) {
            Entity target = level.getEntity(entity);
            if (target == null) {
                return;
            }
            Vec3 centre = target.getPosition(partialTick).add(0, target.getBbHeight() * 0.5, 0);
            double radius = target.getBbWidth() * 0.5 + 0.35;
            for (int ring = 0; ring < SAW_RINGS.length; ring++) {
                double spin = (ring % 2 == 0 ? 1 : -1) * age * SAW_STEP;
                double y = target.getBbHeight() * SAW_RINGS[ring];
                for (int blade = 0; blade < SAW_BLADES; blade++) {
                    double angle = spin + Math.PI * 2 * blade / SAW_BLADES + ring * Math.PI / SAW_BLADES;
                    Vec3 out1 = new Vec3(Math.cos(angle), 0, Math.sin(angle));
                    Vec3 tangent = new Vec3(-out1.z, 0, out1.x).scale(ring % 2 == 0 ? 1 : -1);
                    Vec3 mid = centre.add(out1.scale(radius)).add(0, y, 0);
                    Vec3 tip = tangent.scale(SAW_LENGTH / 2);
                    Vec3 up = new Vec3(0, SAW_WIDTH / 2, 0);
                    Vec3 a = mid.add(tip).subtract(up);
                    Vec3 b = mid.add(tip).add(up);
                    Vec3 c = mid.subtract(tip).add(up);
                    Vec3 d = mid.subtract(tip).subtract(up);
                    float nx = (float) out1.x;
                    float nz = (float) out1.z;
                    PixelDraw.vertex(out, pose, (float) a.x, (float) a.y, (float) a.z, SAW_U1, SAW_TIP_V, 0xFFFFFFFF,
                            LightTexture.FULL_BRIGHT, nx, 0, nz);
                    PixelDraw.vertex(out, pose, (float) b.x, (float) b.y, (float) b.z, SAW_U0, SAW_TIP_V, 0xFFFFFFFF,
                            LightTexture.FULL_BRIGHT, nx, 0, nz);
                    PixelDraw.vertex(out, pose, (float) c.x, (float) c.y, (float) c.z, SAW_U0, SAW_HILT_V, 0xFFFFFFFF,
                            LightTexture.FULL_BRIGHT, nx, 0, nz);
                    PixelDraw.vertex(out, pose, (float) d.x, (float) d.y, (float) d.z, SAW_U1, SAW_HILT_V, 0xFFFFFFFF,
                            LightTexture.FULL_BRIGHT, nx, 0, nz);
                }
            }
        }
    }
}
