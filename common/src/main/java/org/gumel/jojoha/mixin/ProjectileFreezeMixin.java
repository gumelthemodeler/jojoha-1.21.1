package org.gumel.jojoha.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.gumel.jojoha.stand.skill.TimeStopSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stops a projectile caught in stopped time from running its tick at all.
 *
 * <p>Pinning was not enough, and the reason is worth writing down because it looked like it should
 * have been. A held arrow had its position, heading and velocity rewritten every tick, so it did not
 * move - but it still <em>ticked</em>, and an arrow's tick ends in a hit test. That test uses a box
 * grown a whole block past wherever the arrow is going, so an arrow hanging still within a block of
 * a frozen mob finds it, every tick, without having travelled a millimetre.
 *
 * <p>What happened next is the bug the player actually saw. The hit resolves into
 * {@code entity.hurt}, which the stop deliberately answers false to - the damage is being held to
 * pay out when time resumes. Vanilla reads a false there as the target having refused the blow, and
 * its response is {@code deflect(ProjectileDeflection.REVERSE, ...)}: it turns the arrow round.
 * Then, because a pinned arrow's speed is zero and {@code 0 * 0.2} is still zero, it fails the
 * {@code lengthSqr() < 1.0E-7} check on the next line, drops itself as an item and discards. So the
 * arrow did not merely bounce - it was destroyed against a target it never reached, mid-freeze,
 * before it could ever carry through.
 *
 * <p>There is no tuning that fixes that. A frozen projectile must not run its tick, and this is the
 * one place every projectile's tick goes through. Targeting {@code ServerLevel.tickNonPassenger}
 * rather than {@code tick} on the projectiles themselves is deliberate: {@code tick} is overridden
 * separately by arrows, thrown items, fireballs, hooks and the rest, and cancelling the base
 * implementation would leave every subclass's own body running anyway.
 *
 * <p>It also does the catching, not just the cancelling. Anything arriving from outside the bubble
 * is frozen here, at the head of its own tick - see {@code TimeStopSystem.holdIfInside}. The sweep
 * that runs after the level has ticked is too late for those: by then the arrow has already moved,
 * already run its hit test, and already been destroyed against whatever it found. This is the only
 * point that is reliably before all of that.
 *
 * <p>Server-side only, which is where the hold lives; the client simply stops receiving movement
 * for something that is not moving.
 */
@Mixin(ServerLevel.class)
public abstract class ProjectileFreezeMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void jojoha$freezeHeldProjectiles(Entity entity, CallbackInfo ci) {
        // The instanceof first, and a static "is anything stopped at all" behind it: this runs for
        // every entity in the level every tick, so the common case has to cost a type check and
        // nothing else.
        if (entity instanceof Projectile projectile && TimeStopSystem.holdIfInside(projectile)) {
            // The one thing the cancelled tick would still have done - see FrozenMotion.
            org.gumel.jojoha.stand.skill.FrozenMotion.hold(projectile);
            ci.cancel();
        }
    }
}
