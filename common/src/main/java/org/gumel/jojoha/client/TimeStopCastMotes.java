package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandEntity;

/**
 * Spawns the coloured motes that gather around the Stand while a time stop is wound up.
 *
 * <p>Placed on a shell around the Stand and handed the direction they sit in, which the particle
 * uses to fall back inward - so they converge on it rather than drifting off. Spawning them on a
 * shell rather than inside the model matters: motes born inside the Stand spend their first frames
 * hidden behind it and appear to wink into existence at its outline.
 *
 * <p>Client-side and local only. These belong to the wind-up the local player can see happening in
 * front of them, and there is no reason to spend network traffic telling anybody else about motes.
 */
public final class TimeStopCastMotes {
    /** How many motes a tick at the start of the wind-up and at the end of it. */
    private static final int PER_TICK = 1;
    private static final int PER_TICK_CHARGED = 4;
    private static final double SHELL_RADIUS = 1.1;

    /** Kept clear of the ground so they do not spawn inside it on flat terrain. */
    private static final double BASE_HEIGHT = 0.15;
    private static final double SPREAD_HEIGHT = 0.7;

    private TimeStopCastMotes() {
    }

    /** Call once per client tick. */
    public static void tick() {
        // Spawned while the key is held rather than after it is released. The gathering is the point
        // of them: motes that only appear once the move has been committed are decoration, motes that
        // thicken and quicken while you hold are the move telling you what it is worth so far.
        if (!TimeStopCharge.charging()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        StandEntity stand = nearestOwnStand(minecraft, player);
        double x = stand != null ? stand.getX() : player.getX();
        double y = (stand != null ? stand.getY() : player.getY()) + BASE_HEIGHT;
        double z = stand != null ? stand.getZ() : player.getZ();

        int count = PER_TICK + Math.round((PER_TICK_CHARGED - PER_TICK) * TimeStopCharge.charge());
        for (int i = 0; i < count; i++) {
            double theta = minecraft.level.random.nextDouble() * Math.PI * 2;
            double lift = minecraft.level.random.nextDouble() * SPREAD_HEIGHT;
            double offX = Math.cos(theta) * SHELL_RADIUS;
            double offZ = Math.sin(theta) * SHELL_RADIUS;

            // The offset doubles as the direction home - see TimeStopMoteParticle.
            minecraft.level.addParticle(ModRegistries.TIMESTOP_MOTE.get(),
                    x + offX, y + lift, z + offZ,
                    offX, lift - SPREAD_HEIGHT * 0.5, offZ);
        }
    }

    /**
     * The local player's Stand, if it is out.
     *
     * <p>Found by proximity and ownership rather than held, because the Stand is an ordinary entity
     * that can be despawned and resummoned underneath a cached reference.
     */
    private static StandEntity nearestOwnStand(Minecraft minecraft, Player player) {
        for (StandEntity stand : minecraft.level.getEntitiesOfClass(StandEntity.class,
                player.getBoundingBox().inflate(12.0))) {
            if (stand.getOwner() != null && stand.getOwner().getUUID().equals(player.getUUID())) {
                return stand;
            }
        }
        return null;
    }
}
