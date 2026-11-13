package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.grapple.GrappleSwing;
import org.gumel.jojoha.stand.grapple.GrappleZip;
import org.gumel.jojoha.stand.grapple.HermitGrappleHook;

public final class GrappleController {
    private static double length = -1;

    private static HermitGrappleHook hook;

    private GrappleController() {
    }

    public static void clear() {
        hook = null;
        length = -1;
    }

    public static double ropeLengthFor(HermitGrappleHook which, double fallback) {
        return which == hook && length > 0 ? length : fallback;
    }

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
            GrappleZip.forget(player);
            return;
        }

        if (!hook.isAttached()) {
            length = -1;
            return;
        }

        Vec3 anchor = hook.position();

        if (hook.isZip()) {
            length = -1;
            GrappleZip.tick(player, anchor);
            return;
        }

        if (length < 0) {
            length = Math.max(1.6, player.getEyePosition().distanceTo(anchor));
        }

        boolean reelIn = minecraft.options.keyJump.isDown();
        boolean reelOut = minecraft.options.keyShift.isDown();

        length = GrappleSwing.tick(player, anchor, length, reelIn, reelOut);
    }

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
