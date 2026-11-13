package org.gumel.jojoha.stand;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModRegistries;

import java.util.ArrayList;
import java.util.List;

public final class StandAuraEffect {
    private static final double COLOUR_PAYLOAD_SPEED = 1.0;

    private static final int SPAWN_INTERVAL_TICKS = 2;
    private static final int MOTES_PER_SPAWN = 2;

    private static final double EMIT_RADIUS = 0.32;
    private static final double EMIT_MIN_HEIGHT = 0.05;
    private static final double EMIT_HEIGHT_RANGE = 1.85;

    private static final double EMIT_ANGLE_STEP = 2.39996;

    private StandAuraEffect() {
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % SPAWN_INTERVAL_TICKS != 0) {
            return;
        }

        if (asleep(player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        RandomSource random = level.getRandom();

        JojohaPlayerData data = PlayerDataAccess.get(player);
        boolean bound = StandTypes
                .byIdOrDefault(data.stand.standId())
                .form().isBound();

        int colour = Impacts.colourOf(player);
        float red = ((colour >> 16) & 0xFF) / 255F;
        float green = ((colour >> 8) & 0xFF) / 255F;
        float blue = (colour & 0xFF) / 255F;

        List<ServerPlayer> viewers = new ArrayList<>();
        for (ServerPlayer other : level.players()) {
            if (other != player && seesStands(other)) {
                viewers.add(other);
            }
        }
        if (viewers.isEmpty()) {
            return;
        }

        for (int i = 0; i < MOTES_PER_SPAWN; i++) {
            int seed = player.tickCount * MOTES_PER_SPAWN + i;

            Vec3 offset = bound
                    ? handOffset(seed, random, player.yBodyRot)
                    : auraOffset(seed, random);
            double x = player.getX() + offset.x;
            double y = player.getY() + offset.y;
            double z = player.getZ() + offset.z;

            for (ServerPlayer other : viewers) {
                level.sendParticles(other, ModRegistries.STAND_AURA.get(), false,
                        x, y, z, 0, red, green, blue, COLOUR_PAYLOAD_SPEED);
            }
        }
    }

    public static boolean seesStands(ServerPlayer viewer) {
        if (viewer.isSpectator()) {
            return true;
        }
        JojohaPlayerData data = PlayerDataAccess.get(viewer);
        return data.stand.isPresent() || data.anubis;
    }

    public static boolean asleep(Player player) {
        return player.isSleeping() || player.getSleepTimer() > 0;
    }

    public static Vec3 auraOffset(int index, RandomSource random) {
        double angle = index * EMIT_ANGLE_STEP;
        double radius = Math.sqrt(random.nextDouble()) * EMIT_RADIUS;
        double height = EMIT_MIN_HEIGHT + random.nextDouble() * EMIT_HEIGHT_RANGE;

        return new Vec3(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
    }

    public static Vec3 handOffset(int index, RandomSource random, float bodyYaw) {
        double yaw = Math.toRadians(bodyYaw);

        double acrossX = Math.cos(yaw);
        double acrossZ = Math.sin(yaw);
        double side = (index % 2 == 0 ? 1 : -1) * HAND_REACH;

        double scatter = HAND_SCATTER;
        return new Vec3(
                acrossX * side + (random.nextDouble() - 0.5) * scatter,
                HAND_HEIGHT + (random.nextDouble() - 0.5) * scatter,
                acrossZ * side + (random.nextDouble() - 0.5) * scatter);
    }

    private static final double HAND_REACH = 0.42;
    private static final double HAND_HEIGHT = 0.98;
    private static final double HAND_SCATTER = 0.28;

    public static int motesPerSpawn() {
        return MOTES_PER_SPAWN;
    }

    public static int spawnIntervalTicks() {
        return SPAWN_INTERVAL_TICKS;
    }
}
