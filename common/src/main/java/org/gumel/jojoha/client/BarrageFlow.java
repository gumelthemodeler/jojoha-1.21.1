package org.gumel.jojoha.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.combat.StandFist;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class BarrageFlow {
    public enum Path {
        COLUMN, FIGURE_EIGHT, WEAVE
    }

    private static final Map<ResourceLocation, Path> BY_STAND = Map.of(
            StandTypes.STAR_PLATINUM_ID, Path.COLUMN,
            StandTypes.THE_WORLD_ID, Path.FIGURE_EIGHT,
            StandTypes.SECRET_THE_WORLD_ID, Path.FIGURE_EIGHT);

    private static final double CHEST = 0.6;

    private static final int HEAD_EVERY = 15;
    private static final int HEAD_TICKS = 2;
    private static final double HEAD_AT = 0.86;

    private static final double JITTER = 0.5;

    private static final double DRIFT_SPEED = 0.18;
    private static final double DRIFT_SIDE_PX = 2.0;
    private static final double DRIFT_UP_PX = 1.2;

    private static final double RADIUS_TIGHT = 8.0;
    private static final double RADIUS_WIDE = 14.0;
    private static final double RING_PEAK = 1.15;
    private static final double RING_SQUASH = 0.85;
    private static final double RING_DEPTH = 4.0;
    private static final double RING_SPIN = 0.05;
    private static final double TORSO_Y = 21.0;

    private static final double SWIRL = 0.6;
    private static final double BULGE = 0.3;

    private static final double FAR_RADIUS = 4.0;
    private static final double FAR_BEHIND = 3.0;
    private static final double TRAIL_TICKS = 3.0;

    private static final double COLLAPSE_RADIUS = 0.5;

    private static final double SURGE = 0.16;
    private static final double SWAY = 0.07;

    private static final class Smooth {
        long tick = Long.MIN_VALUE;
        float momentumLast;
        float momentum;
        float closingLast;
        float closing;
    }

    private static final Map<Integer, Smooth> SMOOTH = new HashMap<>();

    private BarrageFlow() {
    }

    public static void clear() {
        SMOOTH.clear();
    }

    public static void forget(Set<Integer> keep) {
        SMOOTH.keySet().retainAll(keep);
    }

    private static Smooth smooth(StandEntity stand) {
        Smooth smooth = SMOOTH.computeIfAbsent(stand.getId(), id -> new Smooth());
        long now = stand.level().getGameTime();
        if (smooth.tick != now) {
            boolean fresh = smooth.tick == Long.MIN_VALUE || now - smooth.tick > 1;
            smooth.momentumLast = fresh ? stand.barrageMomentum() : smooth.momentum;
            smooth.closingLast = fresh ? stand.barrageClosing() : smooth.closing;
            smooth.momentum = stand.barrageMomentum();
            smooth.closing = stand.barrageClosing();
            smooth.tick = now;
        }
        return smooth;
    }

    public static float momentum(StandEntity stand, float partialTick) {
        Smooth smooth = smooth(stand);
        return Mth.lerp(partialTick, smooth.momentumLast, smooth.momentum);
    }

    public static float closing(StandEntity stand, float partialTick) {
        Smooth smooth = smooth(stand);
        float c = Mth.clamp(Mth.lerp(partialTick, smooth.closingLast, smooth.closing), 0F, 1F);
        return c * c * (3F - 2F * c);
    }

    public static Path path(StandEntity stand) {
        Path named = stand.getStandType() == null ? null : BY_STAND.get(stand.getStandType().id());
        return named != null ? named : Path.WEAVE;
    }

    public static double across(Path path, double t) {
        return switch (path) {
            case COLUMN -> head(t) ? 0.12 * Math.sin(t * 0.9)
                    : 0.25 * Math.sin(t * 0.9) + 0.1 * Math.sin(t * 2.3);
            case FIGURE_EIGHT -> 0.8 * Math.sin(t * 0.2);
            case WEAVE -> 0.55 * Math.sin(t * 0.31);
        };
    }

    public static double height(Path path, double t) {
        return switch (path) {
            case COLUMN -> head(t) ? HEAD_AT : 0.62 + 0.08 * Math.sin(t * 0.53);
            case FIGURE_EIGHT -> 0.58 + 0.2 * Math.sin(t * 0.4);
            case WEAVE -> 0.6 + 0.16 * Math.sin(t * 0.47 + 1.3);
        };
    }

    private static boolean head(double t) {
        return Math.floorMod((long) Math.floor(t), HEAD_EVERY) < HEAD_TICKS;
    }

    public static Vec3 onBody(LivingEntity target, Vec3 right, double across, double height, float partialTick) {
        return target.getPosition(partialTick)
                .add(0, target.getBbHeight() * height, 0)
                .add(right.scale(across * target.getBbWidth() * 0.5));
    }

    public static Vec3 painted(StandEntity stand, LivingEntity target, Vec3 right, double t, double spread,
                               RandomSource random, float partialTick) {
        Path path = path(stand);
        double across = across(path, t) + signed(random) * JITTER * spread;
        double height = height(path, t) + signed(random) * JITTER * 0.6 * spread;
        return onBody(target, right, Mth.clamp(across, -1.0, 1.0), Mth.clamp(height, 0.15, 0.95), partialTick);
    }

    public static Vec3 converge(StandEntity stand, LivingEntity target, Vec3 right, float partialTick) {
        if (target != null) {
            return onBody(target, right, 0.0, CHEST, partialTick);
        }
        return BarrageLoop.chest(stand, partialTick)
                .add(BarrageLoop.look(stand, partialTick).scale(StandEntity.barrageReach()));
    }

    public static Vec3 pull(StandEntity stand, Vec3 aim, LivingEntity target, Vec3 right, float partialTick) {
        float c = closing(stand, partialTick);
        return c <= 0F ? aim : aim.lerp(converge(stand, target, right, partialTick), c);
    }

    public static double trailTime(double bucket, RandomSource random) {
        return bucket - random.nextDouble() * TRAIL_TICKS;
    }

    public static double spin(StandEntity stand, double angle, double now) {
        return angle + now * RING_SPIN + stand.getId() * 0.7;
    }

    public static Vec3 origin(StandEntity stand, double spin, double depth, boolean far,
                              double now, float partialTick) {
        float m = momentum(stand, partialTick);
        float c = closing(stand, partialTick);
        double radius = (Mth.lerp(m, RADIUS_TIGHT, RADIUS_WIDE) + (far ? FAR_RADIUS : 0.0))
                * (stand.atPeak() ? RING_PEAK : 1.0) * (1.0 - COLLAPSE_RADIUS * c);
        double phase = stand.getId() * 0.7;
        double x = Math.cos(spin) * radius + DRIFT_SIDE_PX * Math.sin(now * DRIFT_SPEED + phase);
        double y = TORSO_Y + Math.sin(spin) * radius * RING_SQUASH
                + DRIFT_UP_PX * Math.sin(now * DRIFT_SPEED * 2.0 + phase);
        double z = depth * RING_DEPTH + (far ? FAR_BEHIND : 0.0) - 1.0;
        return new Vec3(x, y, z);
    }

    public static boolean onRight(double spin) {
        return Math.cos(spin) >= 0.0;
    }

    public static StandFist swung(int slot, long spawnTick, Vec3 aim, double spin, boolean clockwise, double reach) {
        double ox = Math.cos(spin);
        double oy = Math.sin(spin);
        double turn = clockwise ? 1.0 : -1.0;
        double side = (-oy * turn * SWIRL + ox * BULGE) * reach;
        double up = (ox * turn * SWIRL + oy * BULGE) * reach;
        boolean fromRight = slot % 2 == 0;
        return new StandFist(slot, spawnTick, aim, (float) (fromRight ? side : -side), (float) up);
    }

    public static Vec3 bodyOffset(StandEntity stand, float partialTick) {
        if (!BarrageLoop.runs(stand)) {
            return Vec3.ZERO;
        }
        float yaw = Mth.rotLerp(partialTick, stand.yBodyRotO, stand.yBodyRot);
        double now = BarrageLoop.clock(stand, partialTick);
        float m = momentum(stand, partialTick);
        double surge = 0.0;
        for (BarrageLoop.Punch punch : BarrageLoop.punches(stand, partialTick)) {
            if (punch != null && punch.focus()) {
                double hold = BarrageLoop.OUT_TICKS + BarrageLoop.FOCUS_HOLD_TICKS;
                surge = Math.max(surge, punch.age() < hold ? Math.sin(Math.PI * punch.age() / hold) : 0.0);
            }
        }
        Vec3 forward = StandFist.toWorld(Vec3.ZERO, yaw, new Vec3(0, 0, -16));
        Vec3 right = StandFist.right(yaw);
        double sway = SWAY * m * Math.sin(now * DRIFT_SPEED + stand.getId() * 0.7);
        return forward.scale(SURGE * surge).add(right.scale(sway));
    }

    private static double signed(RandomSource random) {
        return random.nextDouble() * 2.0 - 1.0;
    }
}
