package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class DowsingFx {
    private static final double STEP = 0.5;
    private static final int CLIMB = 3;
    private static final int DROP = 6;
    private static final double LIFT = 0.12;
    private static final double ARRIVE = 1.0;

    private static final double GROW_PER_TICK = 1.6;
    private static final int WITHER_TICKS = 20;

    private static final int STRANDS = 3;
    private static final double BRAID = 0.18;
    private static final double TWIST = 1.1;
    private static final double TAPER = 1.5;
    private static final double SAMPLE = 0.4;

    private static final float[] MARK_TINT = {0.72F, 0.42F, 1.00F};
    private static final float MARK_ALPHA = 0.32F;
    private static final double MARK_SWELL = 0.03;

    private static final ResourceLocation WHITE =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private static final List<Dowse> ACTIVE = new ArrayList<>();

    private DowsingFx() {
    }

    public static void begin(int playerId, BlockPos target, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || ticks <= 0) {
            return;
        }
        Entity owner = minecraft.level.getEntity(playerId);
        Vec3 from = owner == null ? Vec3.atCenterOf(target).add(0, 1, 0) : owner.position();
        ACTIVE.removeIf(dowse -> dowse.playerId == playerId);
        ACTIVE.add(new Dowse(playerId, target, ticks, ground(minecraft.level, from, target)));
    }

    public static void tick() {
        Iterator<Dowse> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Dowse dowse = it.next();
            if (++dowse.age >= dowse.ticks) {
                it.remove();
            }
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static List<Vec3> ground(Level level, Vec3 from, BlockPos target) {
        Vec3 goal = Vec3.atCenterOf(target);
        List<Vec3> points = new ArrayList<>();
        double dx = goal.x - from.x;
        double dz = goal.z - from.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        int y = BlockPos.containing(from).getY();

        for (double walked = 0; flat > ARRIVE && walked <= flat - ARRIVE; walked += STEP) {
            double x = from.x + dx * walked / flat;
            double z = from.z + dz * walked / flat;
            int floor = floor(level, x, z, y);
            if (floor == Integer.MIN_VALUE) {
                break;
            }
            y = floor;
            points.add(new Vec3(x, y + LIFT, z));
        }
        if (points.isEmpty()) {
            points.add(from.add(0, LIFT, 0));
        }
        points.add(goal);
        return points;
    }

    private static int floor(Level level, double x, double z, int y) {
        BlockPos at = BlockPos.containing(x, y, z);
        if (solid(level, at)) {
            for (int up = 1; up <= CLIMB; up++) {
                if (!solid(level, at.above(up))) {
                    return y + up;
                }
            }
            return Integer.MIN_VALUE;
        }
        for (int down = 0; down < DROP; down++) {
            if (solid(level, at.below(down + 1))) {
                return y - down;
            }
        }
        return y - DROP;
    }

    private static boolean solid(Level level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    public static void solid(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ACTIVE.isEmpty() || minecraft.level == null) {
            return;
        }
        Vec3 cameraPos = camera.getPosition();

        for (Dowse dowse : ACTIVE) {
            Entity owner = minecraft.level.getEntity(dowse.playerId);
            List<Vec3> line = new ArrayList<>(dowse.ground.size() + 1);
            if (owner instanceof Player player) {
                line.add(player.getRopeHoldPosition(partialTick));
            }
            line.addAll(dowse.ground);
            if (line.size() < 2) {
                continue;
            }

            double[] along = lengths(line);
            double total = along[along.length - 1];
            double head = Math.min(total, dowse.age * GROW_PER_TICK);
            int left = dowse.ticks - dowse.age;
            double tail = left < WITHER_TICKS ? head * (1.0 - left / (double) WITHER_TICKS) : 0.0;
            if (head - tail < SAMPLE) {
                continue;
            }

            Vec3 origin = line.get(0);
            int skin = owner == null ? 0 : HermitSkins.of(owner);

            poseStack.pushPose();
            poseStack.translate(origin.x - cameraPos.x, origin.y - cameraPos.y, origin.z - cameraPos.z);
            for (int strand = 0; strand < STRANDS; strand++) {
                List<Vec3> points = new ArrayList<>();
                for (double s = tail; ; s = Math.min(head, s + SAMPLE)) {
                    points.add(braid(line, along, s, total, tail, strand).subtract(origin));
                    if (s >= head) {
                        break;
                    }
                }
                ThornRope.path(poseStack, buffers, minecraft.level, points, origin, skin);
            }
            poseStack.popPose();
        }
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ACTIVE.isEmpty() || minecraft.level == null) {
            return;
        }
        Vec3 cameraPos = camera.getPosition();
        VertexConsumer buffer = buffers.getBuffer(RenderType.textSeeThrough(WHITE));
        boolean drew = false;

        for (Dowse dowse : ACTIVE) {
            Entity owner = minecraft.level.getEntity(dowse.playerId);
            if (owner != minecraft.player) {
                continue;
            }
            List<Vec3> line = new ArrayList<>(dowse.ground);
            line.add(0, owner.position());
            double total = lengths(line)[line.size() - 1];
            if (dowse.age * GROW_PER_TICK < total) {
                continue;
            }
            int left = dowse.ticks - dowse.age;
            float fade = Mth.clamp(left / (float) WITHER_TICKS, 0F, 1F);
            float pulse = (Mth.sin((dowse.age + partialTick) * 0.3F) + 1F) * 0.5F;
            float alpha = MARK_ALPHA * (0.5F + 0.5F * pulse) * fade;

            BlockPos pos = dowse.target;
            GlowBox.fillLit(poseStack, buffer, new AABB(pos).inflate(MARK_SWELL)
                    .move(-cameraPos.x, -cameraPos.y, -cameraPos.z), MARK_TINT, alpha);
            drew = true;
        }

        if (drew && buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(RenderType.textSeeThrough(WHITE));
        }
    }

    private static double[] lengths(List<Vec3> line) {
        double[] along = new double[line.size()];
        for (int i = 1; i < line.size(); i++) {
            along[i] = along[i - 1] + line.get(i).distanceTo(line.get(i - 1));
        }
        return along;
    }

    private static Vec3 braid(List<Vec3> line, double[] along, double s, double total, double tail, int strand) {
        int seg = 1;
        while (seg < line.size() - 1 && along[seg] < s) {
            seg++;
        }
        Vec3 a = line.get(seg - 1);
        Vec3 b = line.get(seg);
        double span = along[seg] - along[seg - 1];
        double t = span < 1.0E-9 ? 0.0 : Mth.clamp((s - along[seg - 1]) / span, 0.0, 1.0);
        Vec3 base = a.lerp(b, t);

        Vec3 dir = span < 1.0E-9 ? new Vec3(0, 1, 0) : b.subtract(a).scale(1.0 / span);
        Vec3 side = dir.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 lift = side.cross(dir).normalize();

        double taper = Mth.clamp(Math.min((s - tail) / TAPER, (total - s) / TAPER), 0.0, 1.0);
        double angle = s * TWIST + strand * (Math.PI * 2.0 / STRANDS);
        return base.add(side.scale(Math.cos(angle) * BRAID * taper))
                .add(lift.scale(Math.sin(angle) * BRAID * taper));
    }

    private static final class Dowse {
        private final int playerId;
        private final BlockPos target;
        private final int ticks;
        private final List<Vec3> ground;
        private int age;

        private Dowse(int playerId, BlockPos target, int ticks, List<Vec3> ground) {
            this.playerId = playerId;
            this.target = target;
            this.ticks = ticks;
            this.ground = ground;
        }
    }
}
