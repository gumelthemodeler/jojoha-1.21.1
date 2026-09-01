package org.gumel.jojoha.stand.skill;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;

/**
 * Direct control of a long-range Stand: the user stands still and flies it themselves.
 *
 * <p>The Stand is made to wear the player's own rotation every tick. This is what lets the camera
 * sit on the Stand and still answer the mouse: the mouse turns the player, the Stand copies the
 * player, and the camera - which reads its rotation from whatever entity it is attached to - turns
 * with it. Without that copy the view would be locked to wherever the Stand happened to be facing
 * and the mouse would appear dead.
 *
 * <p>The leash is what keeps this from being free reconnaissance. Beyond it the Stand simply
 * refuses to travel further out; it is not dragged back, because being yanked out of the player's
 * hands mid-flight reads as a bug rather than as a limit.
 */
public final class PilotSystem {
    /** How far a piloted Stand may get from its user before it stops going any further. */
    public static final double LEASH = 26.0;

    /**
     * How fast the Stand can end up going, in blocks a tick.
     *
     * <p>About the same as creative flight, which is the thing this is trying to feel like.
     */
    private static final double SPEED = 0.55;

    /**
     * How much of the gap to the wanted velocity is closed each tick, and what is kept when the
     * keys are let go.
     *
     * <p>These two are the entire difference between flying and being teleported. The Stand used to
     * have its position written directly every tick - a fixed step in a fixed direction, starting
     * and stopping dead - which gives a camera riding on it nothing to interpolate smoothly between
     * and reads as stuttering however high the speed is set. Reaching for a velocity and carrying it
     * gives momentum on the way in and a glide on the way out, and gives the client's own entity
     * interpolation a continuous path to follow.
     *
     * <p>The pair is chosen together. What survives each tick is {@code DRAG - ACCELERATION}, and
     * the speed they settle at is {@code ACCELERATION / (1 - DRAG + ACCELERATION)} of {@link #SPEED}
     * - two thirds of it. They were eased from 0.86/0.28 to 0.91/0.18, which leaves that settled
     * speed exactly where it was and only lengthens the approach to it, from about a quarter of a
     * second to about half. Changing one without the other moves the top speed as a side effect.
     */
    private static final double ACCELERATION = 0.18;
    private static final double DRAG = 0.91;

    /** Below this the Stand is treated as stopped, so it settles instead of creeping. */
    private static final double REST = 1.0E-3;
    /** Approach rate for the drift back into formation once control is released. */
    private static final double RECALL_SPEED = 0.9;

    private PilotSystem() {
    }

    /**
     * Applies one tick of steering input.
     *
     * <p>The input arrives every tick while piloting, so it is validated on every one of them - a
     * client that keeps sending after the Stand is gone, or after trust dropped, gets ignored
     * rather than trusted.
     */
    public static void applyClientPose(ServerPlayer player, double x, double y, double z,
                                       float yRot, float xRot) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!data.standPiloting) {
            return;
        }

        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand == null) {
            stop(player, data);
            return;
        }

        // Clamped, not trusted. The client is allowed to say where its Stand flew, because it is
        // the only machine that can answer the keyboard without a round trip - but the leash is the
        // server's rule, so a position outside it is pulled back to the boundary rather than
        // refused. Refusing would leave the Stand where it was while the pilot's own view carried
        // on, which is the disagreement this design exists to avoid.
        Vec3 fromUser = new Vec3(x, y, z).subtract(player.position());
        if (fromUser.lengthSqr() > LEASH * LEASH) {
            Vec3 clamped = player.position().add(fromUser.normalize().scale(LEASH));
            x = clamped.x;
            y = clamped.y;
            z = clamped.z;
        }

        stand.setPos(x, y, z);
        stand.setYRot(yRot);
        stand.setXRot(xRot);
        stand.setYBodyRot(yRot);
        stand.setYHeadRot(yRot);

        // Nothing for vanilla's travel() to apply on top - the position above is the whole answer.
        stand.setDeltaMovement(Vec3.ZERO);
    }

    /**
     * One tick of flight, run identically on the client and the server.
     *
     * <p>Run on the pilot's client and nowhere else. The server used to run its own copy from the
     * same inputs, and two simulations of one flight disagree - over collision, over which tick an
     * input landed on, over rounding. Whatever was done about the disagreement was the bug: taking
     * the server's answer dragged the flight backwards through the round trip, and ignoring it let
     * the two drift apart without limit.
     *
     * <p>So the client flies it and reports where it got to. This is what vanilla does for the
     * player's own movement, for the same reason - a first-person view has to answer the key on the
     * frame it is pressed, and nothing arriving over a network can.
     */
    public static void advance(Player player, StandEntity stand, float forward, float strafe,
                               boolean up, boolean down) {
        // Rotation first, and unconditionally: the view has to keep answering the mouse even on
        // ticks where the player is holding no movement key at all.
        stand.setYRot(player.getYRot());
        stand.setXRot(player.getXRot());
        stand.setYBodyRot(player.getYRot());
        stand.setYHeadRot(player.getYRot());

        Vec3 heading = heading(player, forward, strafe, up, down);
        Vec3 wanted = heading.lengthSqr() < 1.0E-6 ? Vec3.ZERO
                : heading.normalize().scale(SPEED);

        // Eased toward what the keys ask for rather than set to it, and what is left over is carried
        // rather than discarded - so releasing everything coasts to a stop instead of stopping on
        // the frame the key came up.
        //
        // Held in the Stand's own field rather than in deltaMovement, which vanilla's travel() also
        // reads and moves by. Storing it there meant the game applied the tick and then this code
        // applied it again, so the Stand flew at twice its written speed - the "launches rapidly"
        // that no amount of lowering SPEED would have explained.
        Vec3 previous = stand.pilotVelocity();
        Vec3 velocity = previous.scale(DRAG).add(wanted.subtract(previous).scale(ACCELERATION));

        if (velocity.lengthSqr() < REST * REST) {
            velocity = Vec3.ZERO;
        }

        velocity = leash(player, stand, velocity);
        stand.setPilotVelocity(velocity);

        // And nothing left for travel() to add on top.
        stand.setDeltaMovement(Vec3.ZERO);

        // Moved rather than placed. move() is what makes this behave like everything else in the
        // game - it updates the fields the renderer interpolates from, and it is the difference
        // between flying a Stand and dragging an icon around.
        stand.move(MoverType.SELF, velocity);
    }

    /**
     * Keeps the Stand inside its leash without ever taking the controls away.
     *
     * <p>The old rule refused the whole move once the destination fell outside, which meant that a
     * Stand which had reached the boundary could not be flown at all - not even back toward its
     * user, since that request was refused along with everything else. Only the part of the movement
     * that would push it further out is removed, so at the limit it slides along the edge and comes
     * home the instant it is asked to.
     */
    private static Vec3 leash(Player player, StandEntity stand, Vec3 velocity) {
        Vec3 fromUser = stand.position().subtract(player.position());
        if (fromUser.lengthSqr() < LEASH * LEASH) {
            return velocity;
        }

        Vec3 outward = fromUser.normalize();
        double leaving = velocity.dot(outward);
        return leaving <= 0 ? velocity : velocity.subtract(outward.scale(leaving));
    }

    /**
     * Turns held keys into a direction.
     *
     * <p>Forward follows the full look vector, pitch included, so looking up and holding forward
     * climbs - the behaviour anyone who has used spectator or creative flight already expects.
     * Strafing stays horizontal, because a roll-free strafe that also changed altitude would make
     * holding a line around a target almost impossible.
     */
    private static Vec3 heading(Player player, float forward, float strafe, boolean up, boolean down) {
        Vec3 ahead = player.getLookAngle();

        // The user's own left, because that is the direction the key is named after. The strafe
        // arriving from the client is positive for A, matching vanilla's leftImpulse - and this used
        // to be added along a rightward vector, so holding A flew right and holding D flew left.
        float yaw = (float) Math.toRadians(player.getYRot());
        Vec3 left = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));

        double lift = (up ? 1 : 0) - (down ? 1 : 0);
        return ahead.scale(forward).add(left.scale(strafe)).add(0, lift, 0);
    }

    /** Hands the Stand back to its normal following behaviour. */
    public static void stop(ServerPlayer player, JojohaPlayerData data) {
        if (!data.standPiloting) {
            return;
        }

        data.standPiloting = false;
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);

        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand != null) {
            stand.endPiloting();
        }
    }

    public static double recallSpeed() {
        return RECALL_SPEED;
    }
}
