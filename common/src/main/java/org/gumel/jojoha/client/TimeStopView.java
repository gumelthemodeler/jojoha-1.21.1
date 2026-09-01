package org.gumel.jojoha.client;

import net.minecraft.world.phys.Vec3;

/**
 * What this client knows about the time stop it can see, whoever cast it.
 *
 * <p>Every visual reads from here rather than from the local player's own data. That is the whole of
 * the multiplayer fix: a stop cast by somebody else used to be entirely invisible, because the only
 * thing driving the shader, the sphere, the entity inversion and the clock was whether <em>you</em>
 * were the one holding time.
 *
 * <p>The centre is fixed at the moment of the cast and does not follow anyone afterwards. A time stop
 * is a thing that happened to a place: walking out of the sphere should take you out of it, not carry
 * it along with you.
 */
public final class TimeStopView {
    private static boolean active;
    private static double centreX;
    private static double centreY;
    private static double centreZ;

    private static int remainingTicks;
    private static int totalTicks;

    private TimeStopView() {
    }

    /** Called when the server says a stop has started or ended. */
    public static void set(boolean nowActive, double x, double y, double z, int remaining) {
        active = nowActive;
        remainingTicks = remaining;

        // The centre is only written when a stop starts. The packet that ends one carries no
        // position, and taking it anyway would move the centre to the world origin at exactly the
        // moment the colour begins fading back - so the grey would not fade at all, it would jump
        // somewhere thousands of blocks away and be gone in a frame.
        if (nowActive) {
            centreX = x;
            centreY = y;
            centreZ = z;
            totalTicks = Math.max(remaining, 1);
        }
    }

    public static void clear() {
        active = false;
        remainingTicks = 0;
        totalTicks = 0;
    }

    /**
     * Call once per client tick.
     *
     * <p>Counted down here rather than resent. The server tells the client once how long the stop
     * has to run and the client can keep that number itself; a packet per tick per player, for ten
     * seconds, to maintain a countdown is traffic spent on arithmetic.
     */
    public static void tick() {
        if (!active) {
            return;
        }

        if (--remainingTicks <= 0) {
            // Not cleared outright: the server sends an explicit end, and letting the countdown be
            // the only thing that stops it would leave a stop running forever if that packet were
            // ever lost. This is the belt, the packet is the braces.
            active = false;
            remainingTicks = 0;
        }
    }

    public static boolean active() {
        return active;
    }

    public static Vec3 centre() {
        return new Vec3(centreX, centreY, centreZ);
    }

    public static int remainingTicks() {
        return remainingTicks;
    }

    public static int totalTicks() {
        return totalTicks;
    }

    /**
     * Whether something is inside the running stop.
     *
     * <p>The same ball, measured from the same centre, against the same radius the server used to
     * decide what to hold - {@code TimeStopSystem.RADIUS} is read by both, so the two cannot answer
     * differently. Taken at the body's middle rather than its feet, again matching the server, or an
     * arrow level with the edge would be in on one side and out on the other.
     */
    public static boolean holds(net.minecraft.world.entity.Entity entity) {
        if (!active) {
            return false;
        }

        double radius = org.gumel.jojoha.stand.skill.TimeStopSystem.RADIUS;
        return entity.position().add(0, entity.getBbHeight() * 0.5, 0)
                .distanceToSqr(centreX, centreY, centreZ) <= radius * radius;
    }

    /** 0 to 1 through the stop, for the clock to pace itself against. */
    public static float progress() {
        if (!active || totalTicks <= 0) {
            return 0F;
        }

        return Math.min(1F, (totalTicks - remainingTicks) / (float) totalTicks);
    }
}
