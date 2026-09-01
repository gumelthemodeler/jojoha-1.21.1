package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.grapple.GrappleSwing;
import org.gumel.jojoha.stand.grapple.HermitGrappleHook;

/**
 * Runs the rope on the client that is hanging from it.
 *
 * <p>Client-side on purpose, and it is the same decision the pilot system made for the same reason.
 * A swing is a thing you feel in your hands: the arc has to answer the keys on the frame they are
 * pressed, and anything that goes to the server and back has a fifth of a second in it at best. On
 * a pendulum that is the difference between steering and asking.
 *
 * <p>It is also, conveniently, the honest place for it. Minecraft already lets the client decide
 * where its own player is and reports the result; applying the constraint here means the server is
 * told the same thing it is told about walking, and there is nothing to reconcile.
 *
 * <p>The hook itself is the server's. This only reads it.
 */
public final class GrappleController {
    /** The rope length, kept here because the constraint deliberately does not own it. */
    private static double length = -1;

    /** The hook we are hanging from, cached so the entity list is not searched every tick. */
    private static HermitGrappleHook hook;

    private GrappleController() {
    }

    /** Dropped on death, dimension change, disconnect - anything that ends the situation. */
    public static void clear() {
        hook = null;
        length = -1;
    }

    /**
     * How long the vine is for this hook, or the fallback if it is not ours to know.
     *
     * <p>Only the local player's rope has a length in the strict sense - it is the number the
     * constraint is holding them to, and it lives here because nothing else needs it. Another
     * player's vine is drawn at whatever it currently spans, which reads as taut; getting slack
     * right on someone else's rope would mean syncing a float every tick to move a curve nobody is
     * hanging from.
     */
    public static double ropeLengthFor(HermitGrappleHook which, double fallback) {
        return which == hook && length > 0 ? length : fallback;
    }

    /** The hook the local player is attached to, for the rope renderer, or null. */
    public static HermitGrappleHook active() {
        return hook != null && hook.isAlive() ? hook : null;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            clear();
            return;
        }

        if (hook == null || !hook.isAlive() || hook.holder() != player) {
            hook = find(player);
            length = -1;
        }
        if (hook == null) {
            return;
        }

        if (!hook.isAttached()) {
            // Still in the air. Nothing pulls on a hook that has not bitten yet.
            length = -1;
            return;
        }

        Vec3 anchor = hook.position();

        // Hauled, not hung. A zip owns the player's motion outright for the second it lasts, so the
        // rope constraint below never runs for one - see GrappleZip.
        if (hook.isZip()) {
            length = -1;
            org.gumel.jojoha.stand.grapple.GrappleZip.tick(player, anchor);
            return;
        }

        // Taken on the first tick after it lands rather than at the throw, so the rope is however
        // long it actually turned out to be. Measuring at the throw would give a length of nearly
        // nothing and snap the player straight to the wall.
        if (length < 0) {
            length = Math.max(1.6, player.getEyePosition().distanceTo(anchor));
        }

        boolean reelIn = minecraft.options.keyJump.isDown();
        boolean reelOut = minecraft.options.keyShift.isDown();

        length = GrappleSwing.tick(player, anchor, length, reelIn, reelOut);
    }

    /** The player's own hook, if one is in the level. */
    private static HermitGrappleHook find(LocalPlayer player) {
        if (player.level() == null) {
            return null;
        }
        for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
            if (entity instanceof HermitGrappleHook candidate && candidate.holder() == player) {
                return candidate;
            }
        }
        return null;
    }
}
