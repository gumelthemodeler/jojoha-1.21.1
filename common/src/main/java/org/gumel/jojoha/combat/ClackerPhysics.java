package org.gumel.jojoha.combat;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class ClackerPhysics {
    public static final double REACH = 3.0;
    public static final double SPIN_REACH = 0.55;
    public static final double REST_LENGTH = 0.45;
    public static final double REEL_PER_TICK = 0.35;
    public static final double GRAVITY = 0.08;
    public static final double DRAG = 0.94;
    public static final double VERTICAL_DRAG = 0.98;
    public static final double HEAD_RADIUS = 0.12;
    public static final int THROW_TICKS = 10;
    public static final double THROW_SPEED = 1.6;
    public static final double TRAIL_SPREAD_DEGREES = 7.0;
    public static final int TRAIL_DELAY_TICKS = 1;
    public static final int COOLDOWN_TICKS = 12;
    public static final int THROW_WINDUP_TICKS = 7;
    public static final int WHIRL_START_TICKS = 3;
    public static final int WHIRL_LAUNCH_TICKS = 22;
    public static final double WHIRL_TURN_TICKS = 7.5;
    public static final int WHIRL_REHIT_TICKS = 8;
    public static final double LAUNCH_REACH = 4.0;
    public static final double LAUNCH_SPEED = 2.0;
    public static final double LAUNCH_SPREAD_DEGREES = 14.0;
    public static final int LAUNCH_COOLDOWN_TICKS = 24;
    public static final int HEADS = 2;
    public static final double BALL_SIZE = 0.1875;
    public static final double BALL_GAP = BALL_SIZE + 0.02;
    public static final double BALL_BOUNCE = 0.4;
    public static final int VOLLEY_PERIOD = 4;
    public static final int VOLLEY_HITS = 8;
    public static final double VOLLEY_REACH = 4.5;
    public static final double VOLLEY_BREAK = 6.0;
    public static final double VOLLEY_SWERVE = 0.45;
    private static final double VOLLEY_TURN = 2.39996;
    private static final double VOLLEY_NEAREST = 0.12;
    public static final int STRING_NODES = 10;

    private static final double GROUND_FRICTION = 0.7;
    private static final double GROUND_PROBE = HEAD_RADIUS + 0.05;
    private static final double BLOCK_REEL_SPEED = 0.45;
    private static final double BLOCK_STOP_IMPACT = 0.35;
    private static final double ENTITY_RESTITUTION = 0.06;
    private static final double ENTITY_FRICTION = 0.7;
    private static final double TAUT_START = 0.8;
    private static final double TAUT_BLEND = 0.85;
    private static final double STRING_GRAVITY = 0.04;
    private static final double STRING_DAMPING = 0.95;
    private static final int STRING_ITERATIONS = 8;
    private static final double STRING_NODE_RADIUS = 0.04;

    private static final double HOLD_OUT = 0.12;
    private static final double HOLD_FORWARD = 0.1;
    private static final double HOLD_UP = -0.15;

    private ClackerPhysics() {
    }

    public static final class Head {
        public Vec3 pos;
        public Vec3 prevPos;
        public Vec3 vel = Vec3.ZERO;
        public Vec3 pendingThrow;
        public int throwDelay;
        public boolean onGround;
        public double groundImpact;
        public final Vec3[] string = new Vec3[STRING_NODES];
        public final Vec3[] stringPrev = new Vec3[STRING_NODES];
        public boolean stringReady;

        Head(Vec3 start) {
            this.pos = start;
            this.prevPos = start;
        }
    }

    public static final class State {
        public final Head[] heads = new Head[HEADS];
        public InteractionHand hand;
        public Vec3 lastAnchor;
        public double length = REST_LENGTH;
        public double prevLength = REST_LENGTH;
        public int throwTicks;
        public boolean spinning;
        public int spinElapsed;
        public boolean launched;
        public int windup;
        public int cooldown;
        public double contact;
        public int volleyTarget = -1;
        public int volleyTicks;
        public int volleyAge;
        public int volleyStrike = -1;
        public final Set<Integer> struck = new HashSet<>();

        public State(Vec3 anchor, InteractionHand hand) {
            this.hand = hand;
            this.lastAnchor = anchor;
            for (int i = 0; i < HEADS; i++) {
                heads[i] = new Head(anchor.add(0.0, -REST_LENGTH, 0.0));
            }
        }

        public boolean active() {
            return throwTicks > 0 || spinning || windup > 0 || pendingAny();
        }

        public boolean volleying() {
            return volleyTicks > 0;
        }

        boolean pendingAny() {
            for (Head head : heads) {
                if (head.pendingThrow != null) {
                    return true;
                }
            }
            return false;
        }
    }

    public static Vec3 anchor(Player player, InteractionHand hand, float partialTick) {
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        double side = arm == HumanoidArm.RIGHT ? -1.0 : 1.0;
        float bodyYaw = (float) (Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * Math.PI / 180.0);
        double height = player.getBoundingBox().getYsize() - 1.0;
        double forward = player.isCrouching() ? -0.2 : 0.07;
        return player.getPosition(partialTick).add(new Vec3(side * (0.22 + HOLD_OUT), height + HOLD_UP,
                forward + HOLD_FORWARD).yRot(-bodyYaw));
    }

    public static boolean canThrow(State s) {
        return s.cooldown <= 0 && !s.spinning && s.windup <= 0 && !s.pendingAny() && s.throwTicks <= 0
                && !s.volleying();
    }

    public static void windUp(State s) {
        s.windup = THROW_WINDUP_TICKS;
        s.cooldown = COOLDOWN_TICKS + THROW_WINDUP_TICKS;
    }

    public static void volley(State s, int targetId) {
        s.volleyTarget = targetId;
        s.volleyTicks = VOLLEY_HITS * VOLLEY_PERIOD / HEADS + 1;
        s.volleyAge = 0;
        s.volleyStrike = -1;
        s.spinning = false;
        s.spinElapsed = 0;
        s.windup = 0;
        s.throwTicks = 0;
        s.struck.clear();
        for (Head head : s.heads) {
            head.pendingThrow = null;
        }
    }

    public static Vec3 volleyPoint(int head, float age, Vec3 anchor, Entity target) {
        double cycle = (age + head * (VOLLEY_PERIOD / (double) HEADS)) / VOLLEY_PERIOD;
        double strike = Math.floor(cycle);
        double phase = cycle - strike;
        Vec3 centre = target.getBoundingBox().getCenter();
        Vec3 toward = new Vec3(centre.x - anchor.x, 0.0, centre.z - anchor.z);
        toward = toward.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : toward.normalize();
        Vec3 side = new Vec3(-toward.z, 0.0, toward.x);
        double turn = strike * VOLLEY_TURN + head * Math.PI;
        double width = target.getBbWidth();
        double height = target.getBbHeight();
        Vec3 hit = centre.subtract(toward.scale(width * 0.5))
                .add(side.scale(Math.cos(turn) * width * 0.45))
                .add(0.0, Math.sin(turn) * height * 0.3, 0.0);
        double out = VOLLEY_NEAREST + (1.0 - VOLLEY_NEAREST) * Math.sin(Math.PI * phase);
        double swerve = Math.sin(2.0 * Math.PI * phase) * VOLLEY_SWERVE;
        return anchor.add(hit.subtract(anchor).scale(out))
                .add(side.scale(swerve * (head == 0 ? 1.0 : -1.0)))
                .add(0.0, swerve * 0.5, 0.0);
    }

    public static boolean strikesAt(int head, int age) {
        return (age + head * (VOLLEY_PERIOD / HEADS)) % VOLLEY_PERIOD == VOLLEY_PERIOD / 2;
    }

    public static void throwAt(State s, Player player, Vec3 anchor) {
        s.launched = false;
        s.cooldown = Math.max(s.cooldown, COOLDOWN_TICKS);
        aim(s, player, anchor, REACH, TRAIL_SPREAD_DEGREES, TRAIL_DELAY_TICKS);
    }

    private static void launch(State s, Player player, Vec3 anchor) {
        s.launched = true;
        s.cooldown = LAUNCH_COOLDOWN_TICKS;
        aim(s, player, anchor, LAUNCH_REACH, LAUNCH_SPREAD_DEGREES, 0);
    }

    private static void aim(State s, Player player, Vec3 anchor, double reach, double spread, int delay) {
        Vec3 eye = player.getEyePosition();
        Vec3 aim = eye.add(player.getLookAngle().scale(reach)).subtract(anchor);
        Vec3 dir = aim.lengthSqr() < 1.0E-6 ? player.getLookAngle() : aim.normalize();
        s.struck.clear();
        s.spinning = false;
        s.spinElapsed = 0;
        s.windup = 0;
        for (int i = 0; i < HEADS; i++) {
            Head head = s.heads[i];
            double turn = i == 0 ? 0.0 : spread;
            head.pendingThrow = dir.yRot((float) Math.toRadians(turn)).add(0.0, i == 0 ? 0.0 : -0.05, 0.0).normalize();
            head.throwDelay = i * delay;
        }
    }

    public static boolean spin(State s, Player player) {
        if (s.spinning || s.cooldown > 0 || s.windup > 0 || s.pendingAny() || s.throwTicks > 0 || s.volleying()) {
            return false;
        }
        s.spinning = true;
        s.spinElapsed = 0;
        s.launched = false;
        s.struck.clear();
        return true;
    }

    public static Vec3 whirlPoint(int head, double ticks, Vec3 anchor, float bodyYaw) {
        double yaw = Math.toRadians(bodyYaw);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        double turn = 2.0 * Math.PI * Math.max(0.0, ticks - WHIRL_START_TICKS) / WHIRL_TURN_TICKS
                + head * Math.PI;
        return anchor.add(forward.scale(Math.sin(turn) * SPIN_REACH)).add(0.0, Math.cos(turn) * SPIN_REACH, 0.0);
    }

    public static void step(State s, Vec3 anchor, Player owner) {
        Level level = owner.level();
        s.lastAnchor = anchor;
        if (s.cooldown > 0) {
            s.cooldown--;
        }
        if (s.windup > 0 && --s.windup == 0) {
            throwAt(s, owner, anchor);
        }
        s.volleyStrike = -1;
        if (s.volleying()) {
            Entity target = level.getEntity(s.volleyTarget);
            if (target == null || !target.isAlive() || target.getBoundingBox().getCenter().distanceTo(anchor) > VOLLEY_BREAK) {
                endVolley(s, anchor);
            } else {
                stepVolley(s, anchor, target, owner, level);
                return;
            }
        }
        if (s.spinning) {
            stepWhirl(s, anchor, owner, level);
            return;
        }
        double want = s.throwTicks > 0 || s.pendingAny() ? s.launched ? LAUNCH_REACH : REACH : REST_LENGTH;
        s.prevLength = s.length;
        s.length = want > s.length ? want : Math.max(want, s.length - REEL_PER_TICK);

        boolean launched = false;
        for (Head head : s.heads) {
            launched |= stepHead(s, head, anchor, owner, level);
        }
        separate(s, owner);
        if (launched) {
            s.throwTicks = THROW_TICKS;
        } else if (s.throwTicks > 0) {
            s.throwTicks--;
        }
        for (Head head : s.heads) {
            stepString(s, head, anchor, owner, level);
        }
    }

    private static void stepWhirl(State s, Vec3 anchor, Player owner, Level level) {
        s.spinElapsed++;
        s.prevLength = s.length;
        s.length = SPIN_REACH;
        double settle = Math.min(1.0, s.spinElapsed / (double) WHIRL_START_TICKS);
        for (int i = 0; i < HEADS; i++) {
            Head head = s.heads[i];
            head.prevPos = head.pos;
            head.pos = head.pos.lerp(whirlPoint(i, s.spinElapsed, anchor, owner.yBodyRot), settle);
            head.vel = head.pos.subtract(head.prevPos);
            head.onGround = false;
            head.groundImpact = 0.0;
        }
        if (s.spinElapsed % WHIRL_REHIT_TICKS == 0) {
            s.struck.clear();
        }
        if (s.spinElapsed >= WHIRL_LAUNCH_TICKS) {
            launch(s, owner, anchor);
        }
        for (Head head : s.heads) {
            stepString(s, head, anchor, owner, level);
        }
    }

    private static void stepVolley(State s, Vec3 anchor, Entity target, Player owner, Level level) {
        s.volleyAge++;
        s.prevLength = s.length;
        s.length = Math.max(REST_LENGTH, target.getBoundingBox().getCenter().distanceTo(anchor) + 0.5);
        for (int i = 0; i < HEADS; i++) {
            Head head = s.heads[i];
            head.prevPos = head.pos;
            head.pos = volleyPoint(i, s.volleyAge, anchor, target);
            head.vel = head.pos.subtract(head.prevPos);
            head.onGround = false;
            head.groundImpact = 0.0;
            if (strikesAt(i, s.volleyAge)) {
                s.volleyStrike = i;
            }
        }
        if (--s.volleyTicks <= 0) {
            endVolley(s, anchor);
        }
        for (Head head : s.heads) {
            stepString(s, head, anchor, owner, level);
        }
    }

    private static void endVolley(State s, Vec3 anchor) {
        s.volleyTicks = 0;
        s.volleyTarget = -1;
        s.cooldown = Math.max(s.cooldown, COOLDOWN_TICKS);
        for (Head head : s.heads) {
            head.vel = anchor.subtract(head.pos).scale(0.3);
        }
    }

    private static void separate(State s, Player owner) {
        s.contact = 0.0;
        Head a = s.heads[0];
        Head b = s.heads[1];
        Vec3 apart = a.pos.subtract(b.pos);
        double dist = apart.length();
        if (dist >= BALL_GAP) {
            return;
        }
        Vec3 normal;
        if (dist > 1.0E-4) {
            normal = apart.scale(1.0 / dist);
        } else {
            double yaw = Math.toRadians(owner.yBodyRot);
            normal = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        }
        Vec3 push = normal.scale((BALL_GAP - dist) * 0.5);
        a.pos = a.pos.add(push);
        b.pos = b.pos.subtract(push);
        double closing = a.vel.subtract(b.vel).dot(normal);
        if (closing < 0.0) {
            Vec3 impulse = normal.scale(-closing * (1.0 + BALL_BOUNCE) * 0.5);
            a.vel = a.vel.add(impulse);
            b.vel = b.vel.subtract(impulse);
            s.contact = -closing;
        }
    }

    private static boolean stepHead(State s, Head h, Vec3 anchor, Player owner, Level level) {
        h.groundImpact = 0.0;
        h.onGround = false;
        if (h.pendingThrow != null) {
            h.prevPos = h.pos;
            if (h.throwDelay > 0) {
                h.throwDelay--;
                h.pos = h.pos.add(anchor.subtract(h.pos).scale(0.5));
                h.vel = Vec3.ZERO;
                return false;
            }
            h.pos = anchor.add(h.pendingThrow.scale(0.3));
            h.vel = h.pendingThrow.scale(s.launched ? LAUNCH_SPEED : THROW_SPEED);
            h.pendingThrow = null;
            return true;
        }

        h.prevPos = h.pos;
        h.vel = h.vel.subtract(0.0, GRAVITY, 0.0);
        h.vel = new Vec3(h.vel.x * DRAG, h.vel.y * VERTICAL_DRAG, h.vel.z * DRAG);
        if (s.throwTicks > 0 && h.vel.length() > 0.8) {
            h.vel = h.vel.add(0.0, GRAVITY * 0.75, 0.0);
        }

        Vec3 next = h.pos.add(h.vel);
        if (h.vel.lengthSqr() > 1.0E-8) {
            BlockHitResult hit = level.clip(new ClipContext(h.pos, next, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, owner));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Direction face = hit.getDirection();
                Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
                next = hit.getLocation().add(normal.scale(HEAD_RADIUS));
                double into = h.vel.dot(normal);
                if (into < 0.0) {
                    if (face == Direction.UP) {
                        h.onGround = true;
                        if (h.vel.y < -0.3) {
                            h.groundImpact = -h.vel.y;
                        }
                    }
                    Vec3 slide = h.vel.subtract(normal.scale(into));
                    h.vel = slide.scale(GROUND_FRICTION);
                }
                stopOnBlock(s, h, anchor, next, -into);
            }
        }

        if (s.throwTicks > 0) {
            next = bounceOffEntities(h, owner, level, next);
        }

        BlockHitResult support = level.clip(new ClipContext(next, next.subtract(0.0, GROUND_PROBE, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        if (support.getType() == HitResult.Type.BLOCK && support.getDirection() == Direction.UP) {
            double rest = support.getLocation().y + HEAD_RADIUS;
            if (next.y < rest) {
                next = new Vec3(next.x, rest, next.z);
                h.onGround = true;
                double drop = -h.vel.y;
                if (h.vel.y < -0.3) {
                    h.groundImpact = drop;
                }
                h.vel = new Vec3(h.vel.x * GROUND_FRICTION, Math.max(h.vel.y, 0.0), h.vel.z * GROUND_FRICTION);
                stopOnBlock(s, h, anchor, next, drop);
            }
        }

        Vec3 off = next.subtract(anchor);
        double dist = off.length();
        if (dist > s.length) {
            Vec3 n = off.scale(1.0 / dist);
            next = anchor.add(n.scale(s.length));
            double radialVel = h.vel.dot(n);
            if (radialVel > 0.0) {
                h.vel = h.vel.subtract(n.scale(radialVel));
            }
        }

        h.pos = next;
        return false;
    }

    private static void stopOnBlock(State s, Head h, Vec3 anchor, Vec3 at, double impact) {
        if (s.throwTicks <= 0 || s.spinning || impact < BLOCK_STOP_IMPACT) {
            return;
        }
        s.throwTicks = 0;
        Vec3 back = anchor.subtract(at);
        h.vel = back.lengthSqr() < 1.0E-8 ? Vec3.ZERO : back.normalize().scale(BLOCK_REEL_SPEED);
    }

    private static Vec3 bounceOffEntities(Head h, Player owner, Level level, Vec3 next) {
        AABB search = new AABB(h.pos, next).inflate(HEAD_RADIUS + 0.5);
        AABB struck = null;
        Vec3 contact = null;
        double closest = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, search,
                e -> Clackers.hittable(owner, e))) {
            AABB box = candidate.getBoundingBox().inflate(HEAD_RADIUS);
            Vec3 point;
            Optional<Vec3> entry = box.clip(h.pos, next);
            if (entry.isPresent()) {
                point = entry.get();
            } else if (box.contains(next)) {
                point = next;
            } else {
                continue;
            }
            double dist = h.pos.distanceToSqr(point);
            if (dist < closest) {
                closest = dist;
                struck = box;
                contact = point;
            }
        }
        if (struck == null) {
            return next;
        }
        Vec3 offset = contact.subtract(struck.getCenter());
        double ax = Math.abs(offset.x) / Math.max(struck.getXsize() * 0.5, 1.0E-4);
        double ay = Math.abs(offset.y) / Math.max(struck.getYsize() * 0.5, 1.0E-4);
        double az = Math.abs(offset.z) / Math.max(struck.getZsize() * 0.5, 1.0E-4);
        Vec3 normal;
        if (ax >= ay && ax >= az) {
            normal = new Vec3(offset.x < 0.0 ? -1.0 : 1.0, 0.0, 0.0);
        } else if (az >= ax && az >= ay) {
            normal = new Vec3(0.0, 0.0, offset.z < 0.0 ? -1.0 : 1.0);
        } else {
            normal = new Vec3(0.0, offset.y < 0.0 ? -1.0 : 1.0, 0.0);
        }
        double into = h.vel.dot(normal);
        if (into < 0.0) {
            Vec3 tangential = h.vel.subtract(normal.scale(into));
            h.vel = tangential.scale(ENTITY_FRICTION).add(normal.scale(-into * ENTITY_RESTITUTION));
        }
        return contact.add(normal.scale(HEAD_RADIUS * 0.5));
    }

    private static void stepString(State s, Head h, Vec3 anchor, Player owner, Level level) {
        if (!level.isClientSide) {
            return;
        }
        int last = STRING_NODES - 1;
        if (!h.stringReady) {
            for (int i = 0; i <= last; i++) {
                Vec3 p = anchor.add(h.pos.subtract(anchor).scale(i / (double) last));
                h.string[i] = p;
                h.stringPrev[i] = p;
            }
            h.stringReady = true;
        }
        for (int i = 1; i < last; i++) {
            Vec3 current = h.string[i];
            Vec3 velocity = current.subtract(h.stringPrev[i]).scale(STRING_DAMPING);
            h.stringPrev[i] = current;
            h.string[i] = current.add(velocity).subtract(0.0, STRING_GRAVITY, 0.0);
        }
        h.stringPrev[0] = h.string[0];
        h.string[0] = anchor;
        h.stringPrev[last] = h.string[last];
        h.string[last] = h.pos;

        double spacing = Math.max(h.pos.distanceTo(anchor), s.length) / last;
        relax(h, anchor, spacing, STRING_ITERATIONS);
        for (int i = 1; i < last; i++) {
            h.string[i] = resolve(level, owner, h.stringPrev[i], h.string[i]);
        }
        relax(h, anchor, spacing, 2);

        if (!h.onGround) {
            double extension = h.pos.distanceTo(anchor) / Math.max(s.length, 1.0E-3);
            double tension = Mth.clamp((extension - TAUT_START) / (1.0 - TAUT_START), 0.0, 1.0);
            if (tension > 0.0) {
                Vec3 span = h.pos.subtract(anchor);
                for (int i = 1; i < last; i++) {
                    Vec3 straight = anchor.add(span.scale(i / (double) last));
                    h.string[i] = h.string[i].add(straight.subtract(h.string[i]).scale(tension * TAUT_BLEND));
                }
            }
        }
    }

    private static void relax(Head h, Vec3 anchor, double spacing, int iterations) {
        int last = STRING_NODES - 1;
        for (int iter = 0; iter < iterations; iter++) {
            h.string[0] = anchor;
            h.string[last] = h.pos;
            for (int i = 0; i < last; i++) {
                Vec3 a = h.string[i];
                Vec3 b = h.string[i + 1];
                Vec3 delta = b.subtract(a);
                double dist = delta.length();
                if (dist < 1.0E-6) {
                    continue;
                }
                double error = (dist - spacing) / dist;
                boolean pinnedA = i == 0;
                boolean pinnedB = i + 1 == last;
                if (pinnedA && pinnedB) {
                    continue;
                }
                if (pinnedA) {
                    h.string[i + 1] = b.subtract(delta.scale(error));
                } else if (pinnedB) {
                    h.string[i] = a.add(delta.scale(error));
                } else {
                    Vec3 shift = delta.scale(error * 0.5);
                    h.string[i] = a.add(shift);
                    h.string[i + 1] = b.subtract(shift);
                }
            }
        }
    }

    private static Vec3 resolve(Level level, Player owner, Vec3 from, Vec3 to) {
        if (from.distanceToSqr(to) > 1.0E-8) {
            BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, owner));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Direction face = hit.getDirection();
                to = hit.getLocation().add(new Vec3(face.getStepX(), face.getStepY(), face.getStepZ())
                        .scale(STRING_NODE_RADIUS));
            }
        }
        return to;
    }
}
