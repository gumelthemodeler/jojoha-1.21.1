package org.gumel.jojoha.stand.grapple;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class GrappleAim {
    public static final double RANGE = 15.0;

    private static final int RINGS = 6;
    private static final double RING_DEGREES = 5.0;
    private static final int PER_RING = 12;

    private static final int CLIMB_LIMIT = 12;

    private static final double MIN_HEIGHT = 3.0;

    private static final double PERCH_LIFT = 0.35;

    private GrappleAim() {
    }

    public static BlockHitResult find(Player player) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        BlockHitResult straight = cast(level, player, eye, look);
        if (straight != null) {
            return straight;
        }

        Vec3 seed = Math.abs(look.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = look.cross(seed).normalize();
        Vec3 up = right.cross(look).normalize();

        for (int ring = 1; ring <= RINGS; ring++) {
            double angle = Math.toRadians(ring * RING_DEGREES);
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);

            for (int step = 0; step < PER_RING; step++) {
                double around = step * (Math.PI * 2 / PER_RING);

                Vec3 direction = look.scale(cos)
                        .add(right.scale(sin * Math.cos(around)))
                        .add(up.scale(sin * Math.sin(around)));

                BlockHitResult hit = cast(level, player, eye, direction.normalize());
                if (hit != null) {
                    return hit;
                }
            }
        }
        return null;
    }

    public static Vec3 perch(Level level, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();

        if (hit.getDirection() == Direction.DOWN) {
            return hit.getLocation();
        }

        int climbed = 0;
        while (climbed < CLIMB_LIMIT && !standingRoom(level, pos)) {
            pos = pos.above();
            climbed++;
        }

        return new Vec3(pos.getX() + 0.5, pos.getY() + PERCH_LIFT, pos.getZ() + 0.5);
    }

    private static boolean standingRoom(Level level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    private static BlockHitResult cast(Level level, Player player, Vec3 eye, Vec3 direction) {
        HitResult hit = level.clip(new ClipContext(eye, eye.add(direction.scale(RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult block)) {
            return null;
        }

        return block.getLocation().y - player.getY() >= MIN_HEIGHT ? block : null;
    }
}
