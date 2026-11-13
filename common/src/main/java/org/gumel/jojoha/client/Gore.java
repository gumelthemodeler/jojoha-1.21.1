package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.body.Limbs;
import org.gumel.jojoha.body.SeveredLimb;
import org.gumel.jojoha.combat.fx.ImpactTier;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.network.packet.ImpactCuePacket;
import org.gumel.jojoha.stand.StandEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Gore {
    private static final ResourceLocation PIXEL =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/pillar_pixel.png");

    private static final float PX = 1F / 16F;
    private static final int MAX_DROPS = 640;
    private static final int MAX_SPLATS = 120;
    private static final int DROP_TICKS = 80;
    private static final int MIST_TICKS = 14;
    private static final double MIST_GRAVITY = 0.012;
    private static final double MIST_DRAG = 0.88;
    private static final double STREAK_GAP = 0.3;
    private static final double LAUNCH_LEAN = 0.75;
    private static final int GEYSER_AGAIN_TICKS = 16;
    private static final int JET_TICKS = 8;
    private static final double LIFT_OFF = 0.06;
    private static final double QUAKE_REACH = 1.0;
    private static final double QUAKE_HEIGHT = 4.0;
    private static final int QUAKE_LINGER_TICKS = 4;

    private enum Kind {
        DROP, STREAK, GLOB, MIST
    }

    private static final int[][] GLOBS = {
            {0, 0, 1, 0, -1, 0, 0, 1, 0, -1},
            {0, 0, 1, 0, 0, 1},
            {0, 0, 1, 0, 0, 1, 1, 1, 2, 0},
            {0, 0, 1, 0, 1, 1, 2, 1},
            {0, 0, 0, 1, 0, -1, 1, -1}
    };

    private record Quake(Vec3 centre, double radius, long born, int ticks, long until) {
    }

    private record Jet(int entityId, Blood blood, long until) {
    }

    private record Stump(int entityId, int limb, Blood blood, long born) {
    }

    private static final class Stain {
        final long born;
        boolean pooled;

        Stain(long born) {
            this.born = born;
        }
    }

    private static final int STUMP_TICKS = 120;
    private static final int STUMP_BEAT = 18;
    private static final int STUMP_DRIP_EVERY = 4;
    private static final int LIMB_DRIP_TICKS = 160;
    private static final int LIMB_DRIP_EVERY = 6;
    private static final double SHOULDER_HEIGHT = 0.72, HIP_HEIGHT = 0.375;
    private static final double SHOULDER_SIDE = 0.34, HIP_SIDE = 0.12;
    private static final int SPLAT_TICKS = 600;
    private static final int SPLAT_FADE_TICKS = 40;
    private static final double GRAVITY = 0.045;
    private static final double DRAG = 0.98;
    private static final double SURFACE_LIFT = 0.004;
    private static final int WATCH_TICKS = 12;
    private static final double RANGE = 48.0;
    private static final float BIG_DROP_CHANCE = 0.4F;
    private static final int CUE_COVERS_TICKS = 4;
    private static final float WOUNDED = 0.35F;
    private static final int WOUNDED_DRIP_EVERY = 12;
    private static final float DARK_CHANCE = 0.4F;

    private static final int PUDDLE_CELLS = 190;
    private static final int PUDDLE_SPREAD_TICKS = 14;
    private static final int BUBBLE_EVERY = 5;
    private static final float FIGURE_PX = PX * 0.9375F;
    private static final float FIGURE_FROM = 0.3F;
    private static final float FIGURE_SPAN = 0.6F;
    private static final float FLIGHT = 0.07F;
    private static final int RESYNC_TICKS = 6;
    private static final int LOST_TICKS = 60;
    private static final int[][] FIGURE = {
            {-4, 3, 0, 11},
            {-4, 3, 12, 23},
            {-8, -5, 12, 23},
            {4, 7, 12, 23},
            {-4, 3, 24, 31}
    };

    private enum Blood {
        RED(0x6E0B0B, 0x4F0707, true),
        BONE(0xD8D4C4, 0xA9A595, false),
        SLIME(0x6FBF5A, 0x4E9440, true),
        ENDER(0x7A2FB0, 0x521C7A, true);

        private final int bright;
        private final int dark;
        private final boolean stains;

        Blood(int bright, int dark, boolean stains) {
            this.bright = bright;
            this.dark = dark;
            this.stains = stains;
        }

        int shade(boolean darker) {
            return darker ? dark : bright;
        }
    }

    private static final class Drop {
        Vec3 at;
        Vec3 last;
        Vec3 velocity;
        final int size;
        final Blood blood;
        final boolean dark;
        final Kind kind;
        final int shape;
        int age;

        Drop(Vec3 at, Vec3 velocity, int size, Blood blood, boolean dark) {
            this(at, velocity, size, blood, dark, Kind.DROP, 0);
        }

        Drop(Vec3 at, Vec3 velocity, int size, Blood blood, boolean dark, Kind kind, int shape) {
            this.at = at;
            this.last = at;
            this.velocity = velocity;
            this.size = size;
            this.blood = blood;
            this.dark = dark;
            this.kind = kind;
            this.shape = shape;
        }
    }

    private record Splat(BlockPos support, Direction face, double plane, int[] cells, Blood blood, long born) {
    }

    private static final List<Drop> DROPS = new ArrayList<>();
    private static final ArrayDeque<Splat> SPLATS = new ArrayDeque<>();
    private static final Map<Integer, Long> WATCHED = new HashMap<>();
    private static final Map<Integer, Long> SPRAYED = new HashMap<>();
    private static final Set<Integer> HURTING = new HashSet<>();
    private static final Map<Integer, Long> GEYSERED = new HashMap<>();
    private static final List<Quake> QUAKES = new ArrayList<>();
    private static final List<Jet> JETS = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Map<Integer, Reform> REFORMS = new HashMap<>();
    private static final List<Stump> STUMPS = new ArrayList<>();
    private static final Map<Integer, Stain> LIMBS = new HashMap<>();
    private static CameraType savedCamera;
    private static long clock;

    private Gore() {
    }

    public static void clear() {
        DROPS.clear();
        SPLATS.clear();
        WATCHED.clear();
        SPRAYED.clear();
        HURTING.clear();
        GEYSERED.clear();
        QUAKES.clear();
        JETS.clear();
        REFORMS.clear();
        STUMPS.clear();
        LIMBS.clear();
    }

    public static boolean reforming(Entity entity) {
        return REFORMS.containsKey(entity.getId());
    }

    public static void reform(int entityId, int left, int total) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Reform known = REFORMS.get(entityId);
        if (left <= 0 || total <= 0) {
            if (known != null) {
                Entity entity = level.getEntity(entityId);
                if (entity != null) {
                    known.finish(level, entity);
                }
                REFORMS.remove(entityId);
            }
            return;
        }
        if (known != null) {
            if (Math.abs(known.left - left) > RESYNC_TICKS) {
                known.left = left;
            }
            return;
        }
        Entity entity = level.getEntity(entityId);
        if (entity != null) {
            REFORMS.put(entityId, new Reform(entity, left, total));
        }
    }

    private static final class Reform {
        final int total;
        final int[] puddle;
        final int[] order;
        final int[] figure;
        final float[] arrive;
        final int[] source;
        final double plane;
        int left;
        int age;

        Reform(Entity entity, int left, int total) {
            this.total = total;
            this.left = left;
            this.plane = entity.getY() + SURFACE_LIFT;
            RandomSource random = RandomSource.create(entity.getUUID().getLeastSignificantBits());
            int cu = Mth.floor(entity.getX() * 16.0);
            int cv = Mth.floor(entity.getZ() * 16.0);
            List<int[]> cells = new ArrayList<>();
            Set<Long> taken = new HashSet<>();
            cells.add(new int[] {cu, cv, 0});
            taken.add(((long) cu << 32) ^ (cv & 0xFFFFFFFFL));
            for (int tries = 0; tries < PUDDLE_CELLS * 6 && cells.size() < PUDDLE_CELLS; tries++) {
                int[] from = cells.get(random.nextInt(cells.size()));
                Direction step = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                int nu = from[0] + step.getStepX();
                int nv = from[1] + step.getStepZ();
                if (Math.hypot(nu - cu, nv - cv) > 11 || !taken.add(((long) nu << 32) ^ (nv & 0xFFFFFFFFL))) {
                    continue;
                }
                cells.add(new int[] {nu, nv, random.nextFloat() < DARK_CHANCE ? 1 : 0});
            }
            cells.sort(Comparator.comparingDouble(c -> Math.hypot(c[0] - cu, c[1] - cv)));
            this.puddle = new int[cells.size() * 3];
            for (int i = 0; i < cells.size(); i++) {
                System.arraycopy(cells.get(i), 0, puddle, i * 3, 3);
            }
            this.order = new int[cells.size()];
            for (int i = 0; i < order.length; i++) {
                order[i] = i;
            }
            for (int i = order.length - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int swap = order[i];
                order[i] = order[j];
                order[j] = swap;
            }

            List<int[]> shape = new ArrayList<>();
            for (int[] part : FIGURE) {
                for (int u = part[0]; u <= part[1]; u++) {
                    for (int v = part[2]; v <= part[3]; v++) {
                        shape.add(new int[] {u, v, random.nextFloat() < DARK_CHANCE ? 1 : 0});
                    }
                }
            }
            this.figure = new int[shape.size() * 3];
            this.arrive = new float[shape.size()];
            this.source = new int[shape.size()];
            for (int i = 0; i < shape.size(); i++) {
                int[] cell = shape.get(i);
                System.arraycopy(cell, 0, figure, i * 3, 3);
                arrive[i] = FIGURE_FROM + cell[1] / 32F * FIGURE_SPAN + random.nextFloat() * 0.04F;
                source[i] = random.nextInt(cells.size());
            }
        }

        float progress(float partialTick) {
            return Mth.clamp(1F - (left - partialTick) / total, 0F, 1F);
        }

        int shown(float progress) {
            float drain = Mth.clamp((progress - FIGURE_FROM) / FIGURE_SPAN, 0F, 1F);
            int spread = Math.min(order.length, (int) ((age + 1) / (float) PUDDLE_SPREAD_TICKS * order.length));
            return Math.min(spread, Math.round(order.length * (1F - drain * 0.85F)));
        }

        Vec3 puddleCell(int index) {
            return new Vec3((puddle[index * 3] + 0.5) * PX, plane, (puddle[index * 3 + 1] + 0.5) * PX);
        }

        void tick() {
            age++;
            left--;
            float progress = progress(0F);
            if (age % BUBBLE_EVERY == 0 && progress < 0.95F) {
                Vec3 at = puddleCell(RANDOM.nextInt(order.length)).add(0, PX, 0);
                add(new Drop(at, new Vec3((RANDOM.nextDouble() - 0.5) * 0.02, 0.08 + RANDOM.nextDouble() * 0.08,
                        (RANDOM.nextDouble() - 0.5) * 0.02), 1, Blood.RED, RANDOM.nextFloat() < DARK_CHANCE));
            }
        }

        void finish(ClientLevel level, Entity entity) {
            Vec3 base = entity.position();
            for (int i = 0; i < arrive.length; i += 7) {
                Vec3 at = base.add(0, (figure[i * 3 + 1] + 0.5) * FIGURE_PX, 0);
                double angle = RANDOM.nextDouble() * Mth.TWO_PI;
                add(shaped(at, new Vec3(Math.cos(angle) * 0.12, 0.1 + RANDOM.nextDouble() * 0.15,
                        Math.sin(angle) * 0.12), Blood.RED, i % 3 == 0 ? Kind.GLOB : Kind.DROP));
            }
            int kept = shown(1F);
            BlockPos support = BlockPos.containing(base.x, plane - 0.1, base.z);
            if (kept > 0 && !level.getBlockState(support).isAir()) {
                int[] cells = new int[kept * 3];
                int at = 0;
                for (int i = 0; i < order.length && at < kept; i++) {
                    if (order[i] < kept) {
                        System.arraycopy(puddle, i * 3, cells, at * 3, 3);
                        at++;
                    }
                }
                SPLATS.addLast(new Splat(support, Direction.UP, plane, Arrays.copyOf(cells, at * 3),
                        Blood.RED, clock));
                while (SPLATS.size() > MAX_SPLATS) {
                    SPLATS.removeFirst();
                }
            }
        }

        void render(VertexConsumer out, Matrix4f pose, Vec3 eye, Camera camera, float partialTick, ClientLevel level,
                    Entity entity) {
            float progress = progress(partialTick);
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(entity.position().add(0, 0.2, 0)));
            Vec3 across = new Vec3(PX * 0.5, 0, 0);
            Vec3 deep = new Vec3(0, 0, PX * 0.5);
            int shown = shown(progress);
            for (int i = 0; i < order.length; i++) {
                if (order[i] >= shown) {
                    continue;
                }
                quad(out, pose, eye, puddleCell(i), across, deep,
                        0xFF000000 | Blood.RED.shade(puddle[i * 3 + 2] == 1), light);
            }

            Vector3f leftF = camera.getLeftVector();
            Vec3 side = new Vec3(-leftF.x(), 0, -leftF.z());
            side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
            Vec3 base = entity.getPosition(partialTick);
            Vec3 cellRight = side.scale(FIGURE_PX * 0.5);
            Vec3 cellUp = new Vec3(0, FIGURE_PX * 0.5, 0);
            for (int i = 0; i < arrive.length; i++) {
                float t = (progress - arrive[i] + FLIGHT) / FLIGHT;
                if (t <= 0F) {
                    continue;
                }
                Vec3 target = base.add(side.scale((figure[i * 3] + 0.5) * FIGURE_PX))
                        .add(0, (figure[i * 3 + 1] + 0.5) * FIGURE_PX, 0);
                int colour = 0xFF000000 | Blood.RED.shade(figure[i * 3 + 2] == 1);
                if (t >= 1F) {
                    quad(out, pose, eye, target, cellRight, cellUp, colour, light);
                    continue;
                }
                float eased = t * t * (3 - 2 * t);
                Vec3 from = puddleCell(source[i]);
                Vec3 at = from.lerp(target, eased).add(0, Mth.sin(t * Mth.PI) * 0.2, 0);
                quad(out, pose, eye, at, right(camera).scale(PX * 0.5), up(camera).scale(PX * 0.5), colour, light);
            }
        }
    }

    private static Vec3 right(Camera camera) {
        Vector3f left = camera.getLeftVector();
        return new Vec3(-left.x(), -left.y(), -left.z());
    }

    private static Vec3 up(Camera camera) {
        Vector3f up = camera.getUpVector();
        return new Vec3(up.x(), up.y(), up.z());
    }

    private static void reformTick(ClientLevel level) {
        for (Iterator<Map.Entry<Integer, Reform>> it = REFORMS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Reform> entry = it.next();
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null || !entity.isAlive() || entry.getValue().left < -LOST_TICKS) {
                it.remove();
                continue;
            }
            entry.getValue().tick();
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean mine = minecraft.player != null && REFORMS.containsKey(minecraft.player.getId());
        if (mine && minecraft.options.getCameraType().isFirstPerson()) {
            if (savedCamera == null) {
                savedCamera = minecraft.options.getCameraType();
            }
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else if (!mine && savedCamera != null) {
            minecraft.options.setCameraType(savedCamera);
            savedCamera = null;
        }
    }

    private static boolean full() {
        return JojohaClientConfig.GORE.get() == GoreLevel.FULL;
    }

    private static Blood bloodOf(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof StandEntity || entity instanceof ArmorStand
                || entity instanceof AbstractGolem || entity instanceof Blaze || entity instanceof Vex
                || entity instanceof Allay || entity instanceof MagmaCube) {
            return null;
        }
        if (entity.getType().is(EntityTypeTags.SKELETONS) || entity instanceof WitherBoss) {
            return Blood.BONE;
        }
        if (entity instanceof Slime) {
            return Blood.SLIME;
        }
        if (entity instanceof EnderMan || entity instanceof Endermite || entity instanceof EnderDragon) {
            return Blood.ENDER;
        }
        return Blood.RED;
    }

    public static void landed(ImpactCuePacket cue, ImpactTier tier, Vec3 at, Vec3 dir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || JojohaClientConfig.GORE.get() == GoreLevel.OFF || cue.targetId() < 0) {
            return;
        }
        Entity target = level.getEntity(cue.targetId());
        Blood blood = bloodOf(target);
        if (blood == null) {
            return;
        }
        boolean cuts = cue.colour() == Impacts.BLOOD;
        boolean full = full();
        int count = switch (tier) {
            case TAP -> full ? 2 : cuts ? 1 : 0;
            case SOLID -> full ? 5 : 2;
            case HEAVY -> full ? 10 : 5;
            case FINISHER -> full ? 16 : 9;
            default -> 0;
        };
        if (count == 0) {
            return;
        }
        spray(at, dir, cuts ? count + 3 : count, blood, 0.6);
        SPRAYED.put(target.getId(), clock);
        if (target instanceof LivingEntity living
                && ((tier != ImpactTier.TAP && launches(dir)) || shaken(living))) {
            geyser(living, blood);
        }
        if (tier == ImpactTier.HEAVY || tier == ImpactTier.FINISHER) {
            WATCHED.put(target.getId(), clock);
        }
    }

    public static void drip(int entityId, int amount) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || JojohaClientConfig.GORE.get() == GoreLevel.OFF) {
            return;
        }
        Entity entity = level.getEntity(entityId);
        Blood blood = bloodOf(entity);
        if (blood == null) {
            return;
        }
        for (int i = 0; i < Mth.clamp(amount, 1, 4); i++) {
            Vec3 at = entity.position().add((RANDOM.nextDouble() - 0.5) * entity.getBbWidth() * 0.8,
                    entity.getBbHeight() * (0.35 + RANDOM.nextDouble() * 0.4),
                    (RANDOM.nextDouble() - 0.5) * entity.getBbWidth() * 0.8);
            add(new Drop(at, new Vec3((RANDOM.nextDouble() - 0.5) * 0.03, 0.0, (RANDOM.nextDouble() - 0.5) * 0.03),
                    1, blood, RANDOM.nextFloat() < DARK_CHANCE));
        }
    }

    public static void quake(Vec3 centre, double radius, int ticks) {
        if (JojohaClientConfig.GORE.get() == GoreLevel.OFF) {
            return;
        }
        QUAKES.add(new Quake(centre, radius, clock, Math.max(1, ticks), clock + ticks + QUAKE_LINGER_TICKS));
    }

    public static void spurt(int entityId, int amplifier) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || JojohaClientConfig.GORE.get() == GoreLevel.OFF) {
            return;
        }
        Entity entity = level.getEntity(entityId);
        Blood blood = bloodOf(entity);
        if (blood == null) {
            return;
        }
        Vec3 at = entity.position().add(0, entity.getBbHeight() * 0.6, 0);
        int count = (full() ? 14 : 8) + Mth.clamp(amplifier, 0, 2) * 6;
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Mth.TWO_PI;
            double pitch = (RANDOM.nextDouble() - 0.2) * 1.2;
            double speed = 0.12 + RANDOM.nextDouble() * 0.2;
            Vec3 velocity = new Vec3(Math.cos(angle) * Math.cos(pitch) * speed, Math.sin(pitch) * speed + 0.1,
                    Math.sin(angle) * Math.cos(pitch) * speed);
            add(i % 5 == 3 ? shaped(at, velocity, blood, Kind.GLOB) : gout(at, velocity, blood));
        }
    }

    public static void rupture(int entityId, int amount) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || JojohaClientConfig.GORE.get() == GoreLevel.OFF) {
            return;
        }
        Entity entity = level.getEntity(entityId);
        Blood blood = bloodOf(entity);
        if (blood == null) {
            return;
        }
        Vec3 at = entity.position().add(0, entity.getBbHeight() * 0.6, 0);
        int count = (full() ? 44 : 22) + Mth.clamp(amount, 0, 8) * 2;
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Mth.TWO_PI;
            double pitch = (RANDOM.nextDouble() - 0.35) * 1.4;
            double speed = 0.25 + RANDOM.nextDouble() * 0.4;
            Vec3 velocity = new Vec3(Math.cos(angle) * Math.cos(pitch) * speed, Math.sin(pitch) * speed + 0.12,
                    Math.sin(angle) * Math.cos(pitch) * speed);
            add(i % 4 == 2 ? shaped(at, velocity, blood, Kind.GLOB) : gout(at, velocity, blood));
        }
        for (int i = 0; i < count / 3; i++) {
            add(shaped(at, new Vec3((RANDOM.nextDouble() - 0.5) * 0.3, (RANDOM.nextDouble() - 0.3) * 0.25,
                    (RANDOM.nextDouble() - 0.5) * 0.3), blood, Kind.MIST));
        }
        SPRAYED.put(entityId, clock);
    }

    public static void tear(Vec3 at, Vec3 dir, int blood, int bone, double speed) {
        if (Minecraft.getInstance().level == null || JojohaClientConfig.GORE.get() == GoreLevel.OFF) {
            return;
        }
        int share = full() ? 1 : 2;
        for (int i = 0; i < blood / share; i++) {
            Vec3 velocity = flung(dir, 0.55, speed);
            add(i % 3 == 0 ? gout(at, velocity, Blood.RED)
                    : i % 4 == 1 ? shaped(at, velocity, Blood.RED, Kind.GLOB) : shaped(at, velocity, Blood.RED, roll()));
        }
        for (int i = 0; i < blood / (3 * share); i++) {
            add(shaped(at, flung(dir, 0.9, speed * 0.4), Blood.RED, Kind.MIST));
        }
        for (int i = 0; i < bone / share; i++) {
            add(new Drop(at, flung(dir, 0.7, speed * 1.1), 1 + RANDOM.nextInt(2), Blood.BONE,
                    RANDOM.nextFloat() < DARK_CHANCE));
        }
    }

    public static void sever(int entityId, int limb) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || JojohaClientConfig.GORE.get() == GoreLevel.OFF) {
            return;
        }
        Entity entity = level.getEntity(entityId);
        Blood blood = bloodOf(entity);
        if (blood == null) {
            return;
        }
        Vec3 at = socket(entity, limb);
        Vec3 out = outward(entity, limb);
        int count = full() ? 40 : 20;
        for (int i = 0; i < count; i++) {
            Vec3 velocity = flung(out, 0.5, 0.3 + RANDOM.nextDouble() * 0.2);
            add(i % 3 == 0 ? gout(at, velocity, blood) : shaped(at, velocity, blood, roll()));
        }
        for (int i = 0; i < count / 4; i++) {
            add(shaped(at, flung(out, 0.9, 0.12), blood, Kind.MIST));
        }
        if (blood != Blood.BONE) {
            for (int i = 0; i < (full() ? 6 : 3); i++) {
                add(new Drop(at, flung(out, 0.7, 0.3), 1 + RANDOM.nextInt(2), Blood.BONE,
                        RANDOM.nextFloat() < DARK_CHANCE));
            }
            STUMPS.add(new Stump(entityId, limb, blood, clock));
        }
    }

    public static void limb(SeveredLimb limb) {
        if (!limb.bony() && limb.tickCount < LIMB_DRIP_TICKS && !LIMBS.containsKey(limb.getId())) {
            LIMBS.put(limb.getId(), new Stain(clock - limb.tickCount));
        }
    }

    private static Vec3 right(Entity entity) {
        float yaw = (entity instanceof LivingEntity living ? living.yBodyRot : entity.getYRot()) * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
    }

    private static Vec3 socket(Entity entity, int limb) {
        boolean arm = limb == Limbs.RIGHT_ARM || limb == Limbs.LEFT_ARM;
        double side = (limb == Limbs.RIGHT_ARM || limb == Limbs.RIGHT_LEG ? 1.0 : -1.0)
                * (arm ? SHOULDER_SIDE : HIP_SIDE) * entity.getBbWidth() / 0.6;
        return entity.position().add(right(entity).scale(side))
                .add(0, entity.getBbHeight() * (arm ? SHOULDER_HEIGHT : HIP_HEIGHT), 0);
    }

    private static Vec3 outward(Entity entity, int limb) {
        boolean arm = limb == Limbs.RIGHT_ARM || limb == Limbs.LEFT_ARM;
        double side = limb == Limbs.RIGHT_ARM || limb == Limbs.RIGHT_LEG ? 1.0 : -1.0;
        return right(entity).scale(side * (arm ? 1.0 : 0.5)).add(0, arm ? 0.3 : -0.4, 0).normalize();
    }

    private static void stumps(ClientLevel level) {
        for (Iterator<Stump> it = STUMPS.iterator(); it.hasNext(); ) {
            Stump stump = it.next();
            Entity entity = level.getEntity(stump.entityId());
            long age = clock - stump.born();
            if (entity == null || !entity.isAlive() || age > STUMP_TICKS) {
                it.remove();
                continue;
            }
            Vec3 at = socket(entity, stump.limb());
            if (age > 0 && age % STUMP_BEAT == 0) {
                float left = 1F - age / (float) STUMP_TICKS;
                int count = 2 + Math.round((full() ? 12 : 6) * left);
                Vec3 out = outward(entity, stump.limb());
                for (int i = 0; i < count; i++) {
                    Vec3 velocity = flung(out, 0.35, 0.12 + 0.18 * left + RANDOM.nextDouble() * 0.08);
                    add(i % 3 == 0 ? gout(at, velocity, stump.blood()) : shaped(at, velocity, stump.blood(), roll()));
                }
            } else if (age % STUMP_DRIP_EVERY == 0) {
                add(new Drop(at, new Vec3(0, -0.02, 0), 1, stump.blood(), RANDOM.nextFloat() < DARK_CHANCE));
            }
        }
        for (Iterator<Map.Entry<Integer, Stain>> it = LIMBS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Stain> entry = it.next();
            Stain stain = entry.getValue();
            long age = clock - stain.born;
            if (!(level.getEntity(entry.getKey()) instanceof SeveredLimb limb) || age > LIMB_DRIP_TICKS) {
                if (age > LIMB_DRIP_TICKS) {
                    it.remove();
                }
                continue;
            }
            Vec3 at = limb.position().add(0, 0.15, 0);
            if (!stain.pooled && limb.onGround() && limb.tickCount > 2) {
                stain.pooled = true;
                for (int i = 0; i < (full() ? 14 : 7); i++) {
                    add(shaped(at, new Vec3((RANDOM.nextDouble() - 0.5) * 0.16, 0.04 + RANDOM.nextDouble() * 0.06,
                            (RANDOM.nextDouble() - 0.5) * 0.16), Blood.RED, i % 3 == 0 ? Kind.GLOB : Kind.DROP));
                }
            }
            if (age % LIMB_DRIP_EVERY == 0) {
                Vec3 carried = limb.getDeltaMovement().scale(0.3);
                add(new Drop(at, carried.add(0, -0.02, 0), 1, Blood.RED, RANDOM.nextFloat() < DARK_CHANCE));
            }
        }
    }

    private static Vec3 flung(Vec3 dir, double spread, double speed) {
        Vec3 way = dir.add((RANDOM.nextDouble() - 0.5) * 2.0 * spread, (RANDOM.nextDouble() - 0.5) * 2.0 * spread,
                (RANDOM.nextDouble() - 0.5) * 2.0 * spread);
        if (way.lengthSqr() < 1.0E-6) {
            way = new Vec3(0, 1, 0);
        }
        return way.normalize().scale(speed * (0.4 + RANDOM.nextDouble() * 0.8))
                .add(0, 0.04 + RANDOM.nextDouble() * 0.12, 0);
    }

    private static void upheave(Quake quake) {
        double reach = quake.radius() * Math.min(1.0, (clock - quake.born() + 1) / (double) quake.ticks())
                + QUAKE_REACH;
        for (Iterator<Splat> it = SPLATS.iterator(); it.hasNext(); ) {
            Splat splat = it.next();
            Direction.Axis[] tangent = tangents(splat.face());
            Vec3 normal = unit(splat.face().getAxis()).scale(splat.face().getAxisDirection().getStep());
            Vec3 base = unit(splat.face().getAxis()).scale(splat.plane()).add(normal.scale(LIFT_OFF));
            int[] cells = splat.cells();
            Vec3 first = base.add(unit(tangent[0]).scale((cells[0] + 0.5) * PX))
                    .add(unit(tangent[1]).scale((cells[1] + 0.5) * PX));
            double dx = first.x - quake.centre().x;
            double dz = first.z - quake.centre().z;
            if (dx * dx + dz * dz > reach * reach || Math.abs(first.y - quake.centre().y) > QUAKE_HEIGHT) {
                continue;
            }
            double away = Math.sqrt(dx * dx + dz * dz);
            Vec3 outward = away < 1.0E-4 ? Vec3.ZERO : new Vec3(dx / away, 0, dz / away);
            for (int i = 0; i < cells.length; i += 3) {
                Vec3 at = base.add(unit(tangent[0]).scale((cells[i] + 0.5) * PX))
                        .add(unit(tangent[1]).scale((cells[i + 1] + 0.5) * PX));
                Vec3 velocity = outward.scale(0.03 + RANDOM.nextDouble() * 0.1)
                        .add((RANDOM.nextDouble() - 0.5) * 0.08, 0.55 + RANDOM.nextDouble() * 0.6,
                                (RANDOM.nextDouble() - 0.5) * 0.08)
                        .add(normal.scale(0.08));
                add(i % 9 == 0 ? gout(at, velocity, splat.blood())
                        : new Drop(at, velocity, 1 + RANDOM.nextInt(2), splat.blood(), cells[i + 2] == 1));
            }
            it.remove();
        }
        Vec3 centre = quake.centre();
        for (Drop drop : DROPS) {
            double dx = drop.at.x - centre.x;
            double dz = drop.at.z - centre.z;
            if (drop.kind != Kind.MIST && drop.velocity.y < 0.2 && dx * dx + dz * dz <= reach * reach
                    && Math.abs(drop.at.y - centre.y) <= 1.0) {
                drop.velocity = new Vec3(drop.velocity.x, 0.5 + RANDOM.nextDouble() * 0.5, drop.velocity.z);
            }
        }
    }

    private static boolean launches(Vec3 dir) {
        double length = dir.length();
        return length > 1.0E-4 && dir.y / length >= LAUNCH_LEAN;
    }

    private static boolean shaken(LivingEntity entity) {
        for (Quake quake : QUAKES) {
            double dx = entity.getX() - quake.centre().x;
            double dz = entity.getZ() - quake.centre().z;
            double reach = quake.radius() + QUAKE_REACH;
            if (dx * dx + dz * dz <= reach * reach && Math.abs(entity.getY() - quake.centre().y) <= QUAKE_HEIGHT) {
                return true;
            }
        }
        return false;
    }

    private static void geyser(LivingEntity entity, Blood blood) {
        Long before = GEYSERED.get(entity.getId());
        if (before != null && clock - before < GEYSER_AGAIN_TICKS) {
            return;
        }
        GEYSERED.put(entity.getId(), clock);
        JETS.add(new Jet(entity.getId(), blood, clock + JET_TICKS));
        spout(entity, blood, full() ? 16 : 8);
    }

    private static void spout(Entity entity, Blood blood, int count) {
        Vec3 at = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
        Vec3 carried = entity.getDeltaMovement();
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Mth.TWO_PI;
            double outward = RANDOM.nextDouble() * 0.2;
            Vec3 velocity = new Vec3(Math.cos(angle) * outward + carried.x * 0.5,
                    0.75 + RANDOM.nextDouble() * 0.65, Math.sin(angle) * outward + carried.z * 0.5);
            add(i % 5 == 4 ? shaped(at, velocity, blood, Kind.GLOB) : gout(at, velocity, blood));
        }
        for (int i = 0; i < count / 4; i++) {
            add(shaped(at, new Vec3((RANDOM.nextDouble() - 0.5) * 0.2, 0.2 + RANDOM.nextDouble() * 0.3,
                    (RANDOM.nextDouble() - 0.5) * 0.2), blood, Kind.MIST));
        }
    }

    private static Drop gout(Vec3 at, Vec3 velocity, Blood blood) {
        return new Drop(at, velocity, 2 + RANDOM.nextInt(2), blood, RANDOM.nextFloat() < DARK_CHANCE, Kind.STREAK,
                3 + RANDOM.nextInt(3));
    }

    private static Kind roll() {
        float pick = RANDOM.nextFloat();
        return pick < 0.4F ? Kind.DROP : pick < 0.7F ? Kind.STREAK : pick < 0.88F ? Kind.GLOB : Kind.MIST;
    }

    private static Drop shaped(Vec3 at, Vec3 velocity, Blood blood, Kind kind) {
        boolean dark = RANDOM.nextFloat() < DARK_CHANCE;
        return switch (kind) {
            case STREAK -> new Drop(at, velocity, RANDOM.nextBoolean() ? 2 : 1, blood, dark, kind,
                    2 + RANDOM.nextInt(3));
            case GLOB -> new Drop(at, velocity, 1, blood, dark, kind, RANDOM.nextInt(GLOBS.length));
            case MIST -> new Drop(at, velocity.scale(0.6), 1, blood, dark, kind, 0);
            default -> new Drop(at, velocity, RANDOM.nextFloat() < BIG_DROP_CHANCE ? 2 : 1, blood, dark);
        };
    }

    private static void spray(Vec3 at, Vec3 dir, int count, Blood blood, double spread) {
        for (int i = 0; i < count; i++) {
            Vec3 way = dir.add((RANDOM.nextDouble() - 0.5) * 2.0 * spread, (RANDOM.nextDouble() - 0.5) * 2.0 * spread,
                    (RANDOM.nextDouble() - 0.5) * 2.0 * spread);
            if (way.lengthSqr() < 1.0E-6) {
                way = new Vec3(0, 1, 0);
            }
            Vec3 velocity = way.normalize().scale(0.12 + RANDOM.nextDouble() * 0.22)
                    .add(0, 0.08 + RANDOM.nextDouble() * 0.14, 0);
            add(shaped(at, velocity, blood, roll()));
        }
    }

    private static void burst(LivingEntity entity, Blood blood) {
        Vec3 at = entity.position().add(0, entity.getBbHeight() * 0.55, 0);
        int count = full() ? 22 : 12;
        for (int i = 0; i < count; i++) {
            double angle = Mth.TWO_PI * i / count + RANDOM.nextDouble() * 0.6;
            double speed = 0.1 + RANDOM.nextDouble() * 0.2;
            Vec3 velocity = new Vec3(Math.cos(angle) * speed, 0.18 + RANDOM.nextDouble() * 0.22, Math.sin(angle) * speed);
            add(shaped(at, velocity, blood, i % 3 == 0 ? Kind.GLOB : roll()));
        }
    }

    private static void add(Drop drop) {
        if (DROPS.size() >= MAX_DROPS) {
            DROPS.remove(0);
        }
        DROPS.add(drop);
    }

    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        clock++;
        reformTick(level);

        for (Iterator<Map.Entry<Integer, Long>> it = WATCHED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Long> watched = it.next();
            Entity entity = level.getEntity(watched.getKey());
            if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
                Blood blood = bloodOf(living);
                if (blood != null) {
                    burst(living, blood);
                }
                it.remove();
            } else if (entity == null || clock - watched.getValue() > WATCH_TICKS) {
                it.remove();
            }
        }

        for (Quake quake : QUAKES) {
            upheave(quake);
        }
        QUAKES.removeIf(quake -> clock > quake.until());
        for (Iterator<Jet> it = JETS.iterator(); it.hasNext(); ) {
            Jet jet = it.next();
            Entity entity = level.getEntity(jet.entityId());
            if (entity == null || clock > jet.until()) {
                it.remove();
                continue;
            }
            spout(entity, jet.blood(), full() ? 6 : 3);
        }
        GEYSERED.values().removeIf(at -> clock - at > GEYSER_AGAIN_TICKS);
        wounds(level);
        stumps(level);

        for (Iterator<Drop> it = DROPS.iterator(); it.hasNext(); ) {
            Drop drop = it.next();
            drop.last = drop.at;
            boolean mist = drop.kind == Kind.MIST;
            drop.velocity = drop.velocity.scale(mist ? MIST_DRAG : DRAG).add(0, mist ? -MIST_GRAVITY : -GRAVITY, 0);
            Vec3 next = drop.at.add(drop.velocity);
            BlockHitResult hit = level.clip(new ClipContext(drop.at, next, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.ANY, CollisionContext.empty()));
            if (hit.getType() == HitResult.Type.BLOCK) {
                if (!mist && drop.blood.stains && full() && level.getFluidState(hit.getBlockPos()).isEmpty()) {
                    stain(hit, drop);
                }
                it.remove();
                continue;
            }
            drop.at = next;
            if (++drop.age > (mist ? MIST_TICKS : DROP_TICKS)) {
                it.remove();
            }
        }

        SPLATS.removeIf(splat -> clock - splat.born() > SPLAT_TICKS
                || level.getBlockState(splat.support()).isAir()
                || level.getBlockState(splat.support().relative(splat.face())).isSolidRender(level,
                splat.support().relative(splat.face())));
    }

    private static void wounds(ClientLevel level) {
        if (JojohaClientConfig.GORE.get() == GoreLevel.OFF) {
            return;
        }
        Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        SPRAYED.values().removeIf(at -> clock - at > CUE_COVERS_TICKS);
        Set<Integer> hurting = new HashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living.distanceToSqr(eye) > RANGE * RANGE) {
                continue;
            }
            Blood blood = bloodOf(living);
            if (blood == null) {
                continue;
            }
            if (living.hurtTime > 0) {
                hurting.add(living.getId());
                if (!HURTING.contains(living.getId()) && !SPRAYED.containsKey(living.getId())) {
                    Vec3 chest = living.position().add(0, living.getBbHeight() * 0.6, 0);
                    spray(chest, new Vec3(0, 0.4, 0), full() ? 4 : 2, blood, 0.9);
                    if (shaken(living)) {
                        geyser(living, blood);
                    }
                    if (living.isDeadOrDying()) {
                        burst(living, blood);
                    }
                }
            }
            if (full() && living.isAlive() && living.getHealth() <= living.getMaxHealth() * WOUNDED
                    && (clock + living.getId()) % WOUNDED_DRIP_EVERY == 0) {
                drip(living.getId(), 1);
            }
        }
        HURTING.clear();
        HURTING.addAll(hurting);
    }

    private static double along(Vec3 at, Direction.Axis axis) {
        return axis == Direction.Axis.X ? at.x : axis == Direction.Axis.Y ? at.y : at.z;
    }

    private static Direction.Axis[] tangents(Direction face) {
        return switch (face.getAxis()) {
            case X -> new Direction.Axis[] {Direction.Axis.Z, Direction.Axis.Y};
            case Y -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Y};
        };
    }

    private static void stain(BlockHitResult hit, Drop drop) {
        Direction face = hit.getDirection();
        Direction.Axis[] tangent = tangents(face);
        Vec3 where = hit.getLocation();
        BlockPos support = hit.getBlockPos();
        int u = Mth.floor(along(where, tangent[0]) * 16.0);
        int v = Mth.floor(along(where, tangent[1]) * 16.0);
        int blockU = support.get(tangent[0]);
        int blockV = support.get(tangent[1]);

        int wanted = drop.kind == Kind.GLOB ? 7 + RANDOM.nextInt(5)
                : drop.size == 2 ? 5 + RANDOM.nextInt(5) : 3 + RANDOM.nextInt(3);
        List<int[]> cells = new ArrayList<>();
        cells.add(new int[] {u, v, drop.dark ? 1 : 0});
        if (face.getAxis() != Direction.Axis.Y && (drop.kind == Kind.STREAK || RANDOM.nextFloat() < 0.35F)) {
            int run = 2 + RANDOM.nextInt(4);
            for (int i = 1; i <= run && Math.floorDiv(v - i, 16) == blockV; i++) {
                cells.add(new int[] {u, v - i, i == run ? 1 : 0});
            }
            wanted += run;
        }
        for (int tries = 0; tries < wanted * 4 && cells.size() < wanted; tries++) {
            int[] from = cells.get(RANDOM.nextInt(cells.size()));
            Direction step = Direction.Plane.HORIZONTAL.getRandomDirection(RANDOM);
            int nu = from[0] + step.getStepX();
            int nv = from[1] + step.getStepZ();
            if (Math.floorDiv(nu, 16) != blockU || Math.floorDiv(nv, 16) != blockV) {
                continue;
            }
            boolean taken = false;
            for (int[] cell : cells) {
                if (cell[0] == nu && cell[1] == nv) {
                    taken = true;
                    break;
                }
            }
            if (!taken) {
                cells.add(new int[] {nu, nv, RANDOM.nextFloat() < DARK_CHANCE ? 1 : 0});
            }
        }

        int[] packed = new int[cells.size() * 3];
        for (int i = 0; i < cells.size(); i++) {
            System.arraycopy(cells.get(i), 0, packed, i * 3, 3);
        }
        double plane = along(where, face.getAxis()) + face.getAxisDirection().getStep() * SURFACE_LIFT;
        SPLATS.addLast(new Splat(support, face, plane, packed, drop.blood, clock));
        while (SPLATS.size() > MAX_SPLATS) {
            SPLATS.removeFirst();
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || (DROPS.isEmpty() && SPLATS.isEmpty() && REFORMS.isEmpty())) {
            return;
        }
        Vec3 eye = camera.getPosition();
        Matrix4f pose = poseStack.last().pose();
        RenderType type = RenderType.text(PIXEL);
        VertexConsumer out = buffers.getBuffer(type);
        Vector3f leftF = camera.getLeftVector();
        Vector3f upF = camera.getUpVector();
        Vec3 right = new Vec3(-leftF.x(), -leftF.y(), -leftF.z());
        Vec3 up = new Vec3(upF.x(), upF.y(), upF.z());

        for (Drop drop : DROPS) {
            Vec3 at = drop.last.lerp(drop.at, partialTick);
            if (at.distanceToSqr(eye) > RANGE * RANGE) {
                continue;
            }
            float half = drop.size * PX * 0.5F;
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(at));
            Vec3 pixelRight = right.scale(PX * 0.5);
            Vec3 pixelUp = up.scale(PX * 0.5);
            switch (drop.kind) {
                case STREAK -> {
                    quad(out, pose, eye, at, right.scale(half), up.scale(half),
                            0xFF000000 | drop.blood.shade(drop.dark), light);
                    Vec3 back = drop.velocity.scale(-STREAK_GAP);
                    for (int i = 1; i <= drop.shape; i++) {
                        quad(out, pose, eye, at.add(back.scale(i)), pixelRight, pixelUp,
                                0xFF000000 | drop.blood.shade(drop.dark || i == drop.shape), light);
                    }
                }
                case GLOB -> {
                    int[] cells = GLOBS[drop.shape];
                    for (int i = 0; i < cells.length; i += 2) {
                        Vec3 cell = at.add(right.scale(cells[i] * PX)).add(up.scale(cells[i + 1] * PX));
                        quad(out, pose, eye, cell, pixelRight, pixelUp,
                                0xFF000000 | drop.blood.shade(drop.dark ^ (i % 4 == 2)), light);
                    }
                }
                case MIST -> quad(out, pose, eye, at, pixelRight, pixelUp,
                        (drop.age < MIST_TICKS / 2 ? 0xB0000000 : 0x60000000) | drop.blood.shade(drop.dark), light);
                default -> quad(out, pose, eye, at, right.scale(half), up.scale(half),
                        0xFF000000 | drop.blood.shade(drop.dark), light);
            }
        }

        for (Splat splat : SPLATS) {
            if (Vec3.atCenterOf(splat.support()).distanceToSqr(eye) > RANGE * RANGE) {
                continue;
            }
            long age = clock - splat.born();
            float left = Mth.clamp((SPLAT_TICKS - age) / (float) SPLAT_FADE_TICKS, 0F, 1F);
            int alpha = Math.round(Mth.ceil(left * 4F) / 4F * 255F) << 24;
            int light = LevelRenderer.getLightColor(level, splat.support().relative(splat.face()));
            Direction.Axis[] tangent = tangents(splat.face());
            Vec3 across = unit(tangent[0]).scale(PX * 0.5);
            Vec3 down = unit(tangent[1]).scale(PX * 0.5);
            Vec3 base = unit(splat.face().getAxis()).scale(splat.plane());
            int[] cells = splat.cells();
            for (int i = 0; i < cells.length; i += 3) {
                Vec3 centre = base.add(unit(tangent[0]).scale((cells[i] + 0.5) * PX))
                        .add(unit(tangent[1]).scale((cells[i + 1] + 0.5) * PX));
                quad(out, pose, eye, centre, across, down, alpha | splat.blood().shade(cells[i + 2] == 1), light);
            }
        }

        for (Map.Entry<Integer, Reform> entry : REFORMS.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (entity != null && entity.distanceToSqr(eye) <= RANGE * RANGE) {
                entry.getValue().render(out, pose, eye, camera, partialTick, level, entity);
            }
        }

        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(type);
        }
    }

    private static Vec3 unit(Direction.Axis axis) {
        return axis == Direction.Axis.X ? new Vec3(1, 0, 0) : axis == Direction.Axis.Y ? new Vec3(0, 1, 0)
                : new Vec3(0, 0, 1);
    }

    private static void quad(VertexConsumer out, Matrix4f pose, Vec3 eye, Vec3 centre, Vec3 right, Vec3 up,
                             int argb, int light) {
        Vec3 bl = centre.subtract(right).subtract(up);
        Vec3 br = centre.add(right).subtract(up);
        Vec3 tr = centre.add(right).add(up);
        Vec3 tl = centre.subtract(right).add(up);
        vertex(out, pose, eye, bl, argb, light);
        vertex(out, pose, eye, br, argb, light);
        vertex(out, pose, eye, tr, argb, light);
        vertex(out, pose, eye, tl, argb, light);
        vertex(out, pose, eye, tl, argb, light);
        vertex(out, pose, eye, tr, argb, light);
        vertex(out, pose, eye, br, argb, light);
        vertex(out, pose, eye, bl, argb, light);
    }

    private static void vertex(VertexConsumer out, Matrix4f pose, Vec3 eye, Vec3 at, int argb, int light) {
        out.addVertex(pose, (float) (at.x - eye.x), (float) (at.y - eye.y), (float) (at.z - eye.z))
                .setColor(argb)
                .setUv(0.5F, 0.5F)
                .setLight(light);
    }
}
