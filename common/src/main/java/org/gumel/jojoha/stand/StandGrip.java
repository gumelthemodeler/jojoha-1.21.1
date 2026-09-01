package org.gumel.jojoha.stand;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * One thing held in a Stand's hand, and everything that has to be put back afterwards.
 *
 * <h2>Why the held mob is a passenger</h2>
 *
 * <p>The obvious build is to write the mob's position from the hand every tick. The movement doc
 * spends a whole section on why that goes wrong, and it is right: the mob's own movement runs on the
 * same tick, so the two take turns writing the same value and the result is a mob vibrating in the
 * Stand's grip. Fighting that means suppressing movement, then physics, then knockback, then
 * pathfinding - a list that is never quite finished.
 *
 * <p>Riding solves it by construction. A passenger does not move itself; the vehicle positions it,
 * once, in the right order, and the client interpolates the result the way it does for every boat
 * and horse in the game. So the grab makes the target a passenger of the Stand and puts the hand
 * position in {@code getPassengerAttachmentPoint}. The stutter the doc warns about cannot happen,
 * because nothing is competing to write the position.
 *
 * <h2>What still has to be switched off</h2>
 *
 * <p>Riding stops the mob moving. It does not stop it thinking - a held skeleton would still shoot,
 * still pick targets, still try to path. So its AI is suspended too, and its gravity with it, and
 * both are recorded here first so that release can put back what was actually there rather than
 * assuming defaults. A mob that was already gravityless before being grabbed does not gain gravity
 * from having been held.
 */
final class StandGrip {

    /** What is held. */
    private final LivingEntity held;

    /** What it was like before it was picked up, so release can restore rather than assume. */
    private final boolean hadNoAi;
    private final boolean hadNoGravity;

    private StandGrip(LivingEntity held, boolean hadNoAi, boolean hadNoGravity) {
        this.held = held;
        this.hadNoAi = hadNoAi;
        this.hadNoGravity = hadNoGravity;
    }

    /**
     * Takes hold of something, or returns null if it cannot be held.
     *
     * <p>Players are refused. Taking a player's control away from them is a different feature with
     * different consequences, and one that should be a deliberate decision rather than something
     * that falls out of a mob grab reaching far enough.
     */
    static StandGrip take(StandEntity stand, LivingEntity target) {
        if (target instanceof Player || target == stand || !target.isAlive()) {
            return null;
        }
        if (target.isPassenger() || !stand.getPassengers().isEmpty()) {
            return null;
        }

        boolean hadNoAi = target instanceof Mob mob && mob.isNoAi();
        boolean hadNoGravity = target.isNoGravity();

        if (!target.startRiding(stand, true)) {
            return null;
        }

        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setNoAi(true);
        }
        target.setNoGravity(true);
        target.setDeltaMovement(Vec3.ZERO);
        target.fallDistance = 0F;

        return new StandGrip(target, hadNoAi, hadNoGravity);
    }

    LivingEntity held() {
        return held;
    }

    /** Whether this hold is still real - the thing can die, be removed, or be pulled off. */
    boolean stillHeld(StandEntity stand) {
        return held.isAlive() && !held.isRemoved() && held.getVehicle() == stand;
    }

    /**
     * Keeps the held thing turned the way the hand is.
     *
     * <p>Position is the vehicle's job and needs nothing here. Rotation is not: left alone, a held
     * mob keeps facing whatever direction it was facing when it was picked up, so the Stand can turn
     * a full circle while the thing in its fist stares off at the horizon. Following the Stand's yaw
     * is what makes it look gripped rather than parked.
     */
    void tick(StandEntity stand) {
        float yaw = stand.getYRot();
        held.setYRot(yaw);
        held.setYBodyRot(yaw);
        held.setYHeadRot(yaw);
        held.yRotO = yaw;
        held.setDeltaMovement(Vec3.ZERO);
        held.fallDistance = 0F;
    }

    /**
     * Lets go, putting back everything that was changed and handing over the throw.
     *
     * <p>The velocity matters as much as the letting go. A mob released with nothing simply drops,
     * which reads as the Stand losing its grip rather than choosing to throw - so a release always
     * carries a velocity, even if it is only the small one of being set down.
     */
    void release(Vec3 velocity) {
        held.stopRiding();

        if (held instanceof Mob mob) {
            mob.setNoAi(hadNoAi);
        }
        held.setNoGravity(hadNoGravity);

        held.setDeltaMovement(velocity);
        // Without this the server keeps its own idea of the velocity and the throw never leaves the
        // hand on the client - the mob drops straight down where it was let go.
        held.hurtMarked = true;
        held.fallDistance = 0F;
    }
}
