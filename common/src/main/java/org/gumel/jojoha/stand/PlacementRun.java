package org.gumel.jojoha.stand;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The straight run of cells between where a stretch started and where it is being pulled to.
 *
 * <p>Shared rather than duplicated, and that is the whole reason it is a class. The client draws
 * this run and the server places it, from two different traces taken a tick or so apart - so the
 * only way the ghost boxes can be trusted to be the blocks you get is for both sides to be running
 * the same function over the same two corners. Anything computed twice will eventually be computed
 * differently.
 *
 * <p>Axis-aligned on the dominant delta rather than a true line between the corners. A Bresenham
 * diagonal is what a drawing tool wants; a builder wants a row, and a row that wandered off the
 * axis because the aim drifted two blocks sideways would be worse than useless - you would have to
 * undo it. Picking one axis means the run only ever does the thing the player can predict.
 */
public final class PlacementRun {
    /**
     * The longest run a single click will lay.
     *
     * <p>A ceiling on a mistake as much as on a feature. The Stand visits every cell in turn, so a
     * hundred-block run is a hundred blocks of a Stand flying about and a hundred items gone before
     * the player can react to the first one being wrong.
     */
    public static final int MAX_CELLS = 24;

    /**
     * How far up or down counts as "vertical" regardless of the horizontal aim, as |sin(pitch)|.
     *
     * <p>Half is thirty degrees off the horizon.
     */
    private static final double UP_IS_VERTICAL = 0.5;

    private PlacementRun() {
    }

    /** Which way along a fixed axis the drag is going; positive when it has not moved at all. */
    private static Direction.AxisDirection sign(Direction.Axis axis, int dx, int dy, int dz) {
        int delta = switch (axis) {
            case X -> dx;
            case Y -> dy;
            case Z -> dz;
        };
        return delta < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE;
    }

    /**
     * Where a stretch reaches when the crosshair has nothing to land on.
     *
     * <p>Aiming up is aiming at sky, and sky is not a block - so the ordinary path finds no hit,
     * draws nothing and places nothing. That is exactly wrong for the one thing a builder most
     * wants to do without scaffolding, which is run a pillar up from where they are standing.
     *
     * <p>So the far end is taken from the direction rather than from a collision: the run leaves
     * the anchor along whichever axis the player is most nearly looking down, and goes as far along
     * it as the ray would have. Looking further up builds higher, which is the relationship the
     * hand expects. Snapping to one axis is what keeps it a pillar and not a diagonal - the same
     * reason {@link #between} does.
     */
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

        // Vertical wins outright once the aim is properly up or down, rather than only when the Y
        // component happens to be the largest. Compared bare, the axis flips at forty-five degrees
        // - so a player looking up at forty and expecting a pillar silently got a horizontal beam,
        // with nothing on screen to explain why. Half is thirty degrees, which is unambiguously
        // "looking up", and running a row horizontally while craning your neck is not a thing
        // anyone does by accident.
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

    /**
     * Every cell from the anchor to the aim, inclusive of both ends.
     *
     * <p>Returns just the anchor when the two coincide, so a stretch that has not been pulled
     * anywhere yet is a single block - the same thing an ordinary click would place, which is what
     * makes starting a stretch free rather than a mode you have to commit to.
     */
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

        // The chosen shape wins over the drag. This is the whole point of choosing one: a row stays
        // a row when the crosshair strays upward, instead of becoming a column at the moment the
        // player stops watching their pitch.
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
