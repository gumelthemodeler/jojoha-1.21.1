package org.gumel.jojoha.combat;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WallWalk {
    public static final double SPEED = 0.2;
    private static final double BASE_SPEED = 0.1;

    private static final double REACH = 0.06;
    private static final double REMOTE_REACH = 0.25;
    private static final double SERVER_REACH = 0.3;
    private static final double TRIM = 0.05;

    private static final double STICK = 0.04;
    private static final double HOP_UP = 0.42;
    private static final double HOP_OVER = 0.2;
    private static final double LEAP_OFF = 0.45;
    private static final double LEAP_UP = 0.3;
    private static final int LET_GO_TICKS = 8;

    private static final double FEET_TO_WALL = 0.3;
    private static final double BODY_LIFT = 0.9;

    private static final Map<UUID, Direction> SIDE = new HashMap<>();
    private static final Set<UUID> ON = new HashSet<>();
    private static final Set<UUID> CLIMBING_UP = new HashSet<>();
    private static final Map<UUID, Integer> LET_GO = new HashMap<>();

    private WallWalk() {
    }

    public static boolean travel(Player player, boolean jumping) {
        if (!player.level().isClientSide() || !player.isLocalPlayer()) {
            return false;
        }
        UUID id = player.getUUID();
        if (!Climbing.wallCrawling(player) || player.isSpectator() || player.getAbilities().flying
                || player.isPassenger() || player.isInWater() || player.isInLava() || player.isFallFlying()) {
            leave(id);
            LET_GO.remove(id);
            return false;
        }
        Integer letGo = LET_GO.get(id);
        if (letGo != null) {
            if (letGo <= 1) {
                LET_GO.remove(id);
            } else {
                LET_GO.put(id, letGo - 1);
            }
            return false;
        }

        boolean wasOn = ON.contains(id);
        Direction side = wall(player, REACH, SIDE.get(id));
        if (side == null) {
            Direction last = SIDE.get(id);
            if (wasOn && last != null && CLIMBING_UP.contains(id)) {
                player.setDeltaMovement(last.getStepX() * HOP_OVER, HOP_UP, last.getStepZ() * HOP_OVER);
            }
            leave(id);
            return false;
        }

        Vec3 out = new Vec3(-side.getStepX(), 0.0, -side.getStepZ());
        if (jumping && wasOn) {
            player.setDeltaMovement(out.x * LEAP_OFF, LEAP_UP, out.z * LEAP_OFF);
            leave(id);
            LET_GO.put(id, LET_GO_TICKS);
            return false;
        }

        Vec3 look = player.getLookAngle();
        double into = Math.max(0.0, -look.dot(out));
        Vec3 ahead = look.subtract(out.scale(look.dot(out))).add(0.0, into, 0.0);
        ahead = ahead.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : ahead.normalize();
        Vec3 left = out.cross(ahead);

        Vec3 move = ahead.scale(player.zza).add(left.scale(player.xxa));
        if (move.lengthSqr() > 1.0) {
            move = move.normalize();
        }
        move = move.scale(SPEED * player.getAttributeValue(Attributes.MOVEMENT_SPEED) / BASE_SPEED);

        if (player.onGround() && !(player.zza > 0F && move.y > 1.0E-3)) {
            leave(id);
            return false;
        }

        SIDE.put(id, side);
        ON.add(id);
        if (move.y > 0.02) {
            CLIMBING_UP.add(id);
        } else {
            CLIMBING_UP.remove(id);
        }

        player.move(MoverType.SELF, move.subtract(out.scale(STICK)));
        player.setDeltaMovement(move);
        player.resetFallDistance();
        player.calculateEntityAnimation(true);
        return true;
    }

    private static void leave(UUID id) {
        ON.remove(id);
        CLIMBING_UP.remove(id);
        SIDE.remove(id);
    }

    public static boolean touching(Player player) {
        return Climbing.wallCrawling(player) && !player.onGround() && wall(player, SERVER_REACH, null) != null;
    }

    public static Direction renderSide(Player player) {
        if (!Climbing.wallCrawling(player)) {
            return null;
        }
        if (player.isLocalPlayer()) {
            return ON.contains(player.getUUID()) ? SIDE.get(player.getUUID()) : null;
        }
        return player.onGround() ? null : wall(player, REMOTE_REACH, null);
    }

    public static Vec3 renderLift(Direction side) {
        return new Vec3(side.getStepX() * FEET_TO_WALL, BODY_LIFT, side.getStepZ() * FEET_TO_WALL);
    }

    private static Direction wall(Player player, double reach, Direction kept) {
        AABB box = player.getBoundingBox();
        AABB trimmed = new AABB(box.minX, box.minY + TRIM, box.minZ, box.maxX, box.maxY - TRIM, box.maxZ);
        Vec3 look = player.getLookAngle();
        Direction best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            AABB probe = trimmed.move(direction.getStepX() * reach, 0.0, direction.getStepZ() * reach);
            if (player.level().noCollision(player, probe)) {
                continue;
            }
            double score = direction.getStepX() * look.x + direction.getStepZ() * look.z
                    + (direction == kept ? 2.0 : 0.0);
            if (score > bestScore) {
                bestScore = score;
                best = direction;
            }
        }
        return best;
    }

    public static void settleCape(Player player) {
        if (renderSide(player) == null) {
            return;
        }
        player.xCloakO = player.xo;
        player.yCloakO = player.yo;
        player.zCloakO = player.zo;
        player.xCloak = player.getX();
        player.yCloak = player.getY();
        player.zCloak = player.getZ();
    }

    public static void clear() {
        SIDE.clear();
        ON.clear();
        CLIMBING_UP.clear();
        LET_GO.clear();
    }
}
