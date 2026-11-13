package org.gumel.jojoha.combat;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class ScarfPhysics {
    public static final int NODES = 32;
    public static final double REST_LENGTH = 1.0;
    public static final double REACH = 7.0;
    public static final double NECK_SHARE = 0.73;
    public static final double BACK_OFFSET = 0.21;
    public static final double BODY_HALF_WIDTH = 0.36;
    public static final double BODY_HALF_DEPTH = 0.15;
    public static final double LUNGE_SPEED = 1.1;
    public static final int MIN_REACH_TICKS = 5;
    public static final int MAX_REACH_TICKS = 10;
    public static final int HOLD_TICKS = 3;
    public static final int RETRACT_TICKS = 7;
    public static final double WIGGLE = 0.35;
    public static final double WIGGLE_WAVES = 1.5;
    public static final double SIDE_OUT = 0.8;
    public static final double SIDE_BACK = 0.3;
    public static final double SIDE_FRONT = 1.4;
    public static final double SHOULDER = 1.25;
    public static final double SLACK = 1.03;
    public static final double GUIDE = 1.0;
    public static final double GUIDE_START = 0.12;
    public static final int GUIDE_POINTS = 24;

    private static final double GRAVITY = 0.035;
    private static final double DAMPING = 0.9;
    private static final double CLING = 0.012;
    private static final double REEL = 0.6;
    private static final double KEEP_SWING = 0.9;
    private static final int ITERATIONS = 8;
    private static final double NODE_RADIUS = 0.05;

    private ScarfPhysics() {
    }

    public static final class State {
        public final Vec3[] nodes = new Vec3[NODES];
        public final Vec3[] prev = new Vec3[NODES];
        public final Vec3[] shown = new Vec3[NODES];
        public boolean ready;
        public double length = REST_LENGTH;
        public int target = -1;
        public int age;
        public int reachTicks;
        public double side = 1.0;
        public Vec3 tip;
        public Vec3 contact;
        public Vec3 lastAnchor;
        public Vec3 end;
        public double progress;
        public boolean wavy;
        public boolean dropped;
        public boolean hitNow;

        public boolean striking() {
            return target >= 0;
        }
    }

    public static Vec3 forward(Player player, float partialTick) {
        double yaw = Math.toRadians(Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot));
        return new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
    }

    public static Vec3 right(Vec3 forward) {
        return new Vec3(-forward.z, 0.0, forward.x);
    }

    public static Vec3 anchor(Player player, float partialTick) {
        double yaw = Math.toRadians(Mth.lerp(partialTick, player.yHeadRotO, player.yHeadRot));
        Vec3 facing = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        double height = player.getBoundingBox().getYsize() * NECK_SHARE;
        return player.getPosition(partialTick).add(0.0, height, 0.0).subtract(facing.scale(BACK_OFFSET));
    }

    public static Vec3 restTip(Vec3 anchor) {
        return anchor.subtract(0.0, REST_LENGTH, 0.0);
    }

    public static void begin(State s, Player player, Entity target) {
        s.target = target.getId();
        s.age = 0;
        s.dropped = false;
        s.hitNow = false;
        s.contact = null;
        s.side = player.getMainArm() == HumanoidArm.RIGHT ? 1.0 : -1.0;
        Vec3 anchor = anchor(player, 1F);
        Vec3[] path = path(s, player, anchor, aimPoint(s, player, target));
        double length = path[0].distanceTo(path[1]) + path[1].distanceTo(path[2]) + path[2].distanceTo(path[3]);
        s.reachTicks = Mth.clamp((int) Math.ceil(length / LUNGE_SPEED), MIN_REACH_TICKS, MAX_REACH_TICKS);
    }

    public static Vec3 shoulderPoint(State s, Player player) {
        Vec3 forward = forward(player, 1F);
        return player.position().add(0.0, SHOULDER * player.getBoundingBox().getYsize() / 1.8, 0.0)
                .add(right(forward).scale(s.side * SIDE_OUT));
    }

    public static Vec3[] path(State s, Player player, Vec3 anchor, Vec3 end) {
        Vec3 forward = forward(player, 1F);
        Vec3 shoulder = shoulderPoint(s, player);
        return new Vec3[]{
                restTip(anchor),
                shoulder.subtract(forward.scale(SIDE_BACK)),
                shoulder.add(forward.scale(SIDE_FRONT)),
                end};
    }

    public static Vec3 aimPoint(State s, Player player, Entity target) {
        Vec3 centre = target.getBoundingBox().getCenter();
        Vec3 toward = centre.subtract(shoulderPoint(s, player));
        if (toward.lengthSqr() < 1.0E-6) {
            return centre;
        }
        return centre.subtract(toward.normalize().scale(target.getBbWidth() * 0.45));
    }

    public static Vec3 along(Vec3[] p, double t) {
        double u = 1.0 - t;
        return p[0].scale(u * u * u).add(p[1].scale(3.0 * u * u * t)).add(p[2].scale(3.0 * u * t * t))
                .add(p[3].scale(t * t * t));
    }

    public static Vec3 snake(Vec3[] p, double t) {
        double wave = Math.sin(Math.PI * t) * Math.sin(2.0 * Math.PI * WIGGLE_WAVES * t) * WIGGLE;
        return along(p, t).add(0.0, wave, 0.0);
    }

    public static double lunge(double share) {
        return share * (0.6 + 0.4 * share);
    }

    public static void step(State s, Player player, Level level, Vec3 anchor) {
        s.lastAnchor = anchor;
        s.hitNow = false;
        if (s.striking()) {
            s.age++;
            Entity target = level.getEntity(s.target);
            boolean holding = target != null && target.isAlive() && target.distanceTo(player) <= REACH + 2.0;
            int retractFrom = s.reachTicks + HOLD_TICKS;
            if (!holding && s.age <= retractFrom) {
                s.contact = s.tip != null ? s.tip : restTip(anchor);
                s.dropped = true;
                s.age = retractFrom + 1;
            }
            if (s.age <= s.reachTicks) {
                s.end = aimPoint(s, player, target);
                s.progress = lunge(s.age / (double) s.reachTicks);
                s.wavy = true;
                s.tip = snake(path(s, player, anchor, s.end), s.progress);
                if (s.age == s.reachTicks) {
                    s.contact = s.tip;
                    s.hitNow = !s.dropped;
                }
            } else if (s.age <= retractFrom) {
                s.end = aimPoint(s, player, target);
                s.progress = 1.0;
                s.wavy = true;
                s.tip = s.end;
                s.contact = s.tip;
            } else if (s.age <= retractFrom + RETRACT_TICKS) {
                double u = (s.age - retractFrom) / (double) RETRACT_TICKS;
                s.end = s.contact;
                s.progress = (1.0 - u) * (1.0 - u);
                s.wavy = false;
                s.tip = along(path(s, player, anchor, s.end), s.progress);
            } else {
                s.target = -1;
                s.tip = null;
                s.end = null;
                s.contact = null;
                s.dropped = false;
            }
        }
        if (level.isClientSide) {
            stepRope(s, anchor, player, level);
        }
    }

    public static List<Vec3> guide(State s, Player player, Vec3 anchor) {
        List<Vec3> points = new ArrayList<>(GUIDE_POINTS + 2);
        points.add(anchor);
        if (s.tip == null || s.end == null) {
            return points;
        }
        Vec3[] p = path(s, player, anchor, s.end);
        double from = Math.min(GUIDE_START, s.progress);
        for (int i = 0; i < GUIDE_POINTS; i++) {
            double t = from + (s.progress - from) * i / GUIDE_POINTS;
            points.add(s.wavy ? snake(p, t) : along(p, t));
        }
        points.add(s.tip);
        return points;
    }

    public static double lengthOf(List<Vec3> points) {
        double total = 0.0;
        for (int i = 1; i < points.size(); i++) {
            total += points.get(i).distanceTo(points.get(i - 1));
        }
        return total;
    }

    public static Vec3 pointAt(List<Vec3> points, double distance) {
        double walked = 0.0;
        for (int i = 1; i < points.size(); i++) {
            Vec3 a = points.get(i - 1);
            Vec3 b = points.get(i);
            double piece = a.distanceTo(b);
            if (walked + piece >= distance && piece > 1.0E-9) {
                return a.add(b.subtract(a).scale((distance - walked) / piece));
            }
            walked += piece;
        }
        return points.get(points.size() - 1);
    }

    private static void stepRope(State s, Vec3 anchor, Player player, Level level) {
        int last = NODES - 1;
        List<Vec3> guide = s.tip == null ? null : guide(s, player, anchor);
        double guideLength = guide == null ? 0.0 : lengthOf(guide);
        double want = guide == null ? REST_LENGTH : Math.max(REST_LENGTH, guideLength * SLACK);
        s.length = want >= s.length || guide != null ? want : Math.max(want, s.length - REEL);
        if (!s.ready) {
            for (int i = 0; i <= last; i++) {
                Vec3 p = anchor.subtract(0.0, s.length * i / last, 0.0);
                s.nodes[i] = p;
                s.prev[i] = p;
            }
            s.ready = true;
        }
        System.arraycopy(s.nodes, 0, s.shown, 0, NODES);

        Vec3 axis = player.position();
        Vec3 forward = forward(player, 1F);
        double bottom = axis.y - 0.05;
        double top = anchor.y + 0.05;
        double spacing = s.length / last;
        boolean free = guide == null;

        for (int i = 1; i <= last; i++) {
            Vec3 current = s.nodes[i];
            Vec3 velocity = current.subtract(s.prev[i]).scale(DAMPING);
            s.prev[i] = current;
            Vec3 next = current.add(velocity).subtract(0.0, GRAVITY, 0.0);
            if (free) {
                next = next.add(new Vec3(axis.x - next.x, 0.0, axis.z - next.z).scale(CLING));
            } else {
                Vec3 lead = pointAt(guide, guideLength * i / last);
                next = next.add(lead.subtract(next).scale(GUIDE));
            }
            s.nodes[i] = next;
        }
        s.prev[0] = s.nodes[0];
        s.nodes[0] = anchor;

        if (free) {
            Vec3[] pulled = new Vec3[NODES];
            for (int i = 1; i <= last; i++) {
                Vec3 followed = follow(s.nodes[i - 1], s.nodes[i], spacing);
                pulled[i] = followed.subtract(s.nodes[i]);
                s.nodes[i] = outsideBody(followed, axis, forward, bottom, top);
                s.nodes[i] = resolve(level, player, s.shown[i], s.nodes[i]);
            }
            for (int i = 1; i < last; i++) {
                s.prev[i] = s.prev[i].add(pulled[i + 1].scale(KEEP_SWING));
            }
            return;
        }

        s.tip = outsideBody(s.tip, axis, forward, bottom, top);
        s.nodes[last] = s.tip;
        for (int iter = 0; iter < ITERATIONS; iter++) {
            relax(s, anchor, spacing);
            for (int i = 1; i < last; i++) {
                s.nodes[i] = outsideBody(s.nodes[i], axis, forward, bottom, top);
            }
            for (int i = 0; i < last; i++) {
                Vec3 middle = s.nodes[i].add(s.nodes[i + 1]).scale(0.5);
                Vec3 pushed = outsideBody(middle, axis, forward, bottom, top);
                Vec3 shift = pushed.subtract(middle);
                if (shift.lengthSqr() < 1.0E-10) {
                    continue;
                }
                if (i > 0) {
                    s.nodes[i] = s.nodes[i].add(shift);
                }
                if (i + 1 < last) {
                    s.nodes[i + 1] = s.nodes[i + 1].add(shift);
                }
            }
        }
        for (int i = 1; i < last; i++) {
            s.nodes[i] = resolve(level, player, s.shown[i], s.nodes[i]);
        }
        relax(s, anchor, spacing);
    }

    private static Vec3 follow(Vec3 leader, Vec3 node, double spacing) {
        Vec3 away = node.subtract(leader);
        double dist = away.length();
        if (dist < 1.0E-9) {
            return leader.subtract(0.0, spacing, 0.0);
        }
        return leader.add(away.scale(spacing / dist));
    }

    private static void relax(State s, Vec3 anchor, double spacing) {
        int last = NODES - 1;
        s.nodes[0] = anchor;
        s.nodes[last] = s.tip;
        for (int i = 0; i < last; i++) {
            Vec3 a = s.nodes[i];
            Vec3 b = s.nodes[i + 1];
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
                s.nodes[i + 1] = b.subtract(delta.scale(error));
            } else if (pinnedB) {
                s.nodes[i] = a.add(delta.scale(error));
            } else {
                Vec3 shift = delta.scale(error * 0.5);
                s.nodes[i] = a.add(shift);
                s.nodes[i + 1] = b.subtract(shift);
            }
        }
    }

    public static Vec3 outsideBody(Vec3 p, Vec3 axis, Vec3 forward, double bottom, double top) {
        if (p.y < bottom || p.y > top) {
            return p;
        }
        Vec3 right = right(forward);
        double dx = p.x - axis.x;
        double dz = p.z - axis.z;
        double side = dx * right.x + dz * right.z;
        double front = dx * forward.x + dz * forward.z;
        double reach = (side * side) / (BODY_HALF_WIDTH * BODY_HALF_WIDTH)
                + (front * front) / (BODY_HALF_DEPTH * BODY_HALF_DEPTH);
        if (reach >= 1.0) {
            return p;
        }
        if (reach < 1.0E-6) {
            front = -BODY_HALF_DEPTH;
            side = 0.0;
        } else {
            double scale = 1.0 / Math.sqrt(reach);
            side *= scale;
            front *= scale;
        }
        return new Vec3(axis.x + right.x * side + forward.x * front, p.y, axis.z + right.z * side + forward.z * front);
    }

    private static Vec3 resolve(Level level, Player owner, Vec3 from, Vec3 to) {
        if (from.distanceToSqr(to) > 1.0E-8) {
            BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, owner));
            if (hit.getType() == HitResult.Type.BLOCK) {
                Direction face = hit.getDirection();
                to = hit.getLocation().add(new Vec3(face.getStepX(), face.getStepY(), face.getStepZ())
                        .scale(NODE_RADIUS));
            }
        }
        return to;
    }
}
