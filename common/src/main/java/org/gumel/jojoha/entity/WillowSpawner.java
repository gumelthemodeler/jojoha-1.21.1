package org.gumel.jojoha.entity;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.level.ModBiomes;
import org.gumel.jojoha.registry.ModRegistries;

public final class WillowSpawner {
    private static final int ATTEMPT_INTERVAL_TICKS = 3000;

    public static final float ATTEMPT_CHANCE = 0.25F;

    private static final int MIN_DISTANCE = 24;
    private static final int MAX_DISTANCE = 48;
    private static final double CROWDING_RADIUS = 96.0;

    private static final int PLACEMENT_TRIES = 10;

    private WillowSpawner() {
    }

    public static void init() {
        TickEvent.SERVER_POST.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getGameTime() % ATTEMPT_INTERVAL_TICKS != 0) {
                    continue;
                }
                for (ServerPlayer player : level.players()) {
                    trySpawnNear(level, player);
                }
            }
        });
    }

    private static void trySpawnNear(ServerLevel level, ServerPlayer player) {
        if (level.random.nextFloat() >= JojohaServerConfig.WILLOW_CHANCE.chance()) {
            return;
        }

        if (!level.getBiome(player.blockPosition()).is(ModBiomes.IS_MOORS)) {
            return;
        }

        if (!level.getEntitiesOfClass(WillowEntity.class,
                player.getBoundingBox().inflate(CROWDING_RADIUS)).isEmpty()) {
            return;
        }

        for (int attempt = 0; attempt < PLACEMENT_TRIES; attempt++) {
            BlockPos pos = randomGroundNear(level, player);
            if (pos == null) {
                continue;
            }

            WillowEntity willow = ModRegistries.WILLOW.get().create(level);
            if (willow == null) {
                return;
            }

            willow.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    level.random.nextFloat() * 360F, 0F);
            willow.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
            level.addFreshEntity(willow);
            return;
        }
    }

    private static BlockPos randomGroundNear(ServerLevel level, ServerPlayer player) {
        double angle = level.random.nextDouble() * Math.PI * 2;
        double distance = MIN_DISTANCE + level.random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);

        int x = (int) (player.getX() + Math.cos(angle) * distance);
        int z = (int) (player.getZ() + Math.sin(angle) * distance);

        if (!level.isLoaded(new BlockPos(x, level.getMinBuildHeight(), z))) {
            return null;
        }

        BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, 0, z));
        if (!level.getBiome(ground).is(ModBiomes.IS_MOORS)) {
            return null;
        }

        AABB room = ModRegistries.WILLOW.get().getSpawnAABB(
                ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5);
        return level.noCollision(room) ? ground : null;
    }
}
