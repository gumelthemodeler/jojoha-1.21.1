package org.gumel.jojoha.stand.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.BarrageBank;
import org.gumel.jojoha.combat.OwnPets;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.stand.StandEntity;

import java.util.Arrays;
import java.util.Optional;

public final class StandFists {
    public static final double FIST_RADIUS = 0.3;

    private static final double AIR_FAN_DEGREES = 38.0;
    private static final double AIR_CROSSOVER = 0.3;
    private static final double AIR_RISE_DEGREES = 20.0;
    private static final double AIR_NEAR = 0.55;
    private static final double AIR_FAR = 0.95;

    public static final double CLASH_RADIUS = 0.9;

    public static final double OVERPOWER_RATIO = 1.25;

    private static final double CLASH_SEARCH = 5.0;

    @FunctionalInterface
    public interface Lands {
        Landing land(LivingEntity victim, Vec3 contact, Vec3 heading, float damage, boolean finisher);
    }

    public enum Landing {
        HIT,
        GUARDED,
        REFUSED
    }

    public record Shot(LivingEntity target, PunchType type, BarragePattern pattern, Vec3 from, Vec3 look, double reach,
                       float blow, double strength, boolean extra, boolean finisher) {
    }

    private final StandFist[] live = new StandFist[StandFist.SLOTS];
    private final boolean[] landed = new boolean[StandFist.SLOTS];
    private final float[] damage = new float[StandFist.SLOTS];
    private final double[] strength = new double[StandFist.SLOTS];
    private final boolean[] extra = new boolean[StandFist.SLOTS];
    private final boolean[] finisher = new boolean[StandFist.SLOTS];
    private final boolean[] aimed = new boolean[StandFist.SLOTS];

    private int nextBodySlot;
    private int nextOuterSlot = StandFist.BODY_SLOTS;

    private double momentum;

    private int beats;
    private long lastBeatTick = Long.MIN_VALUE / 2;

    private double focus = Double.NaN;

    public boolean anyLive() {
        for (StandFist fist : live) {
            if (fist != null) {
                return true;
            }
        }
        return false;
    }

    public void clear() {
        Arrays.fill(live, null);
    }

    public double momentum() {
        return momentum;
    }

    public int beats() {
        return beats;
    }

    public long lastBeat() {
        return lastBeatTick;
    }

    public Optional<StandFist> throwFist(StandEntity stand, Shot shot) {
        long now = stand.level().getGameTime();
        RandomSource random = stand.getRandom();
        if (!shot.extra()) {
            if (now - lastBeatTick > BarrageGenerator.NEW_BARRAGE_GAP) {
                momentum = 0.0;
                beats = 0;
                focus = BarrageGenerator.focusHeight(random);
            }
            lastBeatTick = now;
            beats++;
        }
        int slot = shot.extra() ? outerSlot(now) : bodySlot(now);
        if (slot < 0) {
            return Optional.empty();
        }

        float yaw = stand.yBodyRot;
        Vec3 right = StandFist.right(yaw);
        boolean fromRight = slot % 2 == 0;
        LivingEntity target = shot.target();

        StandCombatProfile profile = stand.combatProfile();
        Vec3 aim;
        if (target != null) {
            double spread = shot.finisher() ? 0.0 : BarrageGenerator.spread(shot.pattern(), momentum, profile);
            double lead = shot.finisher() ? 1.0 : BarrageGenerator.lead(shot.pattern(), momentum, profile);
            double at = shot.pattern() == BarragePattern.CONCENTRATED && !shot.finisher() ? focus : Double.NaN;
            aim = BarrageGenerator.aim(shot.type(), target.position(), target.getBbWidth(), target.getBbHeight(),
                    target.getDeltaMovement(), target.onGround(), right, fromRight, spread, lead, at, random);
        } else {
            aim = intoTheAir(shot.from(), shot.look(), shot.reach(), fromRight, random);
        }

        StandFist fist = new StandFist(slot, now, aim, shot.type().bendSide(random), shot.type().bendUp(random));
        live[slot] = fist;
        landed[slot] = false;
        damage[slot] = shot.blow();
        this.strength[slot] = shot.strength();
        extra[slot] = shot.extra();
        finisher[slot] = shot.finisher();
        aimed[slot] = target != null;
        return Optional.of(fist);
    }

    public static Vec3 intoTheAir(Vec3 from, Vec3 look, double reach, boolean fromRight, RandomSource random) {
        Vec3 forward = look.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : look.normalize();
        Vec3 side = forward.cross(new Vec3(0, 1, 0));
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : side.normalize();
        Vec3 up = side.cross(forward).normalize();

        double across = Mth.lerp(random.nextDouble(), -AIR_CROSSOVER, 1.0) * (fromRight ? 1 : -1);
        double yaw = Math.toRadians(across * AIR_FAN_DEGREES);
        double pitch = Math.toRadians(signed(random) * AIR_RISE_DEGREES);
        Vec3 direction = forward.scale(Math.cos(yaw) * Math.cos(pitch))
                .add(side.scale(Math.sin(yaw) * Math.cos(pitch)))
                .add(up.scale(Math.sin(pitch)));
        return from.add(direction.scale(reach * Mth.lerp(random.nextDouble(), AIR_NEAR, AIR_FAR)));
    }

    public void tick(StandEntity stand, Player owner, Lands lands) {
        if (!(stand.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        float yaw = stand.yBodyRot;
        Vec3 right = StandFist.right(yaw);

        for (int slot = 0; slot < StandFist.SLOTS; slot++) {
            StandFist fist = live[slot];
            if (fist == null) {
                continue;
            }
            double age = now - fist.spawnTick();
            if (fist.finished(age)) {
                if (!landed[slot] && aimed[slot]) {
                    momentum = BarrageGenerator.momentum(momentum, BarrageGenerator.MISS);
                }
                live[slot] = null;
                continue;
            }
            if (landed[slot] || !fist.outbound(age)) {
                continue;
            }

            Vec3 shoulder = StandFist.shoulderWorld(stand.position(), yaw, slot);
            Vec3 from = fist.at(shoulder, right, age - 1);
            Vec3 to = fist.at(shoulder, right, age);

            if (clash(level, stand, slot, from, to, now)) {
                continue;
            }

            LivingEntity hit = firstOnPath(level, stand, owner, from, to);
            if (hit != null) {
                landed[slot] = true;
                Landing landing = lands.land(hit, to, to.subtract(from), damage[slot], finisher[slot]);
                momentum = BarrageGenerator.momentum(momentum, BarrageGenerator.change(switch (landing) {
                    case HIT -> extra[slot] ? BarrageGenerator.EXTRA_HIT : BarrageGenerator.HIT;
                    case GUARDED -> BarrageGenerator.GUARDED;
                    case REFUSED -> 0.0;
                }, stand.combatProfile()));
            }
        }
    }

    private boolean clash(ServerLevel level, StandEntity stand, int slot, Vec3 from, Vec3 to, long now) {
        StandFist fist = live[slot];
        AABB near = new AABB(from, to).inflate(CLASH_SEARCH);
        for (StandEntity other : level.getEntitiesOfClass(StandEntity.class, near,
                entity -> entity != stand && entity.isAlive())) {
            StandFists theirs = other.fists();
            float otherYaw = other.yBodyRot;
            Vec3 otherRight = StandFist.right(otherYaw);
            for (int o = 0; o < StandFist.SLOTS; o++) {
                StandFist their = theirs.live[o];
                if (their == null || theirs.landed[o]) {
                    continue;
                }
                double theirAge = now - their.spawnTick();
                if (!their.outbound(theirAge)) {
                    continue;
                }
                Vec3 theirShoulder = StandFist.shoulderWorld(other.position(), otherYaw, o);
                Vec3[] closest = closestPoints(from, to,
                        their.at(theirShoulder, otherRight, theirAge - 1), their.at(theirShoulder, otherRight, theirAge));
                if (closest[0].distanceToSqr(closest[1]) > CLASH_RADIUS * CLASH_RADIUS) {
                    continue;
                }

                Vec3 contact = closest[0].lerp(closest[1], 0.5);
                boolean perfect = fist.spawnTick() == their.spawnTick();
                double mine = strength[slot], yours = theirs.strength[o];
                StandEntity through = perfect ? null
                        : mine >= yours * OVERPOWER_RATIO ? stand
                        : yours >= mine * OVERPOWER_RATIO ? other
                        : null;
                if (through != other) {
                    theirs.knockBack(other, o, contact, now);
                }
                if (through != stand) {
                    knockBack(stand, slot, contact, now);
                }
                stand.fistsClashed(other, contact, to.subtract(from), perfect, through);
                return through != stand;
            }
        }
        return false;
    }

    private void knockBack(StandEntity stand, int slot, Vec3 contact, long now) {
        StandFist stopped = new StandFist(slot, now - StandFist.OUT_TICKS, contact, 0F, 0F);
        live[slot] = stopped;
        landed[slot] = true;
        momentum = BarrageGenerator.momentum(momentum, BarrageGenerator.KNOCKED_BACK);
        stand.fistKnockedBack(stopped);
    }

    static Vec3[] closestPoints(Vec3 p1, Vec3 q1, Vec3 p2, Vec3 q2) {
        final double eps = 1.0E-9;
        Vec3 d1 = q1.subtract(p1), d2 = q2.subtract(p2), r = p1.subtract(p2);
        double a = d1.dot(d1), e = d2.dot(d2), f = d2.dot(r);
        double s, t;
        if (a <= eps && e <= eps) {
            s = 0;
            t = 0;
        } else if (a <= eps) {
            s = 0;
            t = Mth.clamp(f / e, 0, 1);
        } else {
            double c = d1.dot(r);
            if (e <= eps) {
                t = 0;
                s = Mth.clamp(-c / a, 0, 1);
            } else {
                double b = d1.dot(d2);
                double denom = a * e - b * b;
                s = denom > eps ? Mth.clamp((b * f - c * e) / denom, 0, 1) : 0;
                t = (b * s + f) / e;
                if (t < 0) {
                    t = 0;
                    s = Mth.clamp(-c / a, 0, 1);
                } else if (t > 1) {
                    t = 1;
                    s = Mth.clamp((b - c) / a, 0, 1);
                }
            }
        }
        return new Vec3[] {p1.add(d1.scale(s)), p2.add(d2.scale(t))};
    }

    private static LivingEntity firstOnPath(ServerLevel level, StandEntity stand, Player owner, Vec3 from, Vec3 to) {
        AABB sweep = new AABB(from, to).inflate(FIST_RADIUS + 1.0);
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, sweep,
                entity -> entity != owner && entity != stand && entity.isAlive() && StandHits.hittable(owner, entity)
                        && !OwnPets.tamedBy(owner, entity) && !BarrageBank.slipping(entity))) {
            AABB body = candidate.getBoundingBox().inflate(FIST_RADIUS);
            double distance;
            if (body.contains(from)) {
                distance = 0;
            } else {
                Optional<Vec3> entry = body.clip(from, to);
                if (entry.isEmpty()) {
                    continue;
                }
                distance = entry.get().distanceToSqr(from);
            }
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private int bodySlot(long now) {
        for (int i = 0; i < StandFist.BODY_SLOTS; i++) {
            int slot = (nextBodySlot + i) % StandFist.BODY_SLOTS;
            if (free(slot, now)) {
                nextBodySlot = (slot + 1) % StandFist.BODY_SLOTS;
                return slot;
            }
        }
        return outerSlot(now);
    }

    private int outerSlot(long now) {
        int outer = StandFist.SLOTS - StandFist.BODY_SLOTS;
        for (int i = 0; i < outer; i++) {
            int slot = StandFist.BODY_SLOTS + Math.floorMod(nextOuterSlot - StandFist.BODY_SLOTS + i, outer);
            if (free(slot, now)) {
                nextOuterSlot = StandFist.BODY_SLOTS + Math.floorMod(slot - StandFist.BODY_SLOTS + 1, outer);
                return slot;
            }
        }
        return -1;
    }

    private boolean free(int slot, long now) {
        StandFist fist = live[slot];
        return fist == null || fist.finished(now - fist.spawnTick());
    }

    private static double signed(RandomSource random) {
        return random.nextDouble() * 2.0 - 1.0;
    }
}
