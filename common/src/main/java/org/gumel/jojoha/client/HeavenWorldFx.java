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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.pillar.PillarClient;
import org.gumel.jojoha.client.pillar.PixelDraw;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.skill.moves.JudgementFistSkill;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class HeavenWorldFx {
    public static final int GOLD = 0xFFD45E;
    public static final int WHITE = 0xFFF8E8;
    public static final int VIOLET = 0x9A5CFF;
    public static final int BLACK = 0x000000;

    public static final int[] GOLDS = {0xFFF4C8, 0xFFD45E, 0xF0B040};
    public static final int[] VIOLETS = {0xE0D0FF, 0x9A5CFF, 0x7A40E0};
    public static final int[] WHITES = {0xFFFFFF, 0xFFF8E8, 0xFFF0D0};

    public static final int HOLD = 0;
    public static final int RISE = 1;
    public static final int DESCEND = 2;

    public static final ResourceLocation BAND = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/heaven_band.png");
    public static final ResourceLocation ORB = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/heaven_orb.png");
    public static final ResourceLocation FLAT = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/heaven_flat.png");

    private static final float WHITE_FADE_TICKS = 30F;
    private static final int MAX_SPARKS = 1600;
    private static final Map<Integer, int[]> STAND_TIERS = new HashMap<>();
    private static final Map<Integer, int[]> TELEGRAPHS = new HashMap<>();
    private static final int[] SHAFTS_BY_TIER = {5, 7, 9, 12};
    private static final float[] SHAFT_HEIGHT_BY_TIER = {1.3F, 1.7F, 2.2F, 2.9F};
    private static final float[] SHAFT_ALPHA_BY_TIER = {0.22F, 0.3F, 0.38F, 0.48F};
    private static final float SHAFT_WIDTH = 0.34F;
    private static final float SHAFT_TURN = 0.018F;
    private static final float COLUMN_HEIGHT = 18F;
    private static final float COLUMN_WIDTH = 1.5F;
    private static final float NEAR_START = 0.15F;
    private static final float NEAR_SPAN = 0.5F;
    private static final int SERVANT_GLOW = 0xFFC83A;

    private static final List<Shape> SHAPES = new ArrayList<>();
    private static final List<Spark> SPARKS = new ArrayList<>();
    private static final Map<Integer, Integer> HALOS = new HashMap<>();
    private static final Map<Integer, Integer> SERVANTS = new HashMap<>();
    private static final Map<Integer, Integer> ERASED = new HashMap<>();
    private static final Map<Integer, Integer> WHITENED = new HashMap<>();
    private static final Map<Integer, Integer> HIGH = new HashMap<>();
    private static final Map<Integer, Integer> CHARGED = new HashMap<>();
    private static final Map<Integer, int[]> WINDUP = new HashMap<>();

    private HeavenWorldFx() {
    }

    public static void ring(Vec3 at, float from, float to, float width, int colour, float alpha, int life) {
        SHAPES.add(new Ring(at, -1, from, to, width, colour, life));
    }

    public static void ringOn(int entityId, float radius, float width, int colour, float alpha, int life) {
        SHAPES.add(new Ring(Vec3.ZERO, entityId, radius, radius, width, colour, life));
    }

    public static void pillar(Vec3 at, float radius, float height, int colour, float alpha, int life, int style) {
        SHAPES.add(new Pillar(at, radius, height, colour, life, style));
    }

    public static void sphere(Vec3 at, float radius, int colour, float alpha, int grow, int life, boolean dark) {
        SHAPES.add(new Dome(at, radius, colour, grow, life, dark));
    }

    public static void beam(int fromId, int toId, Vec3 fallback, int colour, float width, int life, boolean wavy) {
        SHAPES.add(new Beam(fromId, toId, null, fallback, colour, width, life, wavy));
    }

    public static void line(Vec3 from, Vec3 to, int colour, float width, int life) {
        SHAPES.add(new Beam(-1, -1, from, to, colour, width, life, false));
    }

    public static void cracks(Vec3 at, float radius, int life) {
        SHAPES.add(new Fissures(at, radius, life));
    }

    public static void flare(Vec3 at, float size, int colour, int life) {
        SHAPES.add(new Flare(at, size, colour, life, false));
    }

    public static void glint(Vec3 at, float size, int colour, int life) {
        SHAPES.add(new Flare(at, size, colour, life, true));
    }

    public static void spark(Vec3 at, Vec3 velocity, int colour, float size, int life, double gravity, boolean streak) {
        if (SPARKS.size() >= MAX_SPARKS) {
            return;
        }
        SPARKS.add(new Spark(at, velocity, colour, size, life, gravity, streak));
    }

    public static void burst(Vec3 at, int count, double speed, int[] colours, float size, int life, double gravity,
                             boolean streak) {
        RandomSource random = random();
        if (random == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
            if (direction.lengthSqr() < 1.0E-4) {
                continue;
            }
            Vec3 velocity = direction.normalize().scale(speed * (0.4 + random.nextDouble() * 0.8));
            spark(at, velocity, pick(colours, random), size * (0.6F + random.nextFloat() * 0.7F),
                    life / 2 + random.nextInt(Math.max(1, life)), gravity, streak);
        }
    }

    public static void spray(Vec3 at, Vec3 axis, double spread, int count, double speed, int[] colours, float size,
                             int life, double gravity, boolean streak) {
        RandomSource random = random();
        if (random == null) {
            return;
        }
        Vec3 forward = axis.normalize();
        for (int i = 0; i < count; i++) {
            Vec3 velocity = forward.add(random.nextGaussian() * spread, random.nextGaussian() * spread,
                    random.nextGaussian() * spread).normalize().scale(speed * (0.5 + random.nextDouble() * 0.7));
            spark(at, velocity, pick(colours, random), size * (0.6F + random.nextFloat() * 0.7F),
                    life / 2 + random.nextInt(Math.max(1, life)), gravity, streak);
        }
    }

    public static void gather(Vec3 at, double reach, int count, int[] colours, float size, int life) {
        RandomSource random = random();
        if (random == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
            if (direction.lengthSqr() < 1.0E-4) {
                continue;
            }
            Vec3 from = at.add(direction.normalize().scale(reach * (0.6 + random.nextDouble() * 0.4)));
            spark(from, at.subtract(from).scale(1.0 / life), pick(colours, random), size, life, 0.0, true);
        }
    }

    public static void windup(int standId, int ticks) {
        WINDUP.put(standId, new int[]{ticks, ticks});
        CHARGED.remove(standId);
    }

    public static void halo(int entityId, int ticks) {
        HALOS.merge(entityId, ticks, Math::max);
    }

    public static void standTier(int standId, int tier, int ticks) {
        STAND_TIERS.put(standId, new int[]{Mth.clamp(tier, 0, 3), ticks});
    }

    public static int tierOf(StandEntity stand) {
        int[] tier = STAND_TIERS.get(stand.getId());
        return tier == null ? 0 : tier[0];
    }

    public static void telegraph(int standId, int ticks) {
        TELEGRAPHS.put(standId, new int[]{ticks, Math.max(1, ticks)});
    }

    public static float telegraphOf(StandEntity stand, float partialTick) {
        int[] left = TELEGRAPHS.get(stand.getId());
        if (left == null) {
            return 0F;
        }
        float t = Mth.clamp((left[0] - partialTick) / left[1], 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    public static void high(int standId, int ticks) {
        if (ticks <= 0) {
            HIGH.remove(standId);
            return;
        }
        HIGH.put(standId, ticks);
    }

    public static boolean high(StandEntity stand) {
        return HIGH.containsKey(stand.getId());
    }

    public static void servant(int entityId, int ticks) {
        if (ticks <= 0) {
            SERVANTS.remove(entityId);
            return;
        }
        SERVANTS.put(entityId, ticks);
    }

    public static boolean serving(Entity entity) {
        return !SERVANTS.isEmpty() && SERVANTS.containsKey(entity.getId());
    }

    public static int servantColour(Entity entity) {
        return serving(entity) ? SERVANT_GLOW : -1;
    }

    public static void erased(int entityId, int ticks) {
        ERASED.merge(entityId, ticks, Math::max);
    }

    public static void whiten(int entityId, int ticks) {
        WHITENED.put(entityId, ticks);
    }

    public static float whiteness(Entity entity) {
        if (WHITENED.isEmpty()) {
            return 0F;
        }
        Integer left = WHITENED.get(entity.getId());
        return left == null ? 0F : Mth.clamp(left / WHITE_FADE_TICKS, 0F, 1F);
    }

    public static void clear() {
        SHAPES.clear();
        SPARKS.clear();
        HALOS.clear();
        SERVANTS.clear();
        ERASED.clear();
        WHITENED.clear();
        HIGH.clear();
        CHARGED.clear();
        WINDUP.clear();
        STAND_TIERS.clear();
        TELEGRAPHS.clear();
        HeavenSky.clear();
        HeavenGhostLayer.clear();
        HeavenFx.clear();
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            clear();
            return;
        }
        HeavenGhostLayer.tick(level);
        HeavenFx.tick();
        SHAPES.removeIf(shape -> ++shape.age >= shape.life);
        SPARKS.removeIf(Spark::tick);
        RandomSource random = level.getRandom();
        countDown(HALOS, level, (entity, left) -> haloMotes(entity, random));
        countDown(SERVANTS, level, (entity, left) -> servantMotes(entity, random));
        countDown(ERASED, level, (entity, left) -> erasedFlakes(entity, random));
        countDown(HIGH, level, (entity, left) -> {
        });
        WINDUP.values().removeIf(left -> --left[0] < 0);
        STAND_TIERS.values().removeIf(left -> --left[1] < 0);
        TELEGRAPHS.values().removeIf(left -> --left[0] < 0);
        HeavenSky.tick();
        WHITENED.replaceAll((id, left) -> left - 1);
        WHITENED.values().removeIf(left -> left <= 0);
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof StandEntity stand && heavenly(stand)) {
                aura(level, stand, random);
            }
        }
    }

    public static boolean heavenly(StandEntity stand) {
        return Heaven.ID.equals(stand.getStandType().id()) && !stand.isRemoved()
                && StandViewAlpha.of(stand) > 0.05F;
    }

    private interface Each {
        void at(Entity entity, int left);
    }

    private static void countDown(Map<Integer, Integer> marks, ClientLevel level, Each each) {
        Iterator<Map.Entry<Integer, Integer>> iterator = marks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> mark = iterator.next();
            int left = mark.getValue() - 1;
            Entity entity = level.getEntity(mark.getKey());
            if (left <= 0 || entity == null || entity.isRemoved()) {
                iterator.remove();
                continue;
            }
            mark.setValue(left);
            each.at(entity, left);
        }
    }

    private static RandomSource random() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? null : level.getRandom();
    }

    private static int pick(int[] colours, RandomSource random) {
        return colours[random.nextInt(colours.length)];
    }

    private static void aura(ClientLevel level, StandEntity stand, RandomSource random) {
        boolean high = high(stand);
        long time = level.getGameTime() + stand.getId();
        charge(stand, random, time);
        double width = stand.getBbWidth() * 0.8;
        int motes = 1;
        if (time % (4 - tierOf(stand)) == 0) {
            for (int i = 0; i < motes; i++) {
                Vec3 at = stand.position().add((random.nextDouble() - 0.5) * width * 2,
                        random.nextDouble() * stand.getBbHeight(), (random.nextDouble() - 0.5) * width * 2);
                spark(at, new Vec3((random.nextDouble() - 0.5) * 0.01, 0.025 + random.nextDouble() * 0.03,
                                (random.nextDouble() - 0.5) * 0.01),
                        pick(high && random.nextBoolean() ? VIOLETS : GOLDS, random), 0.06F + random.nextFloat() * 0.05F,
                        18 + random.nextInt(14), -0.0008, false);
            }
        }
    }

    private static void charge(StandEntity stand, RandomSource random, long time) {
        int[] windup = WINDUP.get(stand.getId());
        if (!stand.isCharging() && windup == null) {
            CHARGED.remove(stand.getId());
            return;
        }
        int charged = CHARGED.merge(stand.getId(), 1, Integer::sum);
        float built = built(stand.getId(), charged);
        Vec3 hand = PillarClient.hand(stand, 1F);
        gather(hand, 1.6 + built, 2, GOLDS, 0.07F, 6);
        if (built >= 1F && time % 5 == 0) {
            glint(hand, 0.7F, WHITE, 5);
        }
    }

    private static float built(int standId, int charged) {
        int[] windup = WINDUP.get(standId);
        float full = windup != null ? Math.max(1, windup[1])
                : JudgementFistSkill.MAX_CHARGE_TICKS;
        return Math.min(1F, charged / full);
    }

    private static void haloMotes(Entity entity, RandomSource random) {
        if (entity.tickCount % 3 != 0) {
            return;
        }
        double angle = random.nextDouble() * Math.PI * 2.0;
        double reach = 0.4 + random.nextDouble() * 0.6;
        Vec3 at = entity.position().add(Math.cos(angle) * reach, 0.1 + random.nextDouble() * 0.6,
                Math.sin(angle) * reach);
        spark(at, new Vec3(0, 0.03 + random.nextDouble() * 0.025, 0), pick(GOLDS, random), 0.06F, 20, -0.0006,
                false);
    }

    private static void servantMotes(Entity entity, RandomSource random) {
        if (entity.tickCount % 5 != 0) {
            return;
        }
        Vec3 at = entity.position().add((random.nextDouble() - 0.5) * entity.getBbWidth(),
                random.nextDouble() * entity.getBbHeight(), (random.nextDouble() - 0.5) * entity.getBbWidth());
        spark(at, new Vec3(0, 0.025, 0), pick(GOLDS, random), 0.05F, 16, 0.0, false);
    }

    private static void erasedFlakes(Entity entity, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            Vec3 at = entity.position().add((random.nextDouble() - 0.5) * entity.getBbWidth() * 1.4,
                    random.nextDouble() * entity.getBbHeight(), (random.nextDouble() - 0.5) * entity.getBbWidth() * 1.4);
            spark(at, new Vec3(0, 0.04 + random.nextDouble() * 0.03, 0), random.nextBoolean() ? WHITE : VIOLETS[0],
                    0.07F, 14 + random.nextInt(8), -0.002, random.nextInt(3) == 0);
        }
    }

    static int scale(int rgb, float strength) {
        float f = Mth.clamp(strength, 0F, 1F);
        int r = (int) (((rgb >> 16) & 0xFF) * f);
        int g = (int) (((rgb >> 8) & 0xFF) * f);
        int b = (int) ((rgb & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static int tone(int rgb, float strength) {
        return scale(rgb, strength);
    }

    private static float easeOut(float t) {
        float u = 1F - Mth.clamp(t, 0F, 1F);
        return 1F - u * u * u;
    }

    private static float envelope(float t, float in) {
        float rise = Mth.clamp(t / in, 0F, 1F);
        float fall = 1F - Mth.clamp(t, 0F, 1F);
        return rise * (float) Math.pow(fall, 1.3);
    }

    private record Draw(VertexConsumer band, VertexConsumer orb, VertexConsumer flat, PoseStack.Pose pose, Vec3 eye,
                        Vec3 left, Vec3 up, float time) {
        private void vertex(VertexConsumer out, Vec3 p, float u, float v, int argb) {
            PixelDraw.vertex(out, pose, (float) p.x, (float) p.y, (float) p.z, u, v, argb, LightTexture.FULL_BRIGHT,
                    0F, 1F, 0F);
        }

        void quad(VertexConsumer out, Vec3 a0, Vec3 a1, Vec3 b1, Vec3 b0, float v0, float v1, int ca, int cb) {
            vertex(out, a0, 0F, v0, ca);
            vertex(out, a1, 0F, v1, ca);
            vertex(out, b1, 1F, v1, cb);
            vertex(out, b0, 1F, v0, cb);
            vertex(out, b0, 1F, v0, cb);
            vertex(out, b1, 1F, v1, cb);
            vertex(out, a1, 0F, v1, ca);
            vertex(out, a0, 0F, v0, ca);
        }

        int near(Vec3 at, float size, int argb) {
            double distance = at.distanceTo(eye);
            float fade = (float) Mth.clamp((distance - size * NEAR_START) / Math.max(0.3, size * NEAR_SPAN), 0.0, 1.0);
            return fade >= 1F ? argb : scale(argb, fade);
        }

        void ribbon(Vec3 a, Vec3 b, float widthA, float widthB, int ca, int cb) {
            float wide = Math.max(widthA, widthB);
            Vec3 mid = a.add(b).scale(0.5);
            ca = near(mid, wide, ca);
            cb = near(mid, wide, cb);
            Vec3 along = b.subtract(a);
            Vec3 side = along.cross(eye.subtract(a.add(b).scale(0.5)));
            if (side.lengthSqr() < 1.0E-10) {
                return;
            }
            side = side.normalize();
            quad(band, a.add(side.scale(widthA / 2)), a.subtract(side.scale(widthA / 2)),
                    b.subtract(side.scale(widthB / 2)), b.add(side.scale(widthB / 2)), 0F, 1F, ca, cb);
        }

        void lying(Vec3 a, Vec3 b, float widthA, float widthB, int ca, int cb) {
            Vec3 along = b.subtract(a);
            Vec3 side = new Vec3(-along.z, 0, along.x);
            if (side.lengthSqr() < 1.0E-10) {
                return;
            }
            side = side.normalize();
            quad(band, a.add(side.scale(widthA / 2)), a.subtract(side.scale(widthA / 2)),
                    b.subtract(side.scale(widthB / 2)), b.add(side.scale(widthB / 2)), 0F, 1F, ca, cb);
        }

        void orb(Vec3 centre, float size, int argb) {
            argb = near(centre, size, argb);
            Vec3 l = left.scale(size / 2);
            Vec3 u = up.scale(size / 2);
            vertex(orb, centre.add(l).add(u), 0F, 0F, argb);
            vertex(orb, centre.add(l).subtract(u), 0F, 1F, argb);
            vertex(orb, centre.subtract(l).subtract(u), 1F, 1F, argb);
            vertex(orb, centre.subtract(l).add(u), 1F, 0F, argb);
        }

        void pool(Vec3 centre, float size, int argb) {
            argb = near(centre, size, argb);
            double h = size / 2;
            Vec3 a = centre.add(-h, 0, -h);
            Vec3 b = centre.add(-h, 0, h);
            Vec3 c = centre.add(h, 0, h);
            Vec3 d = centre.add(h, 0, -h);
            vertex(orb, a, 0F, 0F, argb);
            vertex(orb, b, 0F, 1F, argb);
            vertex(orb, c, 1F, 1F, argb);
            vertex(orb, d, 1F, 0F, argb);
            vertex(orb, d, 1F, 0F, argb);
            vertex(orb, c, 1F, 1F, argb);
            vertex(orb, b, 0F, 1F, argb);
            vertex(orb, a, 0F, 0F, argb);
        }

        void annulus(Vec3 centre, double radius, double half, int argb, int segments) {
            double inner = Math.max(0.0, radius - half);
            double outer = radius + half;
            for (int s = 0; s < segments; s++) {
                double a0 = s * Math.PI * 2 / segments;
                double a1 = (s + 1) * Math.PI * 2 / segments;
                quad(band, around(centre, inner, a0), around(centre, outer, a0), around(centre, outer, a1),
                        around(centre, inner, a1), 0F, 1F, argb, argb);
            }
        }

        void wall(Vec3 centre, double radius, double height, int argb, int segments) {
            Vec3 lift = new Vec3(0, height, 0);
            for (int s = 0; s < segments; s++) {
                Vec3 a = around(centre, radius, s * Math.PI * 2 / segments);
                Vec3 b = around(centre, radius, (s + 1) * Math.PI * 2 / segments);
                quad(band, a, a.add(lift), b.add(lift), b, 0.5F, 1F, argb, argb);
            }
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        List<StandEntity> lit = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof StandEntity stand && heavenly(stand)) {
                lit.add(stand);
            }
        }
        if (SHAPES.isEmpty() && SPARKS.isEmpty() && HALOS.isEmpty() && lit.isEmpty()) {
            return;
        }
        Vec3 eye = camera.getPosition();
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        poseStack.pushPose();
        poseStack.translate(-eye.x, -eye.y, -eye.z);
        Draw draw = new Draw(buffers.getBuffer(RenderType.eyes(BAND)), buffers.getBuffer(RenderType.eyes(ORB)),
                buffers.getBuffer(RenderType.eyes(FLAT)), poseStack.last(), eye,
                new Vec3(left.x(), left.y(), left.z()), new Vec3(up.x(), up.y(), up.z()),
                level.getGameTime() + partialTick);
        for (Shape shape : SHAPES) {
            shape.draw(draw, level, partialTick);
        }
        for (Spark spark : SPARKS) {
            spark.draw(draw, partialTick);
        }
        for (StandEntity stand : lit) {
            glow(draw, stand, partialTick);
        }
        poseStack.popPose();
    }

    private static Vec3 around(Vec3 centre, double radius, double angle) {
        return centre.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
    }

    private static void glow(Draw draw, StandEntity stand, float partialTick) {
        float view = StandViewAlpha.of(stand);
        boolean high = high(stand);
        int colour = high ? VIOLET : GOLD;
        int tier = tierOf(stand);
        float flare = telegraphOf(stand, partialTick);
        Vec3 feet = stand.getPosition(partialTick);
        shafts(draw, stand, feet, colour, tier, flare, view);
        if (tier >= 2 || flare > 0.01F) {
            float breath = 0.8F + 0.2F * Mth.sin(draw.time() * 0.06F + stand.getId());
            float strength = (tier >= 3 ? 0.2F : tier >= 2 ? 0.12F : 0F) + 0.25F * flare;
            draw.ribbon(feet.add(0, COLUMN_HEIGHT, 0), feet, COLUMN_WIDTH * 0.4F, COLUMN_WIDTH * (1F + 0.4F * flare),
                    0xFF000000, tone(high ? VIOLET : WHITE, strength * breath * view));
        }
        Integer charged = CHARGED.get(stand.getId());
        if (charged != null) {
            float built = built(stand.getId(), charged);
            Vec3 hand = PillarClient.hand(stand, partialTick);
            float flicker = 0.9F + 0.1F * Mth.sin(draw.time() * 1.7F);
            draw.orb(hand, 0.6F + built * 1.6F, tone(GOLD, (0.5F + built * 0.5F) * flicker));
            draw.orb(hand, 0.3F + built * 0.6F, tone(WHITE, 0.7F + built * 0.3F));
        }
    }

    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    private static void shafts(Draw draw, StandEntity stand, Vec3 feet, int colour, int tier, float flare, float view) {
        int count = SHAFTS_BY_TIER[tier];
        float reach = stand.getBbHeight() * (SHAFT_HEIGHT_BY_TIER[tier] + 0.8F * flare);
        float alpha = (SHAFT_ALPHA_BY_TIER[tier] + 0.45F * flare) * view;
        float time = draw.time();
        Vec3 heart = feet.add(0.0, stand.getBbHeight() * 0.6, 0.0);
        float root = SHAFT_WIDTH * stand.getBbWidth() * 0.25F;
        float tip = SHAFT_WIDTH * stand.getBbWidth() * (1.6F + 0.8F * flare);
        for (int i = 0; i < count; i++) {
            double yaw = i * GOLDEN_ANGLE + time * SHAFT_TURN;
            double lift = Math.toRadians(-20.0 + 95.0 * ((i * 0.618034 + 0.17) % 1.0));
            Vec3 out = new Vec3(Math.cos(yaw) * Math.cos(lift), Math.sin(lift), Math.sin(yaw) * Math.cos(lift));
            float breath = 0.55F + 0.45F * Mth.sin(time * 0.09F + i * 2.3F + stand.getId());
            float length = reach * (0.7F + 0.3F * Mth.sin(time * 0.05F + i * 1.7F));
            Vec3 start = heart.add(out.scale(stand.getBbWidth() * 0.3));
            Vec3 end = heart.add(out.scale(length));
            draw.ribbon(start, end, root, tip, tone(colour, alpha * breath), 0xFF000000);
            draw.ribbon(start, heart.add(out.scale(length * 0.55)), root * 0.5F, tip * 0.3F,
                    tone(WHITE, alpha * breath * (tier >= 3 ? 0.9F : 0.55F)), 0xFF000000);
        }
    }

    private abstract static class Shape {
        protected final int colour;
        protected final int life;
        protected int age;

        private Shape(int colour, int life) {
            this.colour = colour;
            this.life = Math.max(1, life);
        }

        protected float progress(float partialTick) {
            return Mth.clamp((age + partialTick) / life, 0F, 1F);
        }

        protected abstract void draw(Draw draw, ClientLevel level, float partialTick);
    }

    private static final class Ring extends Shape {
        private final Vec3 at;
        private final int follow;
        private final float from;
        private final float to;
        private final float width;

        private Ring(Vec3 at, int follow, float from, float to, float width, int colour, int life) {
            super(colour, life);
            this.at = at;
            this.follow = follow;
            this.from = from;
            this.to = to;
            this.width = Math.max(0.15F, width);
        }

        @Override
        protected void draw(Draw draw, ClientLevel level, float partialTick) {
            Vec3 centre = at.add(0, 0.06, 0);
            if (follow >= 0) {
                Entity entity = level.getEntity(follow);
                if (entity == null) {
                    return;
                }
                centre = entity.getPosition(partialTick).add(0.0, 0.06, 0.0);
            }
            float t = progress(partialTick);
            double radius = Mth.lerp(easeOut(t), from, to);
            float k = follow >= 0 ? Math.min(1F, (age + partialTick) / 3F) * Math.min(1F, (life - age) / 4F)
                    : envelope(t, 0.08F);
            if (radius <= 0.05 || k <= 0.01F) {
                return;
            }
            double half = width * 0.6 * (1F - 0.45F * t);
            int segments = Mth.clamp((int) (radius * 10), 32, 160);
            draw.annulus(centre, radius, half, tone(colour, k * 0.9F), segments);
            draw.annulus(centre, radius, half * 0.3, tone(WHITE, k * 0.8F), segments);
            if (follow < 0) {
                draw.wall(centre, radius, width * 1.4 * (1F - t), tone(colour, k * 0.55F), segments);
            }
        }
    }

    private static final class Pillar extends Shape {
        private static final int SEGMENTS = 12;
        private final Vec3 at;
        private final float radius;
        private final float height;
        private final int style;

        private Pillar(Vec3 at, float radius, float height, int colour, int life, int style) {
            super(colour, life);
            this.at = at;
            this.radius = radius;
            this.height = height;
            this.style = style;
        }

        @Override
        protected void draw(Draw draw, ClientLevel level, float partialTick) {
            float t = progress(partialTick);
            float grown = easeOut((age + partialTick) / Math.max(1F, life * 0.25F));
            double bottom = at.y;
            double top = at.y + height;
            if (style == RISE) {
                top = at.y + height * grown;
            } else if (style == DESCEND) {
                bottom = top - height * grown;
            }
            if (top - bottom < 0.05) {
                return;
            }
            float swell = 1F + 0.5F * Math.max(0F, 1F - (age + partialTick) / 3F);
            float thin = t < 0.55F ? 1F : 1F - easeOut((t - 0.55F) / 0.45F);
            float width = radius * 2F * swell * thin;
            float k = envelope(t, 0.05F);
            if (width <= 0.02F || k <= 0.01F) {
                return;
            }
            for (int s = 0; s < SEGMENTS; s++) {
                double y0 = Mth.lerp(s / (double) SEGMENTS, bottom, top);
                double y1 = Mth.lerp((s + 1) / (double) SEGMENTS, bottom, top);
                float ka = k * along(s / (float) SEGMENTS, y0, draw.time());
                float kb = k * along((s + 1) / (float) SEGMENTS, y1, draw.time());
                Vec3 a = new Vec3(at.x, y0, at.z);
                Vec3 b = new Vec3(at.x, y1, at.z);
                draw.ribbon(a, b, width * 2.6F, width * 2.6F, tone(colour, ka * 0.3F), tone(colour, kb * 0.3F));
                draw.ribbon(a, b, width, width, tone(colour, ka), tone(colour, kb));
                draw.ribbon(a, b, width * 0.35F, width * 0.35F, tone(WHITE, ka), tone(WHITE, kb));
            }
            Vec3 base = new Vec3(at.x, style == DESCEND ? bottom : at.y, at.z);
            draw.orb(base.add(0, 0.3, 0), width * 3F, tone(colour, k * 0.8F));
            draw.pool(new Vec3(at.x, at.y + 0.05, at.z), width * 5F, tone(colour, k * 0.6F));
        }

        private float along(float f, double y, float time) {
            float tip = switch (style) {
                case RISE -> Mth.clamp((1F - f) / 0.2F, 0F, 1F);
                case DESCEND -> Mth.clamp(f / 0.2F, 0F, 1F) * Mth.clamp((1F - f) / 0.1F, 0F, 1F);
                default -> Mth.clamp((1F - f) / 0.15F, 0F, 1F);
            };
            return tip * (0.82F + 0.18F * Mth.sin((float) y * 1.1F - time * 0.7F));
        }
    }

    private static final class Dome extends Shape {
        private static final int ROWS = 10;
        private static final int COLUMNS = 28;
        private final Vec3 at;
        private final float radius;
        private final int grow;
        private final boolean dark;

        private Dome(Vec3 at, float radius, int colour, int grow, int life, boolean dark) {
            super(colour, life);
            this.at = at;
            this.radius = radius;
            this.grow = Math.max(1, grow);
            this.dark = dark;
        }

        @Override
        protected void draw(Draw draw, ClientLevel level, float partialTick) {
            float now = age + partialTick;
            double r = radius * easeOut(now / grow);
            if (r <= 0.2) {
                return;
            }
            float out = Mth.clamp((life - now) / Math.min(20F, life * 0.3F), 0F, 1F);
            float k = (dark ? 0.55F + 0.45F * Mth.clamp(now / grow, 0F, 1F) : 0.6F) * out;
            if (dark && now > grow * 0.6F) {
                k *= 0.8F + 0.2F * Mth.sin(now * 2.2F);
            }
            int rim = dark ? VIOLETS[0] : colour;
            Vec3[][] points = new Vec3[ROWS + 1][COLUMNS + 1];
            int[][] shades = new int[ROWS + 1][COLUMNS + 1];
            for (int row = 0; row <= ROWS; row++) {
                double phi = row * (Math.PI / 2) / ROWS;
                for (int col = 0; col <= COLUMNS; col++) {
                    double yaw = col * Math.PI * 2 / COLUMNS;
                    Vec3 normal = new Vec3(Math.cos(yaw) * Math.cos(phi), Math.sin(phi), Math.sin(yaw) * Math.cos(phi));
                    Vec3 p = at.add(normal.scale(r));
                    Vec3 toEye = draw.eye().subtract(p);
                    double facing = toEye.lengthSqr() < 1.0E-6 ? 0.0 : Math.abs(normal.dot(toEye.normalize()));
                    float edge = (float) Math.pow(1.0 - facing, 2.4);
                    float ripple = 0.85F + 0.15F * Mth.sin((float) phi * 7F - draw.time() * 0.12F + (float) yaw * 2F);
                    float base = row == 0 ? 0.35F : 0.04F;
                    points[row][col] = p;
                    shades[row][col] = tone(rim, k * Math.min(1F, (edge * 0.85F + base) * ripple));
                }
            }
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLUMNS; col++) {
                    shell(draw, points[row][col], points[row + 1][col], points[row + 1][col + 1], points[row][col + 1],
                            shades[row][col], shades[row + 1][col], shades[row + 1][col + 1], shades[row][col + 1]);
                }
            }
            int segments = Mth.clamp((int) (r * 8), 48, 160);
            draw.annulus(at.add(0, 0.06, 0), r, 0.5, tone(rim, k * 0.8F), segments);
            draw.annulus(at.add(0, 0.06, 0), r, 0.15, tone(WHITE, k * 0.7F), segments);
        }

        private static void shell(Draw draw, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int ca, int cb, int cc, int cd) {
            VertexConsumer out = draw.flat();
            draw.vertex(out, a, 0F, 0F, ca);
            draw.vertex(out, b, 0F, 1F, cb);
            draw.vertex(out, c, 1F, 1F, cc);
            draw.vertex(out, d, 1F, 0F, cd);
            draw.vertex(out, d, 1F, 0F, cd);
            draw.vertex(out, c, 1F, 1F, cc);
            draw.vertex(out, b, 0F, 1F, cb);
            draw.vertex(out, a, 0F, 0F, ca);
        }
    }

    private static final class Beam extends Shape {
        private final int fromId;
        private final int toId;
        private final Vec3 start;
        private final Vec3 fallback;
        private final float width;
        private final boolean wavy;

        private Beam(int fromId, int toId, Vec3 start, Vec3 fallback, int colour, float width, int life, boolean wavy) {
            super(colour, life);
            this.fromId = fromId;
            this.toId = toId;
            this.start = start;
            this.fallback = fallback;
            this.width = Math.max(0.06F, width * 1.6F);
            this.wavy = wavy;
        }

        @Override
        protected void draw(Draw draw, ClientLevel level, float partialTick) {
            Vec3 a = start;
            if (a == null) {
                Entity from = level.getEntity(fromId);
                if (from == null) {
                    return;
                }
                a = from.getPosition(partialTick).add(0.0, from.getBbHeight() * 0.65, 0.0);
            }
            Entity to = toId < 0 ? null : level.getEntity(toId);
            Vec3 b = to == null ? fallback : to.getPosition(partialTick).add(0.0, to.getBbHeight() * 0.6, 0.0);
            Vec3 span = b.subtract(a);
            double length = span.length();
            if (length < 0.05) {
                return;
            }
            float now = age + partialTick;
            float t = progress(partialTick);
            float swell = 1F + 0.6F * Math.max(0F, 1F - now / 3F);
            float thin = t < 0.45F ? 1F : 1F - easeOut((t - 0.45F) / 0.55F);
            float w = width * swell * thin;
            float k = Math.min(1F, now / 1.5F) * (0.4F + 0.6F * thin);
            if (w <= 0.01F || k <= 0.01F) {
                return;
            }
            int steps = Math.max(6, (int) (length * 3));
            Vec3 previous = a;
            float kPrevious = k * pulse(0F, length, now);
            for (int step = 1; step <= steps; step++) {
                float s = step / (float) steps;
                Vec3 next = a.add(span.scale(s));
                if (wavy && step < steps) {
                    double sway = Mth.sin(s * Mth.PI) * 0.25 * Mth.sin(now * 0.5F + s * 7F);
                    next = next.add(draw.up().scale(sway));
                }
                float kNext = k * pulse(s, length, now);
                draw.ribbon(previous, next, w * 2.8F, w * 2.8F, tone(colour, kPrevious * 0.3F), tone(colour, kNext * 0.3F));
                draw.ribbon(previous, next, w, w, tone(colour, kPrevious), tone(colour, kNext));
                draw.ribbon(previous, next, w * 0.35F, w * 0.35F, tone(WHITE, kPrevious), tone(WHITE, kNext));
                previous = next;
                kPrevious = kNext;
            }
            draw.orb(a, w * 3.5F, tone(colour, k * 0.7F));
            draw.orb(b, w * 4.5F, tone(colour, k * 0.8F));
            draw.orb(b, w * 1.8F, tone(WHITE, k));
        }

        private static float pulse(float s, double length, float now) {
            return 0.8F + 0.2F * Mth.sin(s * (float) length * 1.6F - now * 0.9F);
        }
    }

    private static final class Fissures extends Shape {
        private final Vec3 at;
        private final float radius;
        private final List<Vec3[]> paths = new ArrayList<>();
        private final List<Float> flickers = new ArrayList<>();

        private Fissures(Vec3 at, float radius, int life) {
            super(VIOLET, life);
            this.at = at.add(0, 0.05, 0);
            this.radius = radius;
            RandomSource random = RandomSource.create((long) (at.x * 31 + at.z * 17));
            int count = 8 + (int) (radius / 3);
            for (int i = 0; i < count; i++) {
                double angle = i * Math.PI * 2 / count + random.nextDouble() * 0.5;
                add(this.at, angle, radius * (0.7 + random.nextDouble() * 0.3), random);
                if (random.nextBoolean()) {
                    double fork = 0.35 + random.nextDouble() * 0.3;
                    Vec3 start = this.at.add(Math.cos(angle) * radius * fork, 0, Math.sin(angle) * radius * fork);
                    add(start, angle + (random.nextBoolean() ? 0.6 : -0.6), radius * 0.35, random);
                }
            }
        }

        private void add(Vec3 from, double angle, double reach, RandomSource random) {
            int steps = Math.max(3, (int) (reach / 1.1));
            Vec3[] path = new Vec3[steps + 1];
            path[0] = from;
            double heading = angle;
            for (int s = 1; s <= steps; s++) {
                heading += (random.nextDouble() - 0.5) * 0.7;
                double length = reach / steps;
                path[s] = path[s - 1].add(Math.cos(heading) * length, 0, Math.sin(heading) * length);
            }
            paths.add(path);
            flickers.add(random.nextFloat() * 6F);
        }

        @Override
        protected void draw(Draw draw, ClientLevel level, float partialTick) {
            float now = age + partialTick;
            float reach = easeOut(now / Math.max(1F, life * 0.45F));
            float out = Mth.clamp((life - now) / 10F, 0F, 1F);
            for (int p = 0; p < paths.size(); p++) {
                Vec3[] path = paths.get(p);
                float k = out * (0.75F + 0.25F * Mth.sin(now * 0.8F + flickers.get(p)));
                int shown = (int) Math.ceil(reach * (path.length - 1));
                for (int s = 0; s < shown; s++) {
                    float f0 = s / (float) (path.length - 1);
                    float f1 = (s + 1) / (float) (path.length - 1);
                    float cut = Math.min(1F, reach * (path.length - 1) - s);
                    Vec3 a = path[s];
                    Vec3 b = a.lerp(path[s + 1], cut);
                    float w0 = 0.42F * (1F - f0 * 0.75F);
                    float w1 = 0.42F * (1F - f1 * 0.75F);
                    draw.lying(a, b, w0 * 3F, w1 * 3F, tone(colour, k * 0.35F), tone(colour, k * 0.35F));
                    draw.lying(a, b, w0, w1, tone(VIOLETS[0], k), tone(VIOLETS[0], k));
                    draw.lying(a, b, w0 * 0.35F, w1 * 0.35F, tone(WHITE, k), tone(WHITE, k));
                }
            }
            draw.pool(at, Math.min(radius, 8F), tone(VIOLET, out * 0.5F));
        }
    }

    private static final class Flare extends Shape {
        private final Vec3 at;
        private final float size;
        private final boolean star;

        private Flare(Vec3 at, float size, int colour, int life, boolean star) {
            super(colour, life);
            this.at = at;
            this.size = size;
            this.star = star;
        }

        @Override
        protected void draw(Draw draw, ClientLevel level, float partialTick) {
            float t = progress(partialTick);
            float fade = (1F - t) * (1F - t);
            if (star) {
                float reach = size * (0.6F + 0.8F * easeOut(t)) * (1F - t * 0.4F);
                Vec3 l = draw.left().scale(reach);
                Vec3 u = draw.up().scale(reach);
                float thin = size * 0.12F * (1F - t);
                draw.ribbon(at.subtract(l), at, 0F, thin, 0xFF000000, tone(WHITE, fade));
                draw.ribbon(at, at.add(l), thin, 0F, tone(WHITE, fade), 0xFF000000);
                draw.ribbon(at.subtract(u), at, 0F, thin, 0xFF000000, tone(WHITE, fade));
                draw.ribbon(at, at.add(u), thin, 0F, tone(WHITE, fade), 0xFF000000);
                draw.orb(at, size * 0.9F, tone(colour, fade));
                return;
            }
            draw.orb(at, size * (0.6F + 0.7F * easeOut(t)), tone(colour, fade));
            draw.orb(at, size * 0.35F * (1F - t), tone(WHITE, fade));
        }
    }

    private static final class Spark {
        private Vec3 previous;
        private Vec3 at;
        private Vec3 velocity;
        private final int colour;
        private final float size;
        private final int life;
        private final double gravity;
        private final boolean streak;
        private int age;

        private Spark(Vec3 at, Vec3 velocity, int colour, float size, int life, double gravity, boolean streak) {
            this.previous = at;
            this.at = at;
            this.velocity = velocity;
            this.colour = colour;
            this.size = size;
            this.life = Math.max(2, life);
            this.gravity = gravity;
            this.streak = streak;
        }

        private boolean tick() {
            previous = at;
            at = at.add(velocity);
            velocity = velocity.scale(0.9).add(0, -gravity, 0);
            return ++age >= life;
        }

        private void draw(Draw draw, float partialTick) {
            float t = Mth.clamp((age + partialTick) / life, 0F, 1F);
            float k = Math.min(1F, (age + partialTick) / 1.5F) * (1F - t);
            if (k <= 0.01F) {
                return;
            }
            Vec3 here = previous.lerp(at, partialTick);
            float s = size * (1F - 0.5F * t);
            if (streak && velocity.lengthSqr() > 1.0E-5) {
                Vec3 tail = here.subtract(velocity.scale(3.0));
                draw.ribbon(tail, here, 0F, s * 1.2F, 0xFF000000, tone(colour, k));
                draw.orb(here, s * 2.5F, tone(colour, k * 0.6F));
                return;
            }
            draw.orb(here, s * 3F, tone(colour, k));
        }
    }
}
