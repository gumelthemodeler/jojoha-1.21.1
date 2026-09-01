package org.gumel.jojoha.stand.grapple;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The zip: both vines bite, and they do not let you hang.
 *
 * <p>The opposite half of the same idea as {@link GrappleSwing}, and worth keeping apart from it
 * rather than adding a mode to it. A swing is a constraint - the rope says what you may not do and
 * gravity does the rest, which is why it reads as hanging. A zip is a winch: it says where you are
 * going and takes you there, and gravity is simply not part of the answer.
 *
 * <p>Trying to express one as a setting on the other means a class that is a constraint on some
 * ticks and a drive on others, and every line in it has to ask which. Two short files say it better.
 *
 * <h2>Why it sets the velocity rather than adding to it</h2>
 *
 * <p>Accelerating toward the anchor gives a launch that starts slow and arrives fastest, which is
 * exactly backwards for this. A zip should be at speed immediately - the vines snap taut and you go
 * - and it should hold that speed the whole way so the distance covered is predictable. Setting the
 * velocity outright does both, and it has the useful side effect of overriding whatever the fall was
 * doing, so a zip thrown while dropping does not have to fight the drop first.
 *
 * <p>What it deliberately does not do is stop you at the far end. The pull ends near the anchor and
 * the momentum stays, so you arrive travelling and can carry straight into the next thing - which is
 * most of what makes this fun rather than a teleport with an animation.
 */
public final class GrappleZip {
    /** How fast the vines haul, in blocks per tick. */
    private static final double SPEED = 2.05;

    /**
     * How close counts as arrived.
     *
     * <p>Wide enough that the drive lets go before you reach the wall rather than after. The last
     * stretch is covered on momentum, so you land against it instead of being driven into it.
     */
    public static final double ARRIVE = 1.5;

    /**
     * How much of the drive is given over to going up, near the end.
     *
     * <p>Zipping to a ledge and stopping dead under its lip is the single most annoying way for this
     * to fail, and it is what happens with a pure straight line: the anchor is on the face of the
     * block, so the path to it runs into the wall below it. A little lift over the last part of the
     * travel carries you over the edge instead.
     */
    private static final double LIFT = 0.3;
    private static final double LIFT_FROM = 4.0;

    private GrappleZip() {
    }

    /**
     * One tick of being pulled. Returns false once there is nothing left to pull toward.
     *
     * @param anchor where the vines are fixed
     */
    public static boolean tick(Player player, Vec3 anchor) {
        Vec3 toAnchor = anchor.subtract(player.getEyePosition());
        double distance = toAnchor.length();

        if (distance <= ARRIVE) {
            return false;
        }

        // Pressed against something and getting no closer. The drive is a flat speed toward the
        // anchor, so geometry in the way does not slow it or stop it - it just stops the player
        // moving, and without this the move spends its whole timeout shoving them into a wall.
        //
        // Measured as real progress rather than as contact, because there is no reliable way to ask
        // "am I stuck" and every way to ask "did I get anywhere". A zip that is working closes
        // ground every single tick; one that is not has already failed, whatever the reason.
        if (!closingIn(player, distance)) {
            return false;
        }

        Vec3 heading = toAnchor.scale(1.0 / distance);

        // Tilted upward as the anchor gets close - see LIFT.
        if (distance < LIFT_FROM) {
            double share = LIFT * (1.0 - distance / LIFT_FROM);
            heading = heading.add(0, share, 0).normalize();
        }

        player.setDeltaMovement(heading.scale(SPEED));

        // Arriving is not falling. Without this a long downward zip lands as a killing drop, which
        // is a strange thing for a move whose whole purpose is getting somewhere safely.
        player.resetFallDistance();
        return true;
    }

    /**
     * How near the anchor each flier was last tick, so a stalled one can be told from a slow one.
     *
     * <p>Keyed by player id and cleared as soon as a zip ends, which is what keeps this from being a
     * leak: an entry only exists while somebody is mid-flight.
     */
    private static final java.util.Map<Integer, Double> LAST_DISTANCE = new java.util.HashMap<>();

    /** How many ticks of no progress are allowed before the vine lets go. */
    private static final int STALL_TICKS = 4;

    /** Less than this much closer in a tick does not count as having moved at all. */
    private static final double PROGRESS = 0.05;

    private static final java.util.Map<Integer, Integer> STALLED = new java.util.HashMap<>();

    private static boolean closingIn(Player player, double distance) {
        int id = player.getId();
        Double previous = LAST_DISTANCE.put(id, distance);

        if (previous == null || previous - distance > PROGRESS) {
            STALLED.remove(id);
            return true;
        }

        int stalled = STALLED.merge(id, 1, Integer::sum);
        if (stalled < STALL_TICKS) {
            return true;
        }

        forget(player);
        return false;
    }

    /** Dropped when a zip ends, however it ended. */
    public static void forget(Player player) {
        LAST_DISTANCE.remove(player.getId());
        STALLED.remove(player.getId());
    }
}
