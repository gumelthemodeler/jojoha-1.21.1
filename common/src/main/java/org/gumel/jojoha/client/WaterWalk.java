package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.Climbing;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.hamon.RippleSurface;
import org.gumel.jojoha.registry.ModRegistries;

public final class WaterWalk {

    private WaterWalk() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !Climbing.walksOnWater(player)) {
            return;
        }

        if (player.isInWater()) {
            return;
        }

        Vec3 movement = player.getDeltaMovement();
        if (movement.y >= 0.0) {
            return;
        }

        if (minecraft.level.getFluidState(player.blockPosition().below()).isEmpty()) {
            return;
        }

        player.setDeltaMovement(movement.x, 0.0, movement.z);
        player.setOnGround(true);
        player.resetFallDistance();

        ripples(minecraft, player, movement);
    }

    private static void ripples(Minecraft minecraft, LocalPlayer player, Vec3 movement) {
        if (!(minecraft.level instanceof ClientLevel level)) {
            return;
        }

        double speed = Math.sqrt(movement.x * movement.x + movement.z * movement.z);

        if (speed < 0.01) {
            if (player.tickCount % IDLE_EVERY == 0) {
                ring(level, player, player.position());
            }
            return;
        }

        stride += speed;
        if (stride < STRIDE) {
            return;
        }
        stride = 0.0;

        foot = !foot;
        float yaw = player.getYRot() * ((float) Math.PI / 180F);
        Vec3 across = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw)).scale(foot ? FOOT_APART : -FOOT_APART);

        ring(level, player, player.position().add(across));
    }

    private static void ring(ClientLevel level, LocalPlayer player, Vec3 at) {
        double surface = RippleSurface.surfaceAt(
                level, at.x, player.getY() + 0.1, at.z);
        if (Double.isNaN(surface)) {
            return;
        }

        boolean rainbow = Fate.ultimate(ClientPlayerDataCache.data);
        level.addParticle(rainbow ? ModRegistries.HAMON_RIPPLE_RAINBOW.get()
                        : ModRegistries.HAMON_RIPPLE.get(),
                at.x, surface, at.z, 0.0, 0.0, 0.0);

        for (int i = 0; i < 2; i++) {
            level.addParticle(rainbow ? ModRegistries.HAMON_MOTE_FINE_RAINBOW.get()
                            : ModRegistries.HAMON_MOTE_FINE.get(),
                    at.x + (level.random.nextDouble() - 0.5) * 0.4,
                    surface + level.random.nextDouble() * 0.08,
                    at.z + (level.random.nextDouble() - 0.5) * 0.4,
                    0.0, 0.01, 0.0);
        }
    }

    private static double stride;

    private static boolean foot;

    private static final double FOOT_APART = 0.22;

    private static final double STRIDE = 0.9;

    private static final int IDLE_EVERY = 12;
}
