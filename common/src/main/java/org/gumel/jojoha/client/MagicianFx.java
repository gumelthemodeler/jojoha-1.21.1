package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.joml.Vector3f;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.client.pillar.PixelDraw;
import org.gumel.jojoha.network.packet.MagicianFxPacket;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.magician.Magician;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MagicianFx {
    private static final double SEE_RANGE = 96.0;
    private static final int ANKH_TAG_BASE = 1 << 20;
    private static final ResourceLocation ANKH_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/magician_ankh.png");
    private static final int ANKH_FRAMES = 4;
    private static final int ANKH_FRAME_TICKS = 2;
    private static final double ANKH_WIDE = 1.2;
    private static final double ANKH_TALL = 1.5;
    private static final ResourceLocation[] WALL_FRAMES = wallFrames();
    private static final int WALL_FRAME_TICKS = 2;
    private static final double WALL_HEIGHT = 4.0;
    private static final float[] DOME_FADE = {1F, 1F, 0.9F, 0.78F, 0.66F, 0.54F, 0.42F, 0.31F, 0.21F, 0.12F, 0.05F, 0F, 0F, 0F, 0F, 0F, 0F, 0F};
    private static final double STREAM_REACH = 0.7;
    private static final double TILE = 4.0;
    private static final int FLOOR_EVERY = 20;
    private static final int RING_RISE_TICKS = 6;
    private static final int DOME_RINGS = 18;
    private static final int DOME_SEGMENTS = 36;
    private static final int DOME_CLOSE_TICKS = 1;
    private static final int DOME_STREAMS = 6;
    private static final int DOME_FIRES = 12;
    private static final double WALL_TALL = 2.4;
    private static final double DOME_WALL_TALL = 3.2;
    private static final int SENSE_EVERY = 10;
    private static final double RADAR_ARM = 1.5;
    private static final Vec3[] SIX_WAYS = {new Vec3(1, 0, 0), new Vec3(-1, 0, 0), new Vec3(0, 1, 0),
            new Vec3(0, -1, 0), new Vec3(0, 0, 1), new Vec3(0, 0, -1)};

    private static final class Ankh {
        double size;
        Vec3 at;
        Vec3 velocity;
        int left;
    }

    private static final class Zone {
        Vec3 centre;
        double radius;
        long until;
        long born;
        boolean dome;
        double[] floors;
        double[] fires;
        double[] fireFloors;
    }

    private record Bound(int entityId, float width, long until) {
    }

    private static final class Barrier {
        final List<Vec3> points = new ArrayList<>();
        long until;
    }

    private record Kindled(int standId, long until) {
    }

    private static final Map<Integer, Ankh> ANKHS = new HashMap<>();
    private static final Map<Integer, Zone> ZONES = new HashMap<>();
    private static final List<Bound> BOUND = new ArrayList<>();
    private static final List<Kindled> KINDLED = new ArrayList<>();
    private static final Map<Integer, Barrier> BARRIERS = new HashMap<>();
    private static final Set<Integer> SENSED = new HashSet<>();
    private static final RandomSource RANDOM = RandomSource.create();
    private static int radarStand = -1;
    private static boolean radarMine;
    private static double radarReach;
    private static long radarUntil;
    private static long clock;

    private MagicianFx() {
    }

    private static ResourceLocation[] wallFrames() {
        ResourceLocation[] frames = new ResourceLocation[8];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/magician_wall_" + i + ".png");
        }
        return frames;
    }

    public static float zoneHeat(Vec3 at, double margin) {
        float heat = 0F;
        for (Zone zone : ZONES.values()) {
            if (zone.centre == null) {
                continue;
            }
            double dx = at.x - zone.centre.x;
            double dz = at.z - zone.centre.z;
            double gap = Math.sqrt(dx * dx + dz * dz) - zone.radius;
            heat = Math.max(heat, (float) Mth.clamp(1.0 - gap / margin, 0.0, 1.0));
        }
        return heat;
    }

    public static void clear() {
        ANKHS.clear();
        ZONES.clear();
        BOUND.clear();
        KINDLED.clear();
        BARRIERS.clear();
        SENSED.clear();
        radarStand = -1;
        radarMine = false;
    }

    public static boolean reveals(Entity entity) {
        return !SENSED.isEmpty() && SENSED.contains(entity.getId());
    }

    public static int colour(Entity entity) {
        return reveals(entity) ? Magician.FLAME : -1;
    }

    public static void handle(MagicianFxPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }
        Vec3 at = new Vec3(packet.x(), packet.y(), packet.z());
        switch (packet.kind()) {
            case MagicianFxPacket.ANKH -> ankh(packet, at);
            case MagicianFxPacket.ANKH_END -> {
                ANKHS.remove(packet.id());
                FlameFx.drop(ANKH_TAG_BASE + packet.id());
                FlameFx.burst(at, 30, 0.3);
            }
            case MagicianFxPacket.ZONE_RING, MagicianFxPacket.ZONE_DOME -> {
                Zone zone = ZONES.get(packet.id());
                boolean fresh = zone == null;
                if (fresh) {
                    zone = new Zone();
                    zone.born = clock;
                    ZONES.put(packet.id(), zone);
                }
                zone.centre = at;
                zone.radius = packet.radius();
                zone.until = clock + packet.ticks();
                zone.dome = packet.kind() == MagicianFxPacket.ZONE_DOME;
                if (fresh) {
                    ignite(zone);
                }
            }
            case MagicianFxPacket.ZONE_END -> ZONES.remove(packet.id());
            case MagicianFxPacket.BIND -> BOUND.add(new Bound(packet.id(), packet.radius(), clock + packet.ticks()));
            case MagicianFxPacket.RADAR -> {
                radarStand = packet.id();
                radarMine = (int) packet.dx() == minecraft.player.getId();
                radarReach = packet.radius();
                radarUntil = clock + packet.ticks();
            }
            case MagicianFxPacket.HEAT -> heat(at, packet.radius());
            case MagicianFxPacket.INFERNO -> KINDLED.add(new Kindled(packet.id(), clock + packet.ticks()));
            case MagicianFxPacket.KICK -> kick(at, packet.radius());
            case MagicianFxPacket.WALL -> {
                Barrier barrier = BARRIERS.computeIfAbsent(packet.id(), id -> new Barrier());
                barrier.points.add(at);
                barrier.until = clock + packet.ticks();
                for (int i = 0; i < 4; i++) {
                    FlameFx.mote(at.add((RANDOM.nextDouble() - 0.5) * 0.8, 0.3, (RANDOM.nextDouble() - 0.5) * 0.8),
                            new Vec3(0.0, 0.2 + RANDOM.nextDouble() * 0.2, 0.0), 0.55F + RANDOM.nextFloat() * 0.25F,
                            10 + RANDOM.nextInt(6));
                }
                FlameFx.sparks(at.add(0.0, 0.5, 0.0), 8, 0.4);
            }
            case MagicianFxPacket.WALL_END -> BARRIERS.remove(packet.id());
            default -> {
            }
        }
    }

    private static void ankh(MagicianFxPacket packet, Vec3 at) {
        Ankh ankh = new Ankh();
        ankh.at = at;
        ankh.velocity = new Vec3(packet.dx(), packet.dy(), packet.dz());
        ankh.left = packet.ticks();
        ankh.size = packet.radius();
        ANKHS.put(packet.id(), ankh);
    }

    private static void heat(Vec3 at, double radius) {
        for (int i = 0; i < 48; i++) {
            double angle = RANDOM.nextDouble() * Mth.TWO_PI;
            double away = Math.sqrt(RANDOM.nextDouble()) * radius;
            Vec3 where = at.add(Math.cos(angle) * away, (RANDOM.nextDouble() - 0.6) * 1.2, Math.sin(angle) * away);
            FlameFx.mote(where, new Vec3(0.0, 0.08 + RANDOM.nextDouble() * 0.08, 0.0),
                    0.42F + RANDOM.nextFloat() * 0.2F, 12 + RANDOM.nextInt(8));
        }
        for (int i = 0; i < 24; i++) {
            double angle = RANDOM.nextDouble() * Mth.TWO_PI;
            double away = Math.sqrt(RANDOM.nextDouble()) * radius;
            FlameFx.spark(at.add(Math.cos(angle) * away, 0.0, Math.sin(angle) * away),
                    new Vec3(0.0, 0.15 + RANDOM.nextDouble() * 0.2, 0.0), 14 + RANDOM.nextInt(10));
        }
    }

    private static void kick(Vec3 at, double radius) {
        int count = 40;
        FlameFx.sparks(at.add(0.0, 0.3, 0.0), 24, 0.5);
        for (int i = 0; i < count; i++) {
            double angle = Mth.TWO_PI * i / count;
            Vec3 out = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            FlameFx.mote(at.add(out.scale(0.6)).add(0.0, 0.25, 0.0), out.scale(radius / 9.0).add(0.0, 0.02, 0.0),
                    0.42F + RANDOM.nextFloat() * 0.18F, 9 + RANDOM.nextInt(4));
        }
    }

    private static double floor(ClientLevel level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        for (int up = 0; up < 3 && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(); up++) {
            pos.move(0, 1, 0);
        }
        for (int down = 0; down < 5; down++) {
            pos.move(0, -1, 0);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return pos.getY() + 1.0;
            }
        }
        return y;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        clock++;
        Vec3 eye = minecraft.gameRenderer.getMainCamera().getPosition();

        for (Iterator<Ankh> it = ANKHS.values().iterator(); it.hasNext(); ) {
            Ankh ankh = it.next();
            ankh.at = ankh.at.add(ankh.velocity);
            for (int i = 0; i < 4; i++) {
                FlameFx.lick(ankh.at.subtract(ankh.velocity.scale(RANDOM.nextDouble())), 0.3);
            }
            for (int i = 0; i < 3; i++) {
                FlameFx.spark(ankh.at.add((RANDOM.nextDouble() - 0.5) * 0.8, (RANDOM.nextDouble() - 0.5) * 0.8,
                        (RANDOM.nextDouble() - 0.5) * 0.8), ankh.velocity.scale(0.2), 10 + RANDOM.nextInt(8));
            }
            if (--ankh.left < 0) {
                it.remove();
            }
        }

        for (Iterator<Zone> it = ZONES.values().iterator(); it.hasNext(); ) {
            Zone zone = it.next();
            if (clock >= zone.until) {
                it.remove();
                continue;
            }
            if (zone.centre.distanceToSqr(eye) > SEE_RANGE * SEE_RANGE) {
                continue;
            }
            ground(level, zone);
            if (zone.dome) {
                wall(zone, 0.35, DOME_WALL_TALL);
                dome(level, zone);
            } else {
                wall(zone, 0.6, WALL_TALL);
            }
        }

        for (Iterator<Bound> it = BOUND.iterator(); it.hasNext(); ) {
            Bound bound = it.next();
            Entity entity = level.getEntity(bound.entityId());
            if (entity == null || clock >= bound.until()) {
                it.remove();
                continue;
            }
            double around = bound.width() * 0.75 + 0.15;
            for (int ring = 0; ring < 3; ring++) {
                double height = entity.getBbHeight() * (0.25 + ring * 0.25);
                for (int strand = 0; strand < 3; strand++) {
                    double angle = clock * 0.7 + ring * 2.1 + strand * Mth.TWO_PI / 3.0;
                    FlameFx.mote(entity.position().add(Math.cos(angle) * around, height, Math.sin(angle) * around),
                            new Vec3(0.0, 0.01, 0.0), 0.28F, 8);
                }
            }
        }

        for (Iterator<Barrier> it = BARRIERS.values().iterator(); it.hasNext(); ) {
            Barrier barrier = it.next();
            if (clock >= barrier.until) {
                it.remove();
                continue;
            }
            for (int i = 0; i + 1 < barrier.points.size(); i++) {
                if (RANDOM.nextDouble() >= 0.6) {
                    continue;
                }
                Vec3 a = barrier.points.get(i);
                Vec3 where = a.add(barrier.points.get(i + 1).subtract(a).scale(RANDOM.nextDouble()));
                FlameFx.mote(where.add(0.0, 0.35 + RANDOM.nextDouble() * 0.2, 0.0),
                        new Vec3(0.0, 0.04 + RANDOM.nextDouble() * 0.04, 0.0), 0.5F + RANDOM.nextFloat() * 0.2F,
                        12 + RANDOM.nextInt(7));
                if (RANDOM.nextBoolean()) {
                    int life = 10 + RANDOM.nextInt(5);
                    FlameFx.mote(where.add(0.0, 0.6, 0.0), new Vec3(0.0, WALL_TALL * (0.5 + flicker(i)) / life, 0.0),
                            0.4F + RANDOM.nextFloat() * 0.15F, life);
                }
                if (RANDOM.nextInt(6) == 0) {
                    FlameFx.spark(where.add(0.0, 0.5 + RANDOM.nextDouble(), 0.0),
                            new Vec3(0.0, 0.25 + RANDOM.nextDouble() * 0.2, 0.0), 16 + RANDOM.nextInt(12));
                }
            }
        }

        for (Iterator<Kindled> it = KINDLED.iterator(); it.hasNext(); ) {
            Kindled kindled = it.next();
            Entity stand = level.getEntity(kindled.standId());
            if (stand == null || clock >= kindled.until()) {
                it.remove();
                continue;
            }
            for (int i = 0; i < 7; i++) {
                FlameFx.lick(stand.position().add(0.0, RANDOM.nextDouble() * stand.getBbHeight(), 0.0),
                        stand.getBbWidth() * 0.6);
            }
            FlameFx.spark(stand.position().add((RANDOM.nextDouble() - 0.5) * stand.getBbWidth(),
                    RANDOM.nextDouble() * stand.getBbHeight(), (RANDOM.nextDouble() - 0.5) * stand.getBbWidth()),
                    new Vec3(0.0, 0.2 + RANDOM.nextDouble() * 0.15, 0.0), 12 + RANDOM.nextInt(8));
        }

        radar(level, minecraft);
    }

    private static int slots(Zone zone) {
        return Math.max(16, (int) Math.round(Mth.TWO_PI * zone.radius));
    }

    private static void ground(ClientLevel level, Zone zone) {
        int slots = slots(zone);
        boolean stale = zone.floors == null || zone.floors.length != slots || (clock - zone.born) % FLOOR_EVERY == 0;
        if (zone.dome && zone.fires == null) {
            RandomSource seeded = RandomSource.create(Double.doubleToLongBits(zone.centre.x * 31.0 + zone.centre.z));
            zone.fires = new double[DOME_FIRES * 2];
            zone.fireFloors = new double[DOME_FIRES];
            for (int i = 0; i < DOME_FIRES; i++) {
                double angle = seeded.nextDouble() * Mth.TWO_PI;
                double away = 0.2 + Math.sqrt(seeded.nextDouble()) * 0.65;
                zone.fires[i * 2] = Math.cos(angle) * away;
                zone.fires[i * 2 + 1] = Math.sin(angle) * away;
            }
            stale = true;
        }
        if (!stale) {
            return;
        }
        double[] floors = new double[slots];
        for (int i = 0; i < slots; i++) {
            double angle = Mth.TWO_PI * i / slots;
            floors[i] = floor(level, zone.centre.x + Math.cos(angle) * zone.radius, zone.centre.y + 0.5,
                    zone.centre.z + Math.sin(angle) * zone.radius);
        }
        zone.floors = floors;
        if (zone.fires != null) {
            for (int i = 0; i < DOME_FIRES; i++) {
                zone.fireFloors[i] = floor(level, zone.centre.x + zone.fires[i * 2] * zone.radius, zone.centre.y + 0.5,
                        zone.centre.z + zone.fires[i * 2 + 1] * zone.radius);
            }
        }
    }

    private static void ignite(Zone zone) {
        int count = slots(zone) * 2;
        for (int i = 0; i < count; i++) {
            double angle = Mth.TWO_PI * i / count;
            Vec3 at = zone.centre.add(Math.cos(angle) * zone.radius, 0.3, Math.sin(angle) * zone.radius);
            FlameFx.mote(at, new Vec3(0.0, 0.22 + RANDOM.nextDouble() * 0.2, 0.0), 0.55F + RANDOM.nextFloat() * 0.25F,
                    10 + RANDOM.nextInt(6));
            FlameFx.spark(at, new Vec3(0.0, 0.3 + RANDOM.nextDouble() * 0.4, 0.0), 14 + RANDOM.nextInt(12));
        }
        if (zone.dome) {
            FlameFx.burst(zone.centre.add(0.0, 1.0, 0.0), 48, 0.6);
        }
    }

    private static double flicker(int slot) {
        return 0.5 + 0.5 * Math.sin(slot * 1.7 + clock * 0.23) * Math.sin(slot * 0.6 - clock * 0.11);
    }

    private static void wall(Zone zone, double share, double tall) {
        int slots = zone.floors.length;
        for (int i = 0; i < slots; i++) {
            if (RANDOM.nextDouble() >= share) {
                continue;
            }
            double angle = Mth.TWO_PI * (i + RANDOM.nextDouble()) / slots;
            double x = zone.centre.x + Math.cos(angle) * zone.radius;
            double z = zone.centre.z + Math.sin(angle) * zone.radius;
            double y = zone.floors[i];
            FlameFx.mote(new Vec3(x, y + 0.35 + RANDOM.nextDouble() * 0.2, z),
                    new Vec3(0.0, 0.04 + RANDOM.nextDouble() * 0.04, 0.0), 0.5F + RANDOM.nextFloat() * 0.2F,
                    12 + RANDOM.nextInt(7));
            if (RANDOM.nextBoolean()) {
                int life = 10 + RANDOM.nextInt(5);
                double height = tall * (0.5 + flicker(i));
                FlameFx.mote(new Vec3(x, y + 0.6, z), new Vec3(0.0, height / life, 0.0),
                        0.4F + RANDOM.nextFloat() * 0.15F, life);
            }
            if (RANDOM.nextInt(6) == 0) {
                FlameFx.spark(new Vec3(x, y + 0.5 + RANDOM.nextDouble(), z),
                        new Vec3((RANDOM.nextDouble() - 0.5) * 0.1, 0.25 + RANDOM.nextDouble() * 0.2,
                                (RANDOM.nextDouble() - 0.5) * 0.1), 16 + RANDOM.nextInt(12));
            }
        }
    }

    private static Vec3 onDome(double angle, double rise) {
        return new Vec3(Math.cos(angle) * Math.cos(rise), Math.sin(rise), Math.sin(angle) * Math.cos(rise));
    }

    private static void dome(ClientLevel level, Zone zone) {
        for (int stream = 0; stream < DOME_STREAMS; stream++) {
            double rise = RANDOM.nextDouble() * STREAM_REACH;
            double angle = Mth.TWO_PI * stream / DOME_STREAMS + clock * 0.012 + Math.sin(rise * 3.0 + stream) * 0.12;
            Vec3 climb = new Vec3(-Math.sin(rise) * Math.cos(angle), Math.cos(rise), -Math.sin(rise) * Math.sin(angle));
            FlameFx.mote(zone.centre.add(onDome(angle, rise).scale(zone.radius - 0.2)), climb.scale(0.2),
                    0.6F + RANDOM.nextFloat() * 0.3F, 12 + RANDOM.nextInt(5));
        }
        for (int i = 0; i < DOME_FIRES; i++) {
            if (RANDOM.nextInt(3) == 0) {
                FlameFx.lick(new Vec3(zone.centre.x + zone.fires[i * 2] * zone.radius, zone.fireFloors[i] + 0.4,
                        zone.centre.z + zone.fires[i * 2 + 1] * zone.radius), 0.35);
            }
        }
        for (int i = 0; i < 6; i++) {
            double angle = RANDOM.nextDouble() * Mth.TWO_PI;
            double away = Math.sqrt(RANDOM.nextDouble()) * zone.radius;
            double x = zone.centre.x + Math.cos(angle) * away;
            double z = zone.centre.z + Math.sin(angle) * away;
            double y = zone.centre.y + RANDOM.nextDouble() * zone.radius * 0.6;
            if (i < 2) {
                FlameFx.spark(new Vec3(x, y, z), new Vec3(0.0, 0.05 + RANDOM.nextDouble() * 0.1, 0.0),
                        20 + RANDOM.nextInt(16));
            } else {
                level.addParticle(RANDOM.nextBoolean() ? ParticleTypes.ASH : ParticleTypes.WHITE_ASH, x, y, z,
                        0.0, 0.0, 0.0);
            }
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        Vec3 eye = camera.getPosition();
        PoseStack.Pose pose = poseStack.last();
        boolean domes = false;
        boolean rings = false;
        for (Zone zone : ZONES.values()) {
            if (zone.floors != null && zone.centre.distanceToSqr(eye) <= SEE_RANGE * SEE_RANGE) {
                domes |= zone.dome;
                rings |= !zone.dome;
            }
        }
        rings |= !BARRIERS.isEmpty();
        if (domes) {
            domes(pose, buffers, eye, RenderType.entityTranslucentEmissive(FlameFx.BLAZE), false);
            domes(pose, buffers, eye, RenderType.eyes(FlameFx.BLAZE), true);
        }
        if (rings) {
            int frame = (int) (clock / WALL_FRAME_TICKS % WALL_FRAMES.length);
            int behind = (frame + 3) % WALL_FRAMES.length;
            float drift = (clock / 2 % 256) / 256F;
            rings(pose, buffers, eye, RenderType.entityTranslucentEmissive(WALL_FRAMES[frame]), 0.0, 0F, 1F, false);
            rings(pose, buffers, eye, RenderType.eyes(WALL_FRAMES[frame]), 0.0, 0F, 1F, true);
            rings(pose, buffers, eye, RenderType.entityTranslucentEmissive(WALL_FRAMES[behind]), 0.2, 0.37F + drift,
                    0.6F, false);
            rings(pose, buffers, eye, RenderType.eyes(WALL_FRAMES[behind]), 0.2, 0.37F + drift, 0.6F, true);
        }
        if (!ANKHS.isEmpty()) {
            Vector3f leftF = camera.getLeftVector();
            Vec3 side = new Vec3(leftF.x(), 0.0, leftF.z());
            side = side.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : side.normalize();
            ankhs(pose, buffers, eye, side, partialTick, RenderType.entityTranslucentEmissive(ANKH_TEXTURE), false);
            ankhs(pose, buffers, eye, side, partialTick, RenderType.eyes(ANKH_TEXTURE), true);
        }
    }

    private static void finish(MultiBufferSource buffers, RenderType type) {
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(type);
        }
    }

    private static void ankhs(PoseStack.Pose pose, MultiBufferSource buffers, Vec3 eye, Vec3 side, float partialTick,
                              RenderType type, boolean glow) {
        VertexConsumer out = buffers.getBuffer(type);
        int frame = (int) (clock / ANKH_FRAME_TICKS % ANKH_FRAMES);
        float u0 = frame / (float) ANKH_FRAMES;
        float u1 = (frame + 1) / (float) ANKH_FRAMES;
        int argb = glow ? FlameFx.dim(0.9F) : 0xFFFFFFFF;
        for (Ankh ankh : ANKHS.values()) {
            Vec3 at = ankh.at.subtract(ankh.velocity.scale(1.0 - partialTick)).subtract(eye);
            Vec3 wide = side.scale(ANKH_WIDE * ankh.size);
            Vec3 tall = new Vec3(0.0, ANKH_TALL * ankh.size, 0.0);
            face(out, pose, at.add(wide).add(tall), at.add(wide).subtract(tall), at.subtract(wide).subtract(tall),
                    at.subtract(wide).add(tall), u0, u1, 0F, 1F, argb, glow);
        }
        finish(buffers, type);
    }

    private static void domes(PoseStack.Pose pose, MultiBufferSource buffers, Vec3 eye, RenderType type,
                              boolean glow) {
        VertexConsumer out = buffers.getBuffer(type);
        float rise = (clock % 64) / 64F;
        float slow = (clock / 2 % 64) / 64F;
        float drift = (clock / 2 % 256) / 256F;
        for (Zone zone : ZONES.values()) {
            if (!zone.dome || zone.floors == null || zone.centre.distanceToSqr(eye) > SEE_RANGE * SEE_RANGE) {
                continue;
            }
            shell(out, pose, eye, zone, zone.radius, 0F, rise, 1F, glow);
            shell(out, pose, eye, zone, zone.radius + 0.25, 0.37F + drift, slow, 0.55F, glow);
        }
        finish(buffers, type);
    }

    private static void rings(PoseStack.Pose pose, MultiBufferSource buffers, Vec3 eye, RenderType type, double out,
                              float shift, float strength, boolean glow) {
        VertexConsumer consumer = buffers.getBuffer(type);
        for (Zone zone : ZONES.values()) {
            if (zone.dome || zone.floors == null || zone.centre.distanceToSqr(eye) > SEE_RANGE * SEE_RANGE) {
                continue;
            }
            curtain(consumer, pose, eye, zone, zone.radius + out, shift, strength, glow);
        }
        int argb = shade(0xD0, 0.85F, strength, glow);
        for (Barrier barrier : BARRIERS.values()) {
            double run = 0.0;
            for (int i = 0; i + 1 < barrier.points.size(); i++) {
                Vec3 a = barrier.points.get(i);
                Vec3 b = barrier.points.get(i + 1);
                Vec3 flat = new Vec3(b.x - a.x, 0.0, b.z - a.z);
                double span = flat.length();
                if (span < 1.0E-4) {
                    continue;
                }
                Vec3 aside = new Vec3(-flat.z / span, 0.0, flat.x / span).scale(out);
                Vec3 from = a.add(aside).subtract(eye);
                Vec3 to = b.add(aside).subtract(eye);
                float u0 = shift + (float) (run / TILE);
                run += span;
                float u1 = shift + (float) (run / TILE);
                face(consumer, pose, from.add(0.0, WALL_HEIGHT, 0.0), from, to, to.add(0.0, WALL_HEIGHT, 0.0), u0, u1,
                        0F, 1F, argb, glow);
            }
        }
        finish(buffers, type);
    }

    private static int shade(int body, float glowLevel, float strength, boolean glow) {
        return glow ? FlameFx.dim(glowLevel * strength) : ((int) (body * strength) << 24) | 0xFFFFFF;
    }

    private static void curtain(VertexConsumer out, PoseStack.Pose pose, Vec3 eye, Zone zone, double radius,
                                float shift, float strength, boolean glow) {
        int slots = zone.floors.length;
        int tiles = Math.max(1, (int) Math.round(Mth.TWO_PI * radius / TILE));
        double high = WALL_HEIGHT * Math.min(1.0, (clock - zone.born + 1) / (double) RING_RISE_TICKS);
        int argb = shade(0xD0, 0.85F, strength, glow);
        for (int i = 0; i < slots; i++) {
            double a0 = Mth.TWO_PI * i / slots;
            double a1 = Mth.TWO_PI * (i + 1) / slots;
            double x0 = zone.centre.x + Math.cos(a0) * radius - eye.x;
            double z0 = zone.centre.z + Math.sin(a0) * radius - eye.z;
            double x1 = zone.centre.x + Math.cos(a1) * radius - eye.x;
            double z1 = zone.centre.z + Math.sin(a1) * radius - eye.z;
            double base = zone.floors[i] - eye.y;
            float u0 = shift + tiles * i / (float) slots;
            float u1 = shift + tiles * (i + 1) / (float) slots;
            face(out, pose, new Vec3(x0, base + high, z0), new Vec3(x0, base, z0), new Vec3(x1, base, z1),
                    new Vec3(x1, base + high, z1), u0, u1, 0F, 1F, argb, glow);
        }
    }

    private static void shell(VertexConsumer out, PoseStack.Pose pose, Vec3 eye, Zone zone, double radius,
                              float shift, float scroll, float strength, boolean glow) {
        int tiles = Math.max(4, (int) Math.round(Mth.TWO_PI * radius / TILE));
        Vec3 centre = zone.centre.subtract(eye);
        long age = clock - zone.born;
        for (int ring = 0; ring < DOME_RINGS; ring++) {
            if (age < (long) ring * DOME_CLOSE_TICKS) {
                break;
            }
            double low = Mth.HALF_PI * ring / DOME_RINGS;
            double high = Mth.HALF_PI * (ring + 1) / DOME_RINGS;
            float tierStrength = DOME_FADE[ring];
            if (tierStrength <= 0F) {
                break;
            }
            int argb = shade(0x78, 0.7F, strength * tierStrength, glow);
            float vLow = scroll - (float) (radius * low / TILE);
            float vHigh = scroll - (float) (radius * high / TILE);
            for (int i = 0; i < DOME_SEGMENTS; i++) {
                double a0 = Mth.TWO_PI * i / DOME_SEGMENTS;
                double a1 = Mth.TWO_PI * (i + 1) / DOME_SEGMENTS;
                float u0 = shift + tiles * i / (float) DOME_SEGMENTS;
                float u1 = shift + tiles * (i + 1) / (float) DOME_SEGMENTS;
                face(out, pose, centre.add(onDome(a0, high).scale(radius)), centre.add(onDome(a0, low).scale(radius)),
                        centre.add(onDome(a1, low).scale(radius)), centre.add(onDome(a1, high).scale(radius)),
                        u0, u1, vHigh, vLow, argb, glow);
            }
        }
    }

    private static void face(VertexConsumer out, PoseStack.Pose pose, Vec3 topLeft, Vec3 bottomLeft, Vec3 bottomRight,
                             Vec3 topRight, float u0, float u1, float v0, float v1, int argb, boolean both) {
        PixelDraw.quad(out, pose, topLeft, bottomLeft, bottomRight, topRight, u0, u1, v0, v1, argb,
                LightTexture.FULL_BRIGHT);
        if (both) {
            PixelDraw.quad(out, pose, topRight, bottomRight, bottomLeft, topLeft, u1, u0, v0, v1, argb,
                    LightTexture.FULL_BRIGHT);
        }
    }

    private static void radar(ClientLevel level, Minecraft minecraft) {
        if (radarStand < 0) {
            return;
        }
        Entity stand = level.getEntity(radarStand);
        if (stand == null || clock >= radarUntil) {
            radarStand = -1;
            radarMine = false;
            SENSED.clear();
            return;
        }
        Vec3 heart = stand.position().add(0.0, stand.getBbHeight() * 1.15, 0.0);
        for (Vec3 way : SIX_WAYS) {
            for (int i = 0; i < 2; i++) {
                FlameFx.mote(heart.add(way.scale(0.15 + RANDOM.nextDouble() * RADAR_ARM)), way.scale(0.04), 0.26F, 7);
            }
        }
        if (!radarMine || clock % SENSE_EVERY != 0) {
            return;
        }
        SENSED.clear();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof LivingEntity living && living.isAlive() && entity != minecraft.player
                    && !(entity instanceof StandEntity)
                    && entity.distanceToSqr(minecraft.player) <= radarReach * radarReach) {
                SENSED.add(entity.getId());
            }
        }
    }
}
