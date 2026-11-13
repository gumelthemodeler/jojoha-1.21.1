package org.gumel.jojoha.client;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.chariot.Chariot;
import org.gumel.jojoha.stand.combat.BarrageGenerator;
import org.gumel.jojoha.stand.combat.BarragePattern;
import org.gumel.jojoha.stand.combat.PunchType;
import org.gumel.jojoha.stand.combat.StandFist;
import org.gumel.jojoha.stand.combat.StandFists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BarrageLoop {
    public static final int CYCLE_PUNCHES = 6;

    public static final int FOCUS_HOLD_TICKS = 1;

    private static final int CYCLE_TICKS = CYCLE_PUNCHES + FOCUS_HOLD_TICKS;

    public static final double OUT_TICKS = 1.25;

    public static final double BACK_TICKS = 0.75;

    public static final float FOCUS_SCALE = 1.25F;

    private static final double FIND_FACING = 0.4;

    private static final double FIND_BEYOND = 1.0;

    private static final int PHANTOMS_FEWEST = 3;

    private static final int PHANTOMS_SPREAD = 3;

    private static final int PHANTOMS_PER_BUCKET = PHANTOMS_FEWEST + PHANTOMS_SPREAD;

    private static final int PHANTOM_SHORTEST_TICKS = 2;

    private static final int PHANTOM_LONGEST_TICKS = 3;

    private static final double PHANTOM_SWING_FROM = 0.3;

    private static final double PHANTOM_NEAR = 0.55;

    private static final double PHANTOM_MID = 0.75;

    private static final double PHANTOM_FAR = 0.95;

    private static final long PHANTOM_SALT = 0x5DEECE66DL;

    private static final Map<Integer, Integer> TARGETS = new HashMap<>();

    private static final Map<Integer, Long> STARTED = new HashMap<>();

    public record Punch(StandFist fist, long index, double age, double fistAge, boolean focus) {
    }

    private record Memo(double now, Punch[] arms) {
    }

    private static final Map<Integer, Memo> MEMO = new HashMap<>();

    private BarrageLoop() {
    }

    public static boolean runs(StandEntity stand) {
        return stand.isBarraging() && !stand.isKickBarrage() && !Chariot.is(stand);
    }

    public static void clear() {
        BarrageFlow.clear();
        TARGETS.clear();
        MEMO.clear();
        STARTED.clear();
    }

    public static void forget(Set<Integer> keep) {
        BarrageFlow.forget(keep);
        TARGETS.keySet().retainAll(keep);
        MEMO.keySet().retainAll(keep);
        STARTED.keySet().retainAll(keep);
    }

    private static boolean windingUp(StandEntity stand, long start) {
        Long began = STARTED.get(stand.getId());
        return began != null && start - began < BarrageGenerator.WINDUP_TICKS;
    }

    private static boolean thrown(StandEntity stand, long start, int k) {
        return !windingUp(stand, start) || k == CYCLE_PUNCHES - 1;
    }

    private static boolean focused(StandEntity stand, long start, int k) {
        return k == CYCLE_PUNCHES - 1 || windingUp(stand, start)
                || stand.atPeak() && k == CYCLE_PUNCHES / 2 - 1;
    }

    public static void track(StandEntity stand) {
        STARTED.putIfAbsent(stand.getId(), stand.level().getGameTime());
        LivingEntity target = find(stand);
        if (target == null) {
            TARGETS.remove(stand.getId());
        } else {
            TARGETS.put(stand.getId(), target.getId());
        }
    }

    public static LivingEntity target(StandEntity stand) {
        Integer id = TARGETS.get(stand.getId());
        if (id == null) {
            return null;
        }
        Entity entity = stand.level().getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static LivingEntity find(StandEntity stand) {
        Player owner = stand.getOwner();
        Vec3 forward = forward(stand.yBodyRot);
        double reach = StandEntity.barrageReach() + FIND_BEYOND;
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity body : stand.level().getEntitiesOfClass(LivingEntity.class,
                new AABB(stand.position(), stand.position()).inflate(reach),
                entity -> entity != stand && entity != owner && entity.isAlive() && !(entity instanceof StandEntity))) {
            Vec3 flat = new Vec3(body.getX() - stand.getX(), 0, body.getZ() - stand.getZ());
            double distance = flat.length();
            if (distance < 1.0E-3 || distance > reach || distance >= bestDistance
                    || flat.scale(1.0 / distance).dot(forward) < FIND_FACING) {
                continue;
            }
            best = body;
            bestDistance = distance;
        }
        return best;
    }

    public static double life(boolean focus) {
        return OUT_TICKS + BACK_TICKS + (focus ? FOCUS_HOLD_TICKS : 0);
    }

    public static double fistAge(double age, boolean focus) {
        if (age < OUT_TICKS) {
            return age / OUT_TICKS * StandFist.OUT_TICKS;
        }
        double hold = focus ? FOCUS_HOLD_TICKS : 0;
        if (age < OUT_TICKS + hold) {
            return StandFist.OUT_TICKS;
        }
        double back = Math.min(1.0, (age - OUT_TICKS - hold) / BACK_TICKS);
        return StandFist.OUT_TICKS + back * StandFist.BACK_TICKS * 0.999;
    }

    public static double clock(StandEntity stand, float partialTick) {
        return stand.level().getGameTime() + (double) partialTick;
    }

    public static Punch[] punches(StandEntity stand, float partialTick) {
        Punch[] arms = new Punch[2];
        if (!runs(stand)) {
            return arms;
        }
        double now = clock(stand, partialTick);
        Memo memo = MEMO.get(stand.getId());
        if (memo != null && memo.now() == now) {
            return memo.arms();
        }
        long cycle = Mth.floor(now / CYCLE_TICKS);
        for (long c = cycle - 1; c <= cycle; c++) {
            for (int k = 0; k < CYCLE_PUNCHES; k++) {
                long start = c * CYCLE_TICKS + k;
                if (!thrown(stand, start, k)) {
                    continue;
                }
                boolean focus = focused(stand, start, k);
                double age = now - start;
                if (age < 0.0 || age >= life(focus)) {
                    continue;
                }
                int arm = k % 2;
                if (arms[arm] != null && arms[arm].fist().spawnTick() > start) {
                    continue;
                }
                long index = c * CYCLE_PUNCHES + k;
                arms[arm] = new Punch(fist(stand, arm, index, start, focus, partialTick), index, age,
                        fistAge(age, focus), focus);
            }
        }
        MEMO.put(stand.getId(), new Memo(now, arms));
        return arms;
    }

    public static Punch startedAt(StandEntity stand, long start, float partialTick) {
        long cycle = Math.floorDiv(start, CYCLE_TICKS);
        int k = (int) (start - cycle * CYCLE_TICKS);
        if (k >= CYCLE_PUNCHES || !thrown(stand, start, k)) {
            return null;
        }
        boolean focus = focused(stand, start, k);
        long index = cycle * CYCLE_PUNCHES + k;
        double age = clock(stand, partialTick) - start;
        return new Punch(fist(stand, k % 2, index, start, focus, partialTick), index, age, fistAge(age, focus), focus);
    }

    private static StandFist fist(StandEntity stand, int arm, long index, long start, boolean focus, float partialTick) {
        RandomSource random = RandomSource.create(seed(stand.getId(), index));
        float yaw = Mth.rotLerp(partialTick, stand.yBodyRotO, stand.yBodyRot);
        Vec3 right = StandFist.right(yaw);
        boolean fromRight = arm == 0;
        PunchType type = focus ? PunchType.STRAIGHT : BarragePattern.STANDARD.pick(random, stand.combatProfile());
        LivingEntity target = target(stand);
        Vec3 aim;
        if (target != null) {
            double spread = focus ? 0.0 : BarragePattern.STANDARD.spread() * stand.combatProfile().spread();
            aim = BarrageFlow.painted(stand, target, right, start, spread, random, partialTick);
        } else {
            aim = StandFists.intoTheAir(chest(stand, partialTick), look(stand, partialTick),
                    StandEntity.barrageReach(), fromRight, random);
        }
        aim = BarrageFlow.pull(stand, aim, target, right, partialTick);
        return new StandFist(arm, start, aim, type.bendSide(random), type.bendUp(random));
    }

    public record Phantom(StandFist fist, Vec3 shoulder, boolean far, double age) {
    }

    public static List<Phantom> phantoms(StandEntity stand, float partialTick) {
        List<Phantom> out = new ArrayList<>();
        if (!runs(stand) || !JojohaClientConfig.BARRAGE_PHANTOMS.on()) {
            return out;
        }
        double now = clock(stand, partialTick);
        long bucket = Mth.floor(now);
        float yaw = Mth.rotLerp(partialTick, stand.yBodyRotO, stand.yBodyRot);
        Vec3 origin = stand.getPosition(partialTick).add(BarrageFlow.bodyOffset(stand, partialTick));
        Vec3 right = StandFist.right(yaw);
        LivingEntity target = target(stand);
        Vec3 ahead = look(stand, partialTick);
        double spread = BarragePattern.STANDARD.spread() * stand.combatProfile().spread();

        for (long b = bucket - (PHANTOM_LONGEST_TICKS - 1); b <= bucket; b++) {
            int count = PHANTOMS_FEWEST + RandomSource.create(seed(stand.getId(), b) ^ PHANTOM_SALT)
                    .nextInt(PHANTOMS_SPREAD);
            for (int i = 0; i < count; i++) {
                RandomSource random = RandomSource.create(seed(stand.getId(), b * PHANTOMS_PER_BUCKET + i) ^ PHANTOM_SALT);
                int life = PHANTOM_SHORTEST_TICKS + random.nextInt(PHANTOM_LONGEST_TICKS - PHANTOM_SHORTEST_TICKS + 1);
                double progress = (now - b) / life;
                if (progress >= 1.0) {
                    continue;
                }
                boolean far = i % 2 == 1;
                double spin = BarrageFlow.spin(stand, random.nextDouble() * Mth.TWO_PI, now);
                double depth = random.nextDouble();
                boolean clockwise = random.nextBoolean();
                int slot = StandFist.BODY_SLOTS + 2 * random.nextInt((StandFist.SLOTS - StandFist.BODY_SLOTS) / 2)
                        + (BarrageFlow.onRight(spin) ? 0 : 1);
                double reachShare = far ? Mth.lerp(random.nextDouble(), PHANTOM_NEAR, PHANTOM_MID)
                        : Mth.lerp(random.nextDouble(), PHANTOM_MID, PHANTOM_FAR);
                double t = BarrageFlow.trailTime(b, random);
                Vec3 shoulder = BarrageFlow.origin(stand, spin, depth, far, now, partialTick);
                Vec3 from = StandFist.toWorld(origin, yaw, shoulder);
                Vec3 goal = target != null
                        ? BarrageFlow.painted(stand, target, right, t, spread, random, partialTick)
                        : chest(stand, partialTick).add(ahead.scale(StandEntity.barrageReach()));
                goal = BarrageFlow.pull(stand, goal, target, right, partialTick);
                Vec3 reach = goal.subtract(from);
                double length = Math.min(reach.length(), StandEntity.barrageReach());
                Vec3 aim = reach.lengthSqr() < 1.0E-6 ? goal : from.add(reach.normalize().scale(length * reachShare));
                double age = Mth.lerp(progress, PHANTOM_SWING_FROM, 1.0) * StandFist.OUT_TICKS;
                out.add(new Phantom(BarrageFlow.swung(slot, b, aim, spin, clockwise, from.distanceTo(aim)),
                        shoulder, far, age));
            }
        }
        return out;
    }

    public static Vec3 chest(StandEntity stand, float partialTick) {
        return stand.getPosition(partialTick).add(0, stand.getBbHeight() * 0.6, 0);
    }

    public static Vec3 look(StandEntity stand, float partialTick) {
        Player owner = stand.getOwner();
        if (owner != null) {
            return owner.getViewVector(partialTick);
        }
        return forward(Mth.rotLerp(partialTick, stand.yBodyRotO, stand.yBodyRot));
    }

    private static Vec3 forward(float yaw) {
        return StandFist.toWorld(Vec3.ZERO, yaw, new Vec3(0, 0, -16));
    }

    public static long seed(int standId, long index) {
        long seed = standId * 0x9E3779B97F4A7C15L + index * 0xC2B2AE3D27D4EB4FL;
        seed ^= seed >>> 31;
        seed *= 0xBF58476D1CE4E5B9L;
        seed ^= seed >>> 29;
        return seed;
    }
}
