package org.gumel.jojoha.client.pillar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.CombatShake;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class FlairFx {
    public static final ResourceLocation PIXEL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/pillar_pixel.png");
    public static final ResourceLocation GLOW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/pillar_glow.png");
    public static final ResourceLocation BEAM_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/pillar_beam.png");

    public static final int WIND = 0;
    public static final int HEAT = 1;
    public static final int LIGHT = 2;
    public static final int FLESH = 3;

    public static final int[] WIND_COLOURS = {0xFFF4F6F2, 0xFFDDE4E2, 0xFFC9D2D0};
    public static final int[] SAND_COLOURS = {0xFFE6D3A8, 0xFFD2BB8A, 0xFFBFA272};
    public static final int[] EMBER_COLOURS = {0xFFFFE08A, 0xFFFFA43A, 0xFFE8562A, 0xFFB8281E};
    public static final int[] STEAM_COLOURS = {0xFFF0EEEA, 0xFFD9D6D0};
    public static final int[] GLINT_COLOURS = {0xFFFFFFFF, 0xFFFFF6C8, 0xFFFFE98A};
    public static final int[] BLOOD_COLOURS = {0xFF9E1414, 0xFF7A0E0E, 0xFFC42A22};
    public static final int[] BONE_COLOURS = {0xFFEDE6D2, 0xFFD6CDB4};

    private static final float PX = PixelDraw.PIXEL;
    private static final float LINE_GLOW = 0.3F;
    private static final float BIT_GLOW = 0.45F;
    private static final float RING_GLOW = 0.5F;
    private static final float STAR_GLOW = 0.6F;
    private static final double BIT_HALO = 4.0;
    private static final double LINE_HALO = 5.0;
    private static final double RING_HALO = 3.5;

    private static final List<Bit> BITS = new ArrayList<>();
    private static final List<Line> LINES = new ArrayList<>();
    private static final List<Ring> RINGS = new ArrayList<>();
    private static final List<Star> STARS = new ArrayList<>();
    private static final List<Charge> CHARGES = new ArrayList<>();
    private static final List<Sparkle> SPARKLES = new ArrayList<>();
    private static final List<Glow> GLOWS = new ArrayList<>();

    private static final RandomSource RANDOM = RandomSource.create();

    private FlairFx() {
    }

    public static void clear() {
        BITS.clear();
        LINES.clear();
        RINGS.clear();
        STARS.clear();
        CHARGES.clear();
        SPARKLES.clear();
        GLOWS.clear();
    }

    private static int pick(int[] colours) {
        return colours[RANDOM.nextInt(colours.length)];
    }

    private static double spread(double amount) {
        return (RANDOM.nextDouble() - 0.5) * 2.0 * amount;
    }

    private static Vec3 jitter(double amount) {
        return new Vec3(spread(amount), spread(amount), spread(amount));
    }

    public static void bit(Vec3 at, Vec3 velocity, int argb, int pixels, int life, double gravity, boolean lit) {
        BITS.add(new Bit(at, velocity, argb, pixels, life, gravity, lit));
    }

    public static void burst(Vec3 at, int count, double speed, int[] colours, int pixels, int life, double gravity,
                             boolean lit) {
        for (int i = 0; i < count; i++) {
            Vec3 dir = jitter(1.0);
            if (dir.lengthSqr() < 1.0E-4) {
                dir = new Vec3(0, 1, 0);
            }
            bit(at, dir.normalize().scale(speed * (0.4 + RANDOM.nextDouble() * 0.8)), pick(colours),
                    pixels + RANDOM.nextInt(2), life + RANDOM.nextInt(Math.max(1, life / 2)), gravity, lit);
        }
    }

    public static void spray(Vec3 at, Vec3 heading, double cone, int count, double speed, int[] colours, int pixels,
                             int life, double gravity, boolean lit) {
        Vec3 dir = heading.normalize();
        for (int i = 0; i < count; i++) {
            Vec3 v = dir.add(jitter(cone)).normalize().scale(speed * (0.5 + RANDOM.nextDouble() * 0.7));
            bit(at, v, pick(colours), pixels + RANDOM.nextInt(2), life + RANDOM.nextInt(Math.max(1, life / 2)),
                    gravity, lit);
        }
    }

    public static void line(Vec3 head, Vec3 velocity, double length, int argb, int life, Vec3 centre, Vec3 axis,
                            double pull, double swirl, int pixels) {
        line(head, velocity, length, argb, life, centre, axis, pull, swirl, pixels, LINE_GLOW);
    }

    public static void line(Vec3 head, Vec3 velocity, double length, int argb, int life, Vec3 centre, Vec3 axis,
                            double pull, double swirl, int pixels, float glow) {
        LINES.add(new Line(head, velocity, length, argb, life, centre, axis, pull, swirl, pixels, glow));
    }

    public static void glow(Vec3 at, double size, int life, int argb) {
        GLOWS.add(new Glow(at, size, life, argb));
    }

    public static void gust(Vec3 from, Vec3 to, int count, int[] colours) {
        Vec3 span = to.subtract(from);
        double length = span.length();
        if (length < 1.0E-3) {
            return;
        }
        Vec3 dir = span.scale(1.0 / length);
        Vec3[] basis = PixelDraw.basis(dir);
        double speed = Math.max(0.9, length / 6.0);
        for (int i = 0; i < count; i++) {
            double off = RANDOM.nextDouble() * length * 0.6;
            Vec3 head = from.add(dir.scale(off)).add(basis[1].scale(spread(0.55))).add(basis[2].scale(spread(0.45)));
            line(head, dir.scale(speed * (0.8 + RANDOM.nextDouble() * 0.5)), 0.5 + RANDOM.nextDouble() * 0.8,
                    pick(colours), 4 + RANDOM.nextInt(4), null, null, 0, 0, 1);
        }
    }

    public static void ring(Vec3 centre, Vec3 normal, double from, double to, int life, int argb, int pixels,
                            boolean lit) {
        Vec3[] basis = PixelDraw.basis(normal);
        RINGS.add(new Ring(centre, basis[1], basis[2], from, to, life, argb, pixels, lit));
    }

    public static void groundRing(Vec3 at, double from, double to, int life, int argb, int pixels, boolean lit) {
        RINGS.add(new Ring(at.add(0, 0.06, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1), from, to, life, argb, pixels,
                lit));
    }

    public static void star(Vec3 at, double size, int life, int argb) {
        STARS.add(new Star(at, size, life, argb));
    }

    public static void charge(int entityId, int style, int ticks) {
        CHARGES.add(new Charge(entityId, style, Math.max(2, ticks)));
    }

    public static void sparkle(int entityId, int style, int ticks) {
        SPARKLES.add(new Sparkle(entityId, style, ticks));
    }

    public static void shake(Vec3 at, float strength) {
        CombatShake.add(at.x, at.y, at.z, strength);
    }

    public static void crumbs(ClientLevel level, Vec3 at, int count, double spread, double lift) {
        BlockPos below = BlockPos.containing(at.x, at.y - 0.2, at.z);
        BlockState state = level.getBlockState(below);
        if (state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE) {
            below = below.below();
            state = level.getBlockState(below);
            if (state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE) {
                return;
            }
        }
        BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < count; i++) {
            level.addParticle(option, at.x + spread(spread), below.getY() + 1.05, at.z + spread(spread),
                    spread(0.15), lift * (0.5 + RANDOM.nextDouble()), spread(0.15));
        }
    }

    public static void crumbTrail(ClientLevel level, Vec3 from, Vec3 to, double every, int each) {
        Vec3 span = to.subtract(from);
        double length = span.length();
        int steps = Math.max(1, (int) (length / every));
        for (int i = 0; i <= steps; i++) {
            Vec3 at = from.add(span.scale(i / (double) steps));
            Vec3 ground = floor(level, at);
            if (ground != null) {
                crumbs(level, ground, each, 0.4, 0.18);
            }
        }
    }

    public static Vec3 floor(ClientLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        for (int i = 0; i < 4; i++) {
            BlockPos below = pos.below();
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                return new Vec3(at.x, pos.getY(), at.z);
            }
            pos = below;
        }
        return null;
    }

    public static void tick(ClientLevel level) {
        Iterator<Charge> charges = CHARGES.iterator();
        while (charges.hasNext()) {
            if (!charges.next().tick(level)) {
                charges.remove();
            }
        }
        Iterator<Sparkle> sparkles = SPARKLES.iterator();
        while (sparkles.hasNext()) {
            if (!sparkles.next().tick(level)) {
                sparkles.remove();
            }
        }
        BITS.removeIf(bit -> !bit.tick(level));
        LINES.removeIf(line -> !line.tick());
        RINGS.removeIf(ring -> ++ring.age > ring.life);
        STARS.removeIf(star -> ++star.age > star.life);
        GLOWS.removeIf(glow -> ++glow.age > glow.life);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, ClientLevel level) {
        if (BITS.isEmpty() && LINES.isEmpty() && RINGS.isEmpty() && STARS.isEmpty() && GLOWS.isEmpty()) {
            return;
        }
        Vec3 eye = camera.getPosition();
        Vec3 left = new Vec3(camera.getLeftVector());
        Vec3 up = new Vec3(camera.getUpVector());
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(PIXEL_TEXTURE));
        poseStack.pushPose();
        poseStack.translate(-eye.x, -eye.y, -eye.z);
        PoseStack.Pose pose = poseStack.last();
        for (Bit bit : BITS) {
            int pixels = bit.age > bit.life - 2 ? Math.max(1, bit.pixels - 1) : bit.pixels;
            double half = pixels * PX * 0.5;
            Vec3 l = left.scale(half);
            Vec3 u = up.scale(half);
            PixelDraw.quad(out, pose, bit.pos.add(l).add(u), bit.pos.add(l).subtract(u), bit.pos.subtract(l).subtract(u),
                    bit.pos.subtract(l).add(u), 0F, 1F, 0F, 1F, bit.argb, light(bit.lit, level, bit.pos));
        }
        for (Line line : LINES) {
            double shrink = line.age > line.life - 3 ? (line.life - line.age + 1) / 4.0 : 1.0;
            Vec3 dir = line.velocity.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : line.velocity.normalize();
            Vec3 tail = line.pos.subtract(dir.scale(line.length * shrink));
            PixelDraw.ribbon(out, pose, tail, line.pos, eye, line.pixels * PX, 0F, 1F, 0F, 1F, line.argb,
                    light(line.glow > 0, level, line.pos));
        }
        for (Ring ring : RINGS) {
            int steps = Math.max(1, ring.life);
            double t = Math.min(1.0, ring.age / (double) steps);
            double radius = ring.from + (ring.to - ring.from) * t;
            int pixels = Math.max(1, ring.pixels - (ring.age * ring.pixels) / (steps + 1));
            int segments = Math.max(12, (int) (radius * 10));
            int light = light(ring.lit, level, ring.centre);
            Vec3 prev = null;
            for (int s = 0; s <= segments; s++) {
                double a = Math.PI * 2 * s / segments;
                Vec3 at = ring.centre.add(ring.e1.scale(Math.cos(a) * radius)).add(ring.e2.scale(Math.sin(a) * radius));
                if (prev != null && (s % 4 != 3 || ring.age < 2)) {
                    PixelDraw.ribbon(out, pose, prev, at, eye, pixels * PX, 0F, 1F, 0F, 1F, ring.argb, light);
                }
                prev = at;
            }
        }
        for (Star star : STARS) {
            double scale = star.age <= 1 ? 1.0 : star.age == 2 ? 0.6 : 0.3;
            double size = star.size * scale;
            int width = star.age <= 1 ? 3 : star.age == 2 ? 2 : 1;
            Vec3 l = left.scale(size);
            Vec3 u = up.scale(size);
            PixelDraw.ribbon(out, pose, star.at.add(l), star.at.subtract(l), eye, width * PX, 0F, 1F, 0F, 1F, star.argb,
                    LightTexture.FULL_BRIGHT);
            PixelDraw.ribbon(out, pose, star.at.add(u), star.at.subtract(u), eye, width * PX, 0F, 1F, 0F, 1F, star.argb,
                    LightTexture.FULL_BRIGHT);
            Vec3 d1 = l.add(u).scale(0.4);
            Vec3 d2 = l.subtract(u).scale(0.4);
            PixelDraw.ribbon(out, pose, star.at.add(d1), star.at.subtract(d1), eye, Math.max(1, width - 1) * PX, 0F, 1F,
                    0F, 1F, star.argb, LightTexture.FULL_BRIGHT);
            PixelDraw.ribbon(out, pose, star.at.add(d2), star.at.subtract(d2), eye, Math.max(1, width - 1) * PX, 0F, 1F,
                    0F, 1F, star.argb, LightTexture.FULL_BRIGHT);
        }
        halos(buffers, pose, eye, left, up);
        poseStack.popPose();
    }

    private static void halos(MultiBufferSource buffers, PoseStack.Pose pose, Vec3 eye, Vec3 left, Vec3 up) {
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(GLOW_TEXTURE));
        for (Bit bit : BITS) {
            if (bit.lit) {
                float fade = bit.age > bit.life - 2 ? 0.5F : 1F;
                sprite(glow, pose, bit.pos, left, up, bit.pixels * PX * BIT_HALO, dim(bit.argb, BIT_GLOW * fade));
            }
        }
        for (Star star : STARS) {
            float fade = star.age <= 1 ? 1F : star.age == 2 ? 0.6F : 0.3F;
            sprite(glow, pose, star.at, left, up, star.size * 1.4 * fade, dim(star.argb, STAR_GLOW * fade));
        }
        for (Glow g : GLOWS) {
            float fade = (g.life - g.age + 1) / (float) (g.life + 1);
            double size = g.size * (g.age == 0 ? 0.7 : g.age == 1 ? 1.0 : 0.5 + 0.5 * fade);
            sprite(glow, pose, g.at, left, up, size, dim(g.argb, fade));
        }
        VertexConsumer beam = buffers.getBuffer(RenderType.eyes(BEAM_TEXTURE));
        for (Line line : LINES) {
            if (line.glow <= 0) {
                continue;
            }
            double shrink = line.age > line.life - 3 ? (line.life - line.age + 1) / 4.0 : 1.0;
            Vec3 dir = line.velocity.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : line.velocity.normalize();
            Vec3 tail = line.pos.subtract(dir.scale(line.length * shrink));
            PixelDraw.ribbon(beam, pose, tail, line.pos, eye, (float) (line.pixels * PX * LINE_HALO), 0F, 1F, 0F, 1F,
                    dim(line.argb, line.glow * (float) shrink), LightTexture.FULL_BRIGHT);
        }
        for (Ring ring : RINGS) {
            if (!ring.lit) {
                continue;
            }
            int steps = Math.max(1, ring.life);
            double t = Math.min(1.0, ring.age / (double) steps);
            double radius = ring.from + (ring.to - ring.from) * t;
            int pixels = Math.max(1, ring.pixels - (ring.age * ring.pixels) / (steps + 1));
            int segments = Math.max(12, (int) (radius * 10));
            int argb = dim(ring.argb, RING_GLOW * (float) (1.0 - t * 0.6));
            Vec3 prev = null;
            for (int s = 0; s <= segments; s++) {
                double a = Math.PI * 2 * s / segments;
                Vec3 at = ring.centre.add(ring.e1.scale(Math.cos(a) * radius)).add(ring.e2.scale(Math.sin(a) * radius));
                if (prev != null) {
                    PixelDraw.ribbon(beam, pose, prev, at, eye, (float) (pixels * PX * RING_HALO), 0F, 1F, 0F, 1F, argb,
                            LightTexture.FULL_BRIGHT);
                }
                prev = at;
            }
        }
    }

    private static void sprite(VertexConsumer out, PoseStack.Pose pose, Vec3 at, Vec3 left, Vec3 up, double size,
                               int argb) {
        double half = Math.max(PX, Math.round(size / PX) * PX) * 0.5;
        Vec3 l = left.scale(half);
        Vec3 u = up.scale(half);
        PixelDraw.quad(out, pose, at.add(l).add(u), at.add(l).subtract(u), at.subtract(l).subtract(u),
                at.subtract(l).add(u), 0F, 1F, 0F, 1F, argb, LightTexture.FULL_BRIGHT);
    }

    public static int dim(int argb, float strength) {
        float f = Math.max(0F, Math.min(1F, strength));
        int r = (int) (((argb >> 16) & 0xFF) * f);
        int g = (int) (((argb >> 8) & 0xFF) * f);
        int b = (int) ((argb & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int light(boolean lit, ClientLevel level, Vec3 at) {
        return lit ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(level, BlockPos.containing(at));
    }

    private static final class Bit {
        Vec3 pos;
        Vec3 velocity;
        final int argb;
        final int pixels;
        final int life;
        final double gravity;
        final boolean lit;
        int age;

        Bit(Vec3 pos, Vec3 velocity, int argb, int pixels, int life, double gravity, boolean lit) {
            this.pos = pos;
            this.velocity = velocity;
            this.argb = argb;
            this.pixels = Math.max(1, pixels);
            this.life = Math.max(1, life);
            this.gravity = gravity;
            this.lit = lit;
        }

        boolean tick(ClientLevel level) {
            if (++age > life) {
                return false;
            }
            Vec3 next = pos.add(velocity);
            if (gravity > 0 && !level.getBlockState(BlockPos.containing(next)).getCollisionShape(level,
                    BlockPos.containing(next)).isEmpty()) {
                velocity = new Vec3(velocity.x * 0.3, 0, velocity.z * 0.3);
                return true;
            }
            pos = next;
            velocity = velocity.scale(0.86).add(0, -gravity, 0);
            return true;
        }
    }

    private static final class Line {
        Vec3 pos;
        Vec3 velocity;
        final double length;
        final int argb;
        final int life;
        final Vec3 centre;
        final Vec3 axis;
        final double pull;
        final double swirl;
        final int pixels;
        final float glow;
        int age;

        Line(Vec3 pos, Vec3 velocity, double length, int argb, int life, Vec3 centre, Vec3 axis, double pull,
             double swirl, int pixels, float glow) {
            this.glow = glow;
            this.pos = pos;
            this.velocity = velocity;
            this.length = length;
            this.argb = argb;
            this.life = Math.max(1, life);
            this.centre = centre;
            this.axis = axis;
            this.pull = pull;
            this.swirl = swirl;
            this.pixels = Math.max(1, pixels);
        }

        boolean tick() {
            if (++age > life) {
                return false;
            }
            if (centre != null) {
                Vec3 offset = pos.subtract(centre);
                Vec3 around = axis == null ? Vec3.ZERO : axis.cross(offset);
                if (around.lengthSqr() > 1.0E-6) {
                    velocity = velocity.add(around.normalize().scale(swirl));
                }
                velocity = velocity.add(offset.scale(-pull)).scale(0.9);
                if (offset.lengthSqr() < 0.04 && pull > 0) {
                    return false;
                }
            }
            pos = pos.add(velocity);
            return true;
        }
    }

    private static final class Ring {
        final Vec3 centre;
        final Vec3 e1;
        final Vec3 e2;
        final double from;
        final double to;
        final int life;
        final int argb;
        final int pixels;
        final boolean lit;
        int age;

        Ring(Vec3 centre, Vec3 e1, Vec3 e2, double from, double to, int life, int argb, int pixels, boolean lit) {
            this.centre = centre;
            this.e1 = e1;
            this.e2 = e2;
            this.from = from;
            this.to = to;
            this.life = Math.max(1, life);
            this.argb = argb;
            this.pixels = Math.max(1, pixels);
            this.lit = lit;
        }
    }

    private static final class Star {
        final Vec3 at;
        final double size;
        final int life;
        final int argb;
        int age;

        Star(Vec3 at, double size, int life, int argb) {
            this.at = at;
            this.size = size;
            this.life = Math.max(1, life);
            this.argb = argb;
        }
    }

    private static final class Glow {
        final Vec3 at;
        final double size;
        final int life;
        final int argb;
        int age;

        Glow(Vec3 at, double size, int life, int argb) {
            this.at = at;
            this.size = size;
            this.life = Math.max(1, life);
            this.argb = argb;
        }
    }

    private static final class Charge {
        final int entityId;
        final int style;
        final int ticks;
        int age;

        Charge(int entityId, int style, int ticks) {
            this.entityId = entityId;
            this.style = style;
            this.ticks = ticks;
        }

        boolean tick(ClientLevel level) {
            Entity entity = level.getEntity(entityId);
            if (entity == null || !entity.isAlive() || ++age > ticks) {
                return false;
            }
            Vec3 hand = PillarClient.hand(entity, 1F);
            Vec3 chest = entity.position().add(0, entity.getBbHeight() * 0.6, 0);
            boolean last = age == ticks;
            float built = age / (float) ticks;
            switch (style) {
                case WIND -> glow(hand, 0.4 + built * 0.6, 1, dim(WIND_COLOURS[0], 0.25F + built * 0.25F));
                case HEAT -> glow(hand, 0.5 + built * 0.9, 1, dim(EMBER_COLOURS[1], 0.4F + built * 0.5F));
                case LIGHT -> glow(hand, 0.5 + built * 1.1, 1, dim(GLINT_COLOURS[1], 0.45F + built * 0.55F));
                default -> {
                }
            }
            if (last) {
                switch (style) {
                    case WIND -> glow(hand, 1.8, 4, 0xFFC8D8DC);
                    case HEAT -> glow(hand, 2.2, 5, 0xFFFF8A2A);
                    case LIGHT -> glow(hand, 2.8, 5, 0xFFFFF2B8);
                    default -> glow(chest, 1.4, 3, 0xFF6A0C0A);
                }
            }
            switch (style) {
                case WIND -> {
                    for (int i = 0; i < 3; i++) {
                        double angle = RANDOM.nextDouble() * Math.PI * 2;
                        double r = 1.6 + RANDOM.nextDouble() * 1.2;
                        Vec3 head = hand.add(Math.cos(angle) * r, spread(0.8), Math.sin(angle) * r);
                        line(head, Vec3.ZERO, 0.4 + RANDOM.nextDouble() * 0.4, pick(WIND_COLOURS), 14, hand,
                                new Vec3(0, 1, 0), 0.06, 0.09, 1);
                    }
                    if (age % 3 == 0 && entity.onGround()) {
                        crumbs(level, entity.position(), 2, 1.2, 0.12);
                    }
                    if (last) {
                        ring(hand, entity.getLookAngle(), 0.2, 1.1, 4, WIND_COLOURS[0], 2, true);
                    }
                }
                case HEAT -> {
                    for (int i = 0; i < 2; i++) {
                        Vec3 from = hand.add(jitter(0.9));
                        bit(from, hand.subtract(from).scale(0.18), pick(EMBER_COLOURS), 1, 6, 0, true);
                    }
                    if (age % 2 == 0) {
                        bit(chest.add(jitter(0.3)), new Vec3(spread(0.01), 0.05, spread(0.01)), pick(STEAM_COLOURS), 2,
                                10, -0.002, false);
                    }
                    if (last) {
                        star(hand, 0.35, 3, EMBER_COLOURS[1]);
                        burst(hand, 10, 0.18, EMBER_COLOURS, 1, 8, 0.01, true);
                    }
                }
                case LIGHT -> {
                    for (int i = 0; i < 2; i++) {
                        Vec3 from = hand.add(jitter(1.2));
                        bit(from, hand.subtract(from).scale(0.22), pick(GLINT_COLOURS), 1, 5, 0, true);
                    }
                    if (age % 4 == 0) {
                        star(hand.add(jitter(0.15)), 0.18, 2, GLINT_COLOURS[0]);
                    }
                    if (last) {
                        star(hand, 0.6, 3, GLINT_COLOURS[0]);
                    }
                }
                default -> {
                    if (age % 2 == 0) {
                        Vec3 from = chest.add(jitter(0.8));
                        bit(from, chest.subtract(from).scale(0.2), pick(BONE_COLOURS), 1, 5, 0, false);
                        bit(chest.add(jitter(0.3)), jitter(0.04), pick(BLOOD_COLOURS), 1, 8, 0.02, false);
                    }
                    if (last) {
                        burst(chest, 12, 0.2, BLOOD_COLOURS, 2, 10, 0.03, false);
                    }
                }
            }
            return true;
        }
    }

    private static final class Sparkle {
        final int entityId;
        final int style;
        final int ticks;
        int age;

        Sparkle(int entityId, int style, int ticks) {
            this.entityId = entityId;
            this.style = style;
            this.ticks = ticks;
        }

        boolean tick(ClientLevel level) {
            Entity entity = level.getEntity(entityId);
            if (entity == null || !entity.isAlive() || ++age > ticks) {
                return false;
            }
            Vec3 centre = entity.getBoundingBox().getCenter();
            switch (style) {
                case LIGHT -> {
                    spray(centre, jitter(1.0).add(0, 0.6, 0), 0.6, 3, 0.25, GLINT_COLOURS, 1, 6, 0.015, true);
                    if (age % 2 == 0) {
                        glow(centre, entity.getBbWidth() + 1.2 + RANDOM.nextDouble() * 0.4, 2, 0xFF8C8058);
                    }
                    if (age % 3 == 0) {
                        star(centre.add(jitter(0.4)), 0.25, 2, GLINT_COLOURS[0]);
                    }
                    if (age % 2 == 0) {
                        spray(centre, jitter(1.0), 0.8, 2, 0.15, BLOOD_COLOURS, 1, 8, 0.03, false);
                    }
                }
                case HEAT -> {
                    if (age % 2 == 0) {
                        bit(centre.add(jitter(entity.getBbWidth() * 0.5)), new Vec3(spread(0.02), 0.06, spread(0.02)),
                                pick(EMBER_COLOURS), 1, 8, -0.002, true);
                    }
                    if (age % 4 == 0) {
                        glow(centre, entity.getBbHeight() + 0.4, 3, 0xFF5A1C08);
                    }
                }
                default -> {
                    if (age % 2 == 0) {
                        spray(centre, jitter(1.0), 0.8, 2, 0.15, BLOOD_COLOURS, 1, 8, 0.03, false);
                    }
                }
            }
            return true;
        }
    }

    public static void peripheralWind(ClientLevel level, Entity viewer) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != viewer || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        Vec3 eye = viewer.getEyePosition();
        Vec3 look = viewer.getLookAngle();
        Vec3[] basis = PixelDraw.basis(look);
        double side = RANDOM.nextBoolean() ? 1.0 : -1.0;
        Vec3 head = eye.add(look.scale(1.6 + RANDOM.nextDouble())).add(basis[1].scale(side * (0.9 + RANDOM.nextDouble() * 0.4)))
                .add(basis[2].scale(spread(0.7)));
        line(head, look.scale(-0.55).add(basis[1].scale(side * 0.05)), 0.5 + RANDOM.nextDouble() * 0.5,
                pick(WIND_COLOURS), 5, null, null, 0, 0, 1, 0F);
    }
}
