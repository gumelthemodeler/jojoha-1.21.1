package org.gumel.jojoha.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.gumel.jojoha.client.TimeStopView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The client half of freezing a projectile, which is the half that was twitching.
 *
 * <p>Stopping the server ticking a held arrow stops it moving; it does not stop the client's own
 * copy from carrying on without it. The client runs the same projectile simulation locally - it
 * applies gravity, applies drag, moves the arrow along whatever velocity it last heard about, and
 * re-derives the arrow's pitch and yaw from that velocity every frame. Meanwhile the server, having
 * cancelled its tick, has nothing new to send, so nothing corrects it until something else does. The
 * result is an arrow that will not hold still: it sags and turns under a physics step that the thing
 * it is a copy of is no longer taking.
 *
 * <p>Frozen here by the same rule the server uses rather than by being told which arrows are held.
 * The client already knows a stop is running, where its centre is and how wide it is - all three are
 * the numbers the server itself decided and broadcast - so asking "is this projectile inside the
 * stop" gets the same answer on both sides without a packet per arrow. The one number they share is
 * {@code TimeStopSystem.RADIUS}, which both this and the sphere on screen are measured from.
 *
 * <p>Cancelling the tick outright, rather than re-pinning after it, is what removes the twitch
 * rather than hiding it. A pin applied after the fact still lets the arrow move first, and a client
 * interpolates toward wherever an entity was going - so the correction itself becomes the wobble.
 * A tick that never happens has nothing to interpolate away from.
 */
@Mixin(ClientLevel.class)
public abstract class ClientProjectileFreezeMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void jojoha$freezeHeldProjectiles(Entity entity, CallbackInfo ci) {
        // Ordered so the common case costs a boolean read: almost always no stop is running, and
        // this is called for every entity in the level every tick.
        if (!TimeStopView.active() || !(entity instanceof Projectile)) {
            return;
        }

        if (TimeStopView.holds(entity)) {
            // Without this the arrow does not move and never stops twitching: the renderer keeps
            // sliding it across the gap between where it was and where it is, and a cancelled tick
            // never closes that gap. See FrozenMotion.
            org.gumel.jojoha.stand.skill.FrozenMotion.hold(entity);
            ci.cancel();
        }
    }
}
