package org.gumel.jojoha.worldevent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.level.MeteorFall;
import org.gumel.jojoha.network.NetworkHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class MeteorShower implements WorldEvents.Handler {
    static final TagKey<Biome> LANDS_IN = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "meteor_shower"));

    private static final int SCAN_RADIUS = 160;
    private static final int SCAN_STEP = 32;
    private static final int FEWEST = 3;
    private static final int MOST = 5;
    private static final int NEAREST = 48;
    private static final int FARTHEST = 150;
    private static final double CLEAR_OF_PLAYERS = 40.0;
    private static final int TRIES = 16;

    private static final int STREAK_EVERY = 10;
    private static final int STREAK_ODDS = 2;
    private static final double STREAK_SPEED = 2.2;
    private static final double BIG_SPEED = 1.3;
    private static final int STREAK_LIFE = 22;
    private static final int BIG_LIFE = 44;
    private static final int BIG_ONE_IN = 5;
    private static final double STREAK_NEAREST = 50.0;
    private static final double STREAK_FARTHEST = 80.0;
    private static final double STREAK_LOWEST = 40.0;
    private static final double STREAK_RISE = 25.0;
    private static final double STREAK_VIEW_SPREAD = Math.toRadians(110.0);
    private static final double RADIANT_JITTER = Math.toRadians(24.0);
    private static final double DROP_LEAST = 0.35;
    private static final double DROP_EXTRA = 0.25;

    private double radiant = Double.NaN;

    private int left;
    private long nextAt;

    @Override
    public boolean canRoll(ServerLevel level) {
        return !level.isRaining() && level.players().stream().anyMatch(player -> nearLand(level, player.blockPosition()));
    }

    private static boolean nearLand(ServerLevel level, BlockPos centre) {
        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx += SCAN_STEP) {
            for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz += SCAN_STEP) {
                if (level.getBiome(centre.offset(dx, 0, dz)).is(LANDS_IN)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void start(ServerLevel level, WorldEvents state) {
        left = FEWEST + level.random.nextInt(MOST - FEWEST + 1);
        radiant = level.random.nextDouble() * Math.PI * 2;
        nextAt = level.getGameTime() + gap(level, state);
    }

    private int gap(ServerLevel level, WorldEvents state) {
        long remaining = Math.max(1L, state.endsAt() - level.getGameTime());
        int spread = (int) Math.max(200L, remaining / Math.max(1, left + 1));
        return spread / 2 + level.random.nextInt(spread);
    }

    @Override
    public void tick(ServerLevel level, WorldEvents state) {
        if (level.getGameTime() % STREAK_EVERY == 0) {
            for (ServerPlayer player : level.players()) {
                if (level.random.nextInt(STREAK_ODDS) == 0) {
                    launch(level, player);
                }
            }
        }
        if (left <= 0 && nextAt == 0L) {
            left = FEWEST;
        }
        if (left <= 0 || level.getGameTime() < nextAt) {
            return;
        }
        List<ServerPlayer> players = level.players();
        if (!players.isEmpty()) {
            BlockPos landing = landing(level, players.get(level.random.nextInt(players.size())), state.forced());
            if (landing != null) {
                MeteorFall.begin(level, landing);
            }
        }
        left--;
        nextAt = level.getGameTime() + gap(level, state);
    }

    @Nullable
    private static BlockPos landing(ServerLevel level, ServerPlayer player, boolean anywhere) {
        for (int i = 0; i < TRIES; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double out = NEAREST + level.random.nextDouble() * (FARTHEST - NEAREST);
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * out);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * out);
            BlockPos column = new BlockPos(x, player.getBlockY(), z);
            if (!level.hasChunkAt(column) || MeteorFall.isFalling(column)) {
                continue;
            }
            BlockPos at = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!anywhere && !level.getBiome(at).is(LANDS_IN)) {
                continue;
            }
            boolean clear = level.players().stream()
                    .noneMatch(other -> other.distanceToSqr(Vec3.atCenterOf(at)) < CLEAR_OF_PLAYERS * CLEAR_OF_PLAYERS);
            if (clear) {
                return at;
            }
        }
        return null;
    }

    private void launch(ServerLevel level, ServerPlayer player) {
        if (Double.isNaN(radiant)) {
            radiant = level.random.nextDouble() * Math.PI * 2;
        }
        double look = Math.toRadians(player.getYRot());
        double facing = Math.atan2(Math.cos(look), -Math.sin(look));
        double angle = facing + (level.random.nextDouble() - 0.5) * STREAK_VIEW_SPREAD;
        double out = STREAK_NEAREST + level.random.nextDouble() * (STREAK_FARTHEST - STREAK_NEAREST);
        Vec3 middle = player.position().add(Math.cos(angle) * out,
                STREAK_LOWEST + level.random.nextDouble() * STREAK_RISE, Math.sin(angle) * out);
        double heading = radiant + (level.random.nextDouble() - 0.5) * RADIANT_JITTER;
        double drop = DROP_LEAST + level.random.nextDouble() * DROP_EXTRA;
        boolean big = level.random.nextInt(BIG_ONE_IN) == 0;
        double speed = (big ? BIG_SPEED : STREAK_SPEED) * (0.85 + level.random.nextDouble() * 0.3);
        Vec3 velocity = new Vec3(Math.cos(heading), -drop, Math.sin(heading)).normalize().scale(speed);
        int life = (big ? BIG_LIFE : STREAK_LIFE) + level.random.nextInt(8);
        Vec3 from = middle.subtract(velocity.scale(life * 0.5));
        NetworkHandler.sendMeteorStreak(player, from, velocity, life, big);
    }

    @Override
    public void stop(ServerLevel level, WorldEvents state) {
        left = 0;
        nextAt = 0L;
        radiant = Double.NaN;
    }
}
