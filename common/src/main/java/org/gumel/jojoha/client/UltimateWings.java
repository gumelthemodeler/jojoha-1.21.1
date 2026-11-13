package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.ultimate.WingFlight;

public final class UltimateWings {
    private static final double SINK = 0.1;

    private static final double SINK_EASE = 0.35;

    private static final double GLIDE_SPEED = 0.36;

    private static final double STEER = 0.08;

    private static boolean jumpWasDown;

    private UltimateWings() {
    }

    public static boolean wingsOut(Player player) {
        JojohaPlayerData data = ClientPlayerDataCache.data;
        return player == Minecraft.getInstance().player && data != null && Fate.ultimate(data) && data.ultimateWings;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            jumpWasDown = false;
            return;
        }
        boolean jumpDown = minecraft.options.keyJump.isDown();
        boolean pressed = jumpDown && !jumpWasDown;
        jumpWasDown = jumpDown;

        if (!wingsOut(player) || !WingFlight.canGlide(player)) {
            return;
        }

        if (pressed) {
            if (player.isFallFlying()) {
                NetworkHandler.sendWingFlap();
            } else if (!player.isShiftKeyDown() && player.tryToStartFallFlying()) {
                player.connection.send(new ServerboundPlayerCommandPacket(player,
                        ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            }
        }
        if (player.isFallFlying()) {
            return;
        }

        Vec3 motion = player.getDeltaMovement();
        double y = motion.y < -SINK ? motion.y + (-SINK - motion.y) * SINK_EASE : motion.y;
        Vec3 look = player.getLookAngle();
        Vec3 heading = new Vec3(look.x, 0.0, look.z);
        double x = motion.x;
        double z = motion.z;
        if (heading.lengthSqr() > 1.0E-4) {
            heading = heading.normalize().scale(GLIDE_SPEED);
            x += (heading.x - x) * STEER;
            z += (heading.z - z) * STEER;
        }
        player.setDeltaMovement(x, y, z);
    }
}
