package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.gumel.jojoha.network.NetworkHandler;

public final class MoveInputReport {
    private static final float DEAD_ZONE = 0.1F;

    private static int sentForward = Integer.MIN_VALUE;
    private static int sentLeft = Integer.MIN_VALUE;

    private MoveInputReport() {
    }

    public static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.getConnection() == null) {
            sentForward = Integer.MIN_VALUE;
            sentLeft = Integer.MIN_VALUE;
            return;
        }
        int forward = sign(player.input.forwardImpulse);
        int left = sign(player.input.leftImpulse);
        if (forward == sentForward && left == sentLeft) {
            return;
        }
        sentForward = forward;
        sentLeft = left;
        NetworkHandler.sendMoveInput(forward, left);
    }

    private static int sign(float impulse) {
        return impulse > DEAD_ZONE ? 1 : impulse < -DEAD_ZONE ? -1 : 0;
    }
}
