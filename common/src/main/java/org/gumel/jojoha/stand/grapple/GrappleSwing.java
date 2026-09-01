package org.gumel.jojoha.stand.grapple;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * What the rope does to whoever is on the end of it.
 *
 * <p>One tick of a pendulum, and it is the whole of the move. Everything else - the vine, the hook,
 * the drawing - is presentation; this is the part that decides whether the grapple feels like a
 * grapple or like being dragged on a string.
 *
 * <h2>The constraint</h2>
 *
 * <p>A rope is not a spring and it is not a winch. It does exactly one thing: it refuses to get any
 * longer. Inside its length it does nothing at all, and you fall normally. At its length it removes
 * the part of your motion that would stretch it and leaves everything else untouched.
 *
 * <p>That second sentence is the whole trick, and it is why this is written as a projection rather
 * than as a force. Take the velocity, split it into the part pointing along the rope and the part
 * across it, throw away the outward half of the first, keep the second entirely. What survives is
 * motion around the anchor - which is a swing, and it comes out of the geometry rather than out of
 * any number that had to be tuned.
 *
 * <p>Pulling toward the anchor with a force instead is the obvious alternative and it is wrong in a
 * way that is hard to unpick afterwards: a force that is strong enough to hold you on a long rope
 * yanks you inward on a short one, so the swing speeds up as it tightens and the whole thing turns
 * into a slingshot. Projection has no strength to get wrong.
 *
 * <h2>What is added on top</h2>
 *
 * <p>Only three things, and each of them is there because the pure pendulum is missing something a
 * player expects:
 *
 * <ul>
 *   <li><b>A little slack.</b> Real rope has some give and a hard constraint reads as a wall - you
 *       hit the end of it and stop. The correction is scaled rather than absolute, so arriving at
 *       full extension is a firm catch rather than a collision.</li>
 *   <li><b>Air control.</b> A pendulum you cannot influence is a ride. The strafe input pushes
 *       across the rope, never along it, so it steers the swing without ever being able to defeat
 *       the constraint.</li>
 *   <li><b>Reeling.</b> Holding jump shortens the rope, sneak lets it out. This is what turns a
 *       swing into travel: you climb on the way up and pay out on the way down.</li>
 * </ul>
 */
public final class GrappleSwing {
    /**
     * How much of the overstretch is taken back each tick.
     *
     * <p>Not all of it. Pulling the full distance back makes the rope perfectly rigid, and a rigid
     * rope stops you dead the instant you reach its length - which reads as hitting something. Most
     * of it, every tick, converges over three or four ticks into a catch that has some give in it.
     */
    private static final double TAKE_UP = 0.62;

    /** How much of the outward motion the rope refuses. One is a rope; less is elastic. */
    private static final double REFUSE = 0.94;

    /** How hard the strafe keys push across the rope, in blocks per tick squared. */
    private static final double AIR_CONTROL = 0.058;

    /**
     * How hard the vine hauls you along itself, in blocks per tick squared.
     *
     * <p>The pure pendulum was the wrong model on its own. A rope that only refuses to stretch is a
     * swing, and a swing is something that happens to you - you get back exactly the height you
     * gave up and never any more, so the move could not take you anywhere you were not already
     * going. Hermit Purple is a way of travelling, and travelling needs the rope to do work.
     *
     * <p>So it pulls, continuously, along its own length. Everything the constraint does is
     * unchanged underneath - the tangential motion is still untouched, so it still swings - but now
     * there is energy going in, and a swing with energy going in gains height across passes instead
     * of losing it. That is the difference between hanging from a rope and moving on one.
     *
     * <p>It falls off as you close on the anchor, so arriving is a glide rather than a slam into
     * the wall you were aiming at.
     */
    private static final double PULL = 0.092;
    private static final double PULL_EASE = 4.0;

    /** Below this the vine has done its job and stops hauling, or it would grind you into the block. */
    private static final double PULL_STOP = 2.2;

    /** How fast the rope can be reeled in or paid out, in blocks per tick. */
    private static final double REEL_IN = 0.32;
    private static final double REEL_OUT = 0.28;

    /** The rope may never be shorter than this, so you cannot winch yourself into the anchor. */
    private static final double MIN_LENGTH = 1.6;

    /**
     * Drag along the rope, applied only while swinging.
     *
     * <p>Slightly less than one, so a swing left alone loses height over several passes instead of
     * running forever. Vanilla air drag does not do this on its own here, because the constraint
     * keeps handing the energy back.
     */
    private static final double SWING_DRAG = 0.9985;

    private GrappleSwing() {
    }

    /**
     * Applies one tick of rope to a player, and hands back the rope length it wants next tick.
     *
     * <p>Returns the length rather than storing it so that the caller owns the state. There is one
     * of these per player at most, the caller already has somewhere to keep it, and a static field
     * in here would be a second copy that could disagree with the first.
     *
     * @param length the current rope length
     * @param reelIn whether the player is asking to climb
     * @param reelOut whether the player is asking for slack
     * @return the length after any reeling
     */
    public static double tick(Player player, Vec3 anchor, double length,
                              boolean reelIn, boolean reelOut) {
        Vec3 from = player.getEyePosition();
        Vec3 toAnchor = anchor.subtract(from);
        double distance = toAnchor.length();

        if (distance < 1.0E-4) {
            return length;
        }

        Vec3 along = toAnchor.scale(1.0 / distance);
        Vec3 velocity = player.getDeltaMovement();

        // Reeling changes the rope, not the player. Shortening it while they are already at full
        // extension lets the constraint below do the pulling on the next line, which keeps every
        // route to moving the player going through the same piece of code.
        if (reelIn) {
            length = Math.max(MIN_LENGTH, Math.min(length, distance) - REEL_IN);
        } else if (!reelOut && distance < length) {
            // Slack is taken up as it appears, so the vine behaves like something being wound in
            // rather than something you are falling through. Without this the pull above would
            // shorten the gap while the rope stayed long, and the constraint would have nothing to
            // catch you on at the bottom of the next swing.
            length = Math.max(MIN_LENGTH, distance);
        } else if (reelOut) {
            length = Math.min(HermitGrappleHook.MAX_RANGE, length + REEL_OUT);
        }

        // The haul, and it happens whether the rope is taut or slack - the vine is pulling itself
        // in, not merely refusing to lengthen. Eased down over the last few blocks so the arrival
        // is a glide; see PULL.
        if (distance > PULL_STOP) {
            double ease = Math.min(1.0, (distance - PULL_STOP) / PULL_EASE);
            velocity = velocity.add(along.scale(PULL * ease));
        }

        if (distance > length) {
            // Back toward the anchor by most of the overstretch - see TAKE_UP.
            velocity = velocity.add(along.scale((distance - length) * TAKE_UP));

            // And the refusal: whatever is left moving away from the anchor is removed, because a
            // rope cannot get longer. Motion across the rope is not touched, and that is the swing.
            double outward = -velocity.dot(along);
            if (outward > 0) {
                velocity = velocity.add(along.scale(outward * REFUSE));
            }

            velocity = velocity.scale(SWING_DRAG);
        }

        velocity = velocity.add(steer(player, along));

        player.setDeltaMovement(velocity);

        // Swinging into the ground should not hurt. The arc naturally ends near the floor and
        // taking damage for arriving is punishing something the move is for.
        player.resetFallDistance();
        return length;
    }

    /**
     * The player's own input, projected across the rope.
     *
     * <p>The projection is the important half. Feeding the raw input in would let a player press
     * forward into the anchor and haul themselves up it, which is climbing rather than swinging;
     * taking out the component along the rope leaves them able to steer the arc and unable to
     * shorten it. The constraint stays the only thing that decides distance.
     */
    private static Vec3 steer(Player player, Vec3 along) {
        float forward = player.zza;
        float strafe = player.xxa;
        if (forward == 0F && strafe == 0F) {
            return Vec3.ZERO;
        }

        float yaw = player.getYRot() * ((float) Math.PI / 180F);
        Vec3 ahead = new Vec3(-net.minecraft.util.Mth.sin(yaw), 0, net.minecraft.util.Mth.cos(yaw));
        Vec3 side = new Vec3(ahead.z, 0, -ahead.x);

        Vec3 push = ahead.scale(forward).add(side.scale(strafe));
        if (push.lengthSqr() < 1.0E-6) {
            return Vec3.ZERO;
        }

        push = push.normalize().scale(AIR_CONTROL);

        // Strip the part that runs along the rope.
        return push.subtract(along.scale(push.dot(along)));
    }
}
