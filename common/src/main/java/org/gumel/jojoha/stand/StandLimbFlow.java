package org.gumel.jojoha.stand;

import net.minecraft.util.Mth;

/**
 * The Stand's sense of its own motion, as its limbs feel it.
 *
 * <p>Banking the whole body into a turn (see {@code StandRenderer}) tips the Stand like a rigid
 * object. What actually reads as flying is the body arriving somewhere slightly before its arms and
 * legs do - the limbs stream behind, then overshoot and settle. This holds the numbers that produce
 * that: the Stand's travel, resolved into its own forward/lateral/vertical axes, and smoothed three
 * times over at three different rates.
 *
 * <p>The three rates are the whole trick. One smoothed value moves every limb in lockstep, which is
 * just the rigid body again with extra steps. Feeding the shoulders a quick response, the elbows a
 * slower one and the trailing ends slower still makes a change in direction travel <em>outward</em>
 * along each limb, one joint at a time - which is what a chain of joints with mass actually does,
 * and what the eye reads as flowing rather than posed.
 *
 * <p>Deliberately plain arithmetic with no client-only types: an instance is held on the entity
 * itself, which exists on both sides, so anything client-flavoured in here would blow up a
 * dedicated server the moment it loaded the class.
 */
public final class StandLimbFlow {
    /**
     * How much of the gap each stage closes per tick. Descending, so each joint out along a limb
     * is lazier than the one before it and the motion arrives late by a growing margin.
     */
    private static final float NEAR_RESPONSE = 0.45F;
    private static final float MID_RESPONSE = 0.22F;
    private static final float FAR_RESPONSE = 0.12F;

    /**
     * Travel below this (blocks/tick) is treated as stationary.
     *
     * <p>The Stand is repositioned every tick by a spring, which never truly comes to rest - it
     * always has some residual jitter. Amplified into limb angles that reads as a twitch, so the
     * floor is what lets the Stand actually look still when it is still.
     */
    private static final double DEADZONE = 0.004;

    /** Ceiling on the input speed, so a long pursuit dash doesn't fling the limbs off the model. */
    private static final float MAX_SPEED = 0.9F;

    private final Axis forward = new Axis();
    private final Axis lateral = new Axis();
    private final Axis vertical = new Axis();

    /**
     * Folds one tick of travel into the chain.
     *
     * <p>Takes the distance actually covered since last tick rather than a velocity vector,
     * because the Stand is placed with {@code setPos} every tick and never accumulates a delta
     * movement to read.
     */
    public void tick(double dx, double dy, double dz, float yawDegrees) {
        float yaw = (float) Math.toRadians(yawDegrees);

        // Into the Stand's own frame, so "moving forward" stays forward however it is facing.
        double along = -dx * Math.sin(yaw) + dz * Math.cos(yaw);
        double across = dx * Math.cos(yaw) + dz * Math.sin(yaw);

        forward.tick(clean(along));
        lateral.tick(clean(across));
        vertical.tick(clean(dy));
    }

    private static float clean(double travel) {
        if (Math.abs(travel) < DEADZONE) {
            return 0F;
        }
        return Mth.clamp((float) travel, -MAX_SPEED, MAX_SPEED);
    }

    public Axis forward() {
        return forward;
    }

    public Axis lateral() {
        return lateral;
    }

    public Axis vertical() {
        return vertical;
    }

    /** One direction of travel, tracked at three removes from the body. */
    public static final class Axis {
        private final Stage near = new Stage(NEAR_RESPONSE);
        private final Stage mid = new Stage(MID_RESPONSE);
        private final Stage far = new Stage(FAR_RESPONSE);

        private void tick(float target) {
            // Chained rather than run in parallel off the same input: each stage is chasing the one
            // inboard of it, which is what makes the delay accumulate down the limb instead of
            // every joint independently lagging the body by a fixed amount.
            near.tick(target);
            mid.tick(near.value);
            far.tick(mid.value);
        }

        /** Shoulders and hips - closest to the body, quickest to react. */
        public float near(float partialTick) {
            return near.at(partialTick);
        }

        /** Elbows and knees. */
        public float mid(float partialTick) {
            return mid.at(partialTick);
        }

        /** The trailing ends - hands, feet, hair. */
        public float far(float partialTick) {
            return far.at(partialTick);
        }

        /**
         * How far this stage is currently behind the one inboard of it.
         *
         * <p>This is the whip. A joint's own lag says where it is; the <em>difference</em> between
         * two joints says how hard the limb is currently being dragged, which is the part that
         * spikes on a direction change and then snaps back to nothing.
         */
        public float whip(float partialTick) {
            return near.at(partialTick) - far.at(partialTick);
        }
    }

    /**
     * One smoothed value, remembering last tick so the renderer can interpolate between them.
     *
     * <p>Smoothed on the tick rather than per frame on purpose: an exponential smooth applied once
     * a frame converges at a rate that depends on the framerate, so the same Stand would trail its
     * limbs differently at 30fps and 240fps.
     */
    private static final class Stage {
        private final float response;
        private float value;
        private float previous;

        private Stage(float response) {
            this.response = response;
        }

        private void tick(float target) {
            previous = value;
            value += (target - value) * response;
        }

        private float at(float partialTick) {
            return Mth.lerp(partialTick, previous, value);
        }
    }
}
