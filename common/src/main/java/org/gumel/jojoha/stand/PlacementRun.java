package org.gumel.jojoha.stand;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class PlacementRun {
    public static final int MAX_CELLS = 24;

    public static final int MAX_VOLUME_CELLS = 128;

    private static final double UP_IS_VERTICAL = 0.5;

    public static final Vec3 CENTRE = new Vec3(0.5, 0.5, 0.5);

    private PlacementRun() {
    }

    public static Vec3 grip(BlockHitResult hit, BlockPos cell) {
        Vec3 at = hit.getLocation().subtract(Vec3.atLowerCornerOf(cell));
        return new Vec3(Mth.clamp(at.x, 0.0, 1.0), Mth.clamp(at.y, 0.0, 1.0), Mth.clamp(at.z, 0.0, 1.0));
    }

    public static BlockHitResult hitAt(BlockPos cell, Direction face, Vec3 grip) {
        return new BlockHitResult(Vec3.atLowerCornerOf(cell).add(grip), face, cell, false);
    }

    private static Direction.AxisDirection sign(Direction.Axis axis, int dx, int dy, int dz) {
        int delta = switch (axis) {
            case X -> dx;
            case Y -> dy;
            case Z -> dz;
        };
        return delta < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE;
    }

    public static BlockPos farEnd(BlockPos anchor, Vec3 eye, Vec3 look, double reach, BuildMode mode) {
        Vec3 end = eye.add(look.scale(reach));

        Direction.Axis chosen = mode.axis(Mth.floor(end.x) - anchor.getX(),
                Mth.floor(end.y) - anchor.getY(), Mth.floor(end.z) - anchor.getZ());
        if (chosen != null) {
            return switch (chosen) {
                case X -> new BlockPos(Mth.floor(end.x), anchor.getY(), anchor.getZ());
                case Y -> new BlockPos(anchor.getX(), Mth.floor(end.y), anchor.getZ());
                case Z -> new BlockPos(anchor.getX(), anchor.getY(), Mth.floor(end.z));
            };
        }

        double ax = Math.abs(look.x);
        double ay = Math.abs(look.y);
        double az = Math.abs(look.z);

        if (ay >= UP_IS_VERTICAL) {
            return new BlockPos(anchor.getX(), Mth.floor(end.y), anchor.getZ());
        }

        if (ay >= ax && ay >= az) {
            return new BlockPos(anchor.getX(), Mth.floor(end.y), anchor.getZ());
        }
        if (ax >= az) {
            return new BlockPos(Mth.floor(end.x), anchor.getY(), anchor.getZ());
        }
        return new BlockPos(anchor.getX(), anchor.getY(), Mth.floor(end.z));
    }

    public static List<BlockPos> box(BlockPos a, BlockPos b) {
        List<BlockPos> cells = new ArrayList<>();
        for (int y = Math.min(a.getY(), b.getY()); y <= Math.max(a.getY(), b.getY()); y++) {
            for (int x = Math.min(a.getX(), b.getX()); x <= Math.max(a.getX(), b.getX()); x++) {
                for (int z = Math.min(a.getZ(), b.getZ()); z <= Math.max(a.getZ(), b.getZ()); z++) {
                    if (cells.size() >= MAX_VOLUME_CELLS) {
                        return cells;
                    }
                    cells.add(new BlockPos(x, y, z));
                }
            }
        }
        return cells;
    }

    public static List<BlockPos> between(BlockPos anchor, BlockPos aim, BuildMode mode) {
        List<BlockPos> cells = new ArrayList<>();
        cells.add(anchor);

        int dx = aim.getX() - anchor.getX();
        int dy = aim.getY() - anchor.getY();
        int dz = aim.getZ() - anchor.getZ();

        int span = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
        if (span == 0) {
            return cells;
        }

        Direction.Axis fixed = mode.axis(dx, dy, dz);
        Direction along = fixed == null
                ? Direction.getNearest(dx, dy, dz)
                : Direction.fromAxisAndDirection(fixed, sign(fixed, dx, dy, dz));

        if (fixed != null) {
            span = Math.abs(switch (fixed) {
                case X -> dx;
                case Y -> dy;
                case Z -> dz;
            });
            if (span == 0) {
                return cells;
            }
        }

        int length = Math.min(span, MAX_CELLS - 1);

        for (int step = 1; step <= length; step++) {
            cells.add(anchor.relative(along, step));
        }
        return cells;
    }
}
