package org.gumel.jojoha.vampire;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class RipperPath {
    public static final double REACH = 32.0;
    private static final double STEP = 0.5;

    private static final double SEEK_RADIUS = 7.0;

    private static final double TURN_PER_STEP = 6.0;
    private static final double TURN_TOTAL = 70.0;

    public static final double HIT_RADIUS = 0.8;

    private RipperPath() {
    }

    public static Result walk(ServerPlayer player, Vec3 from) {
        ServerLevel level = player.serverLevel();
        Vec3 aim = player.getLookAngle();
        Vec3 heading = aim;
        Vec3 at = from;

        List<Vec3> points = new ArrayList<>();
        List<LivingEntity> struck = new ArrayList<>();
        points.add(at);

        for (double travelled = 0; travelled < REACH; travelled += STEP) {
            LivingEntity toward = nearestAhead(level, player, at, heading);
            if (toward != null) {
                heading = steer(heading, toward.getBoundingBox().getCenter().subtract(at), aim);
            }

            at = at.add(heading.scale(STEP));
            points.add(at);

            AABB probe = new AABB(at.subtract(HIT_RADIUS, HIT_RADIUS, HIT_RADIUS),
                    at.add(HIT_RADIUS, HIT_RADIUS, HIT_RADIUS));
            for (LivingEntity caught : level.getEntitiesOfClass(LivingEntity.class, probe,
                    entity -> entity != player && entity.isAlive())) {
                if (!struck.contains(caught)) {
                    struck.add(caught);
                }
            }
        }

        return new Result(points, struck);
    }

    private static LivingEntity nearestAhead(ServerLevel level, ServerPlayer player, Vec3 at,
                                             Vec3 heading) {
        AABB around = new AABB(at.subtract(SEEK_RADIUS, SEEK_RADIUS, SEEK_RADIUS),
                at.add(SEEK_RADIUS, SEEK_RADIUS, SEEK_RADIUS));

        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;

        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, around,
                entity -> entity != player && entity.isAlive())) {
            Vec3 offset = candidate.getBoundingBox().getCenter().subtract(at);
            double distance = offset.length();
            if (distance < 1.0E-4) {
                continue;
            }

            if (offset.normalize().dot(heading) < AHEAD_DOT) {
                continue;
            }

            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static final double AHEAD_DOT = 0.35;

    private static Vec3 steer(Vec3 heading, Vec3 toward, Vec3 aim) {
        Vec3 wanted = toward.normalize();
        double share = Math.min(1.0, Math.toRadians(TURN_PER_STEP)
                / Math.max(1.0E-4, Math.acos(clamp(heading.dot(wanted)))));

        Vec3 turned = heading.add(wanted.subtract(heading).scale(share)).normalize();

        if (Math.toDegrees(Math.acos(clamp(turned.dot(aim)))) > TURN_TOTAL) {
            return heading;
        }
        return turned;
    }

    private static double clamp(double dot) {
        return Math.max(-1.0, Math.min(1.0, dot));
    }

    public record Result(List<Vec3> points, List<LivingEntity> struck) {
    }
}
