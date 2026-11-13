package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.stand.grapple.HermitGrappleHook;
import org.gumel.jojoha.stand.skill.moves.DoubleJumpSkill;

public final class DoubleJumpInput {
    private static final int GRACE_TICKS = 2;

    private static boolean wasDown;

    private static int airborneTicks;

    private static boolean jumped;

    private static boolean spent;

    private DoubleJumpInput() {
    }

    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) {
            wasDown = false;
            forget();
            return;
        }

        boolean down = minecraft.options.keyJump.isDown();
        boolean pressed = down && !wasDown;
        wasDown = down;

        if (footing(player)) {
            airborneTicks = 0;
            spent = false;
            jumped = down;
            return;
        }
        airborneTicks++;

        if (!pressed || minecraft.screen != null) {
            return;
        }
        if (airborneTicks <= GRACE_TICKS) {
            jumped = true;
            return;
        }
        if (HermitGrappleHook.findFor(player) != null) {
            return;
        }
        if (jumped && !spent && DoubleJumpSkill.INSTANCE
                .isUnlocked(ClientPlayerDataCache.data)) {
            spent = true;
            NetworkHandler.sendRequestDoubleJump(player.getDeltaMovement());
        }
    }

    private static void forget() {
        airborneTicks = 0;
        jumped = false;
        spent = false;
    }

    private static boolean footing(LocalPlayer player) {
        return player.onGround()
                || player.getAbilities().flying
                || player.isSpectator()
                || player.isPassenger()
                || player.onClimbable()
                || player.isInWater()
                || player.isInLava()
                || player.isFallFlying();
    }
}
