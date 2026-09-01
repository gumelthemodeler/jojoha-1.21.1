package org.gumel.jojoha.stand.grapple;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Where the vine would go, asked once and answered the same way everywhere.
 *
 * <p>Shared between the mark drawn on the block and the throw itself, deliberately. Two separate
 * pieces of aiming code are two chances to disagree, and the one place a player will notice the
 * disagreement is the moment they commit to a jump - the mark said that ledge, the vine took the
 * wall behind it. Same function, same answer, no gap.
 *
 * <h2>It does not miss</h2>
 *
 * <p>A grapple that misses is a grapple that drops you, and being dropped is not an interesting
 * failure - there is nothing to learn from it and nothing to do about it except throw again from
 * wherever you land. So the aim widens until it finds something rather than reporting nothing.
 *
 * <p>Widening outward from the crosshair is what keeps that honest. It is not a search for the
 * nearest block in the world, which would happily fasten to the floor behind you; it is a search
 * for the block nearest to where you were already pointing, and the first ring that finds anything
 * wins. Aim well and it takes exactly what you aimed at. Aim loosely and it takes the sensible
 * thing next to it.
 */
public final class GrappleAim {
    /** How far the vine reaches, in blocks. */
    public static final double RANGE = 15.0;

    /**
     * How far off the crosshair the search is willing to go, and how finely it looks.
     *
     * <p>Thirty degrees is generous without being a lie. Past that the thing it finds is no longer
     * something the player could reasonably say they were aiming at, and a grapple that fastens to
     * something behind your shoulder is worse than one that misses.
     */
    private static final int RINGS = 6;
    private static final double RING_DEGREES = 5.0;
    private static final int PER_RING = 12;

    /** How far up a zip may climb from whatever it struck. */
    private static final int CLIMB_LIMIT = 12;

    /**
     * How far above the thrower a surface has to be before the vine will take it.
     *
     * <p>A grapple that fastens to the floor is a grapple that does nothing. Worse, it does nothing
     * expensively - it spends the energy, plays the sound, and leaves the player standing exactly
     * where they were wondering whether the move is broken.
     *
     * <p>Measured from the feet rather than the eyes, so a wall at head height still counts. Three
     * blocks is about the point where swinging from something starts to be worth doing.
     */
    private static final double MIN_HEIGHT = 3.0;

    /** How far above the surface the zip aims, so it arrives standing rather than embedded. */
    private static final double PERCH_LIFT = 0.35;

    private GrappleAim() {
    }

    /**
     * The point the vine would take hold of, or null if there is nothing worth taking.
     *
     * <p>"Nothing worth taking" now includes the ground. See MIN_HEIGHT.
     */
    public static BlockHitResult find(Player player) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        // Dead ahead first, so a deliberate aim is never overridden by the search below.
        BlockHitResult straight = cast(level, player, eye, look);
        if (straight != null) {
            return straight;
        }

        // A pair of axes across the look direction. The choice of seed only has to avoid being
        // parallel to the look, which is what the near-vertical case is guarding.
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

    /**
     * The spot on top of whatever was hit, which is where a zip actually wants to put you.
     *
     * <p>A block hit gives a point on a <em>face</em>, and for the grapple that is exactly right -
     * you hang off the face and swing. For a zip it is the wrong destination in the most annoying
     * possible way: aim at the wall of a building and the anchor is halfway up the wall, so you are
     * hauled into the brickwork and stop there. What was wanted was the roof.
     *
     * <p>So the column above the hit is walked until it runs out of blocks, and the answer is the
     * surface at the top of it. Aim at the base of a five-block wall and you are taken to the top of
     * the wall, which is what "zip up there" means to anyone throwing it.
     *
     * <p>Started from the block the hit reports rather than from the point it reports, and that is
     * the whole of what was wrong with the first version. The hit location lies exactly on the
     * boundary between the block and the air outside it, so rounding it to a block position is a
     * coin toss - and the earlier attempt to bias it inward only nudged horizontally, which does
     * nothing at all for a face pointing straight up or down.
     *
     * <p>Aiming up at a high block is therefore the case it failed hardest on. The ray strikes the
     * underside, the horizontal nudge is zero, the rounding drops into the empty block below, the
     * climb finds nothing solid to walk through and stops immediately - and the perch comes back
     * just beneath the ceiling you were aiming at. The vines fly to a point inside the floor above,
     * and the pull presses you into it, which from the ground looks exactly like nothing happening.
     *
     * <p>{@code getBlockPos} is the block that was actually struck, on any face, with no rounding
     * involved. Climbing from there needs no bias and has no orientation to get wrong.
     */
    public static Vec3 perch(Level level, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();

        // Not through a ceiling. Climbing is right for a wall - you are beside it and fly up the
        // outside - and wrong for something overhead, where the top of the column is on the far
        // side of solid matter. Striking the underside means exactly that, and the perch it used to
        // return was a point the vines could only reach by passing through the block: the pull
        // drove the player into it and held them there until the timeout, which is the "stuck for
        // some seconds" testers hit whenever they zipped upward. Hanging under the face is both
        // reachable and what aiming at a ceiling asks for.
        if (hit.getDirection() == net.minecraft.core.Direction.DOWN) {
            return hit.getLocation();
        }

        // Up through anything solid, so a wall is climbed to its parapet rather than to its brick.
        int climbed = 0;
        while (climbed < CLIMB_LIMIT && !standingRoom(level, pos)) {
            pos = pos.above();
            climbed++;
        }

        // Standing room, not the inside of the last block.
        return new Vec3(pos.getX() + 0.5, pos.getY() + PERCH_LIFT, pos.getZ() + 0.5);
    }

    /**
     * Whether a player would actually fit here, which is two blocks rather than one.
     *
     * <p>The climb used to stop at the first empty block, and an empty block with a solid one on top
     * of it is not somewhere anybody can stand - it is a gap under an overhang. The zip aimed into
     * it, the player was driven head-first into the ceiling, and the arrival never registered.
     */
    private static boolean standingRoom(Level level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    /**
     * One ray to the limit of the vine's reach, or null if it found nothing usable.
     *
     * <p>The height test lives here rather than at the end of the search, and that matters: refusing
     * a low hit inside the cast means the widening rings carry on looking, so aiming near the foot
     * of a wall finds the wall higher up instead of giving up. Testing afterwards would have thrown
     * away the whole search on the first thing it happened to touch.
     */
    private static BlockHitResult cast(Level level, Player player, Vec3 eye, Vec3 direction) {
        HitResult hit = level.clip(new ClipContext(eye, eye.add(direction.scale(RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult block)) {
            return null;
        }

        return block.getLocation().y - player.getY() >= MIN_HEIGHT ? block : null;
    }
}
