package org.gumel.jojoha.worldevent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import org.gumel.jojoha.entity.VampireEntity;
import org.gumel.jojoha.registry.ModRegistries;
import org.jetbrains.annotations.Nullable;

public final class VampireNight implements WorldEvents.Handler {
    private static final int SPAWN_EVERY = 60;
    private static final double COUNT_RANGE = 56.0;
    private static final int FEWEST = 5;
    private static final int MOST = 10;
    private static final int PACK = 2;
    private static final int PACK_EXTRA = 2;
    private static final int PACK_SPREAD = 4;
    private static final float MAX_EFFECTIVE_DIFFICULTY = 6.75F;
    private static final int NEAREST = 20;
    private static final int FARTHEST = 40;
    private static final int TRIES = 6;
    private static final int DARK_ENOUGH = 7;

    @Override
    public boolean canRoll(ServerLevel level) {
        return !level.players().isEmpty();
    }

    @Override
    public void start(ServerLevel level, WorldEvents state) {
        for (ServerPlayer player : level.players()) {
            level.playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT,
                    1.0F, 0.5F);
        }
    }

    @Override
    public void tick(ServerLevel level, WorldEvents state) {
        if (level.getGameTime() % SPAWN_EVERY != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() && !state.forced()) {
                continue;
            }
            int cap = cap(level, player.blockPosition());
            int near = level.getEntitiesOfClass(VampireEntity.class, player.getBoundingBox().inflate(COUNT_RANGE)).size();
            int pack = Math.min(cap - near, PACK + level.random.nextInt(PACK_EXTRA + 1));
            if (pack <= 0) {
                continue;
            }
            VampireEntity leader = spawnAround(level, ModRegistries.VAMPIRE.get(), player.blockPosition(), NEAREST, FARTHEST, true);
            if (leader == null) {
                continue;
            }
            for (int i = 1; i < pack; i++) {
                spawnAround(level, ModRegistries.VAMPIRE.get(), leader.blockPosition(), 1, PACK_SPREAD, true);
            }
        }
    }

    static int cap(ServerLevel level, BlockPos at) {
        DifficultyInstance difficulty = level.getCurrentDifficultyAt(at);
        return Math.round(Mth.lerp(Mth.clamp(difficulty.getEffectiveDifficulty() / MAX_EFFECTIVE_DIFFICULTY, 0F, 1F),
                FEWEST, MOST));
    }

    @Override
    public void stop(ServerLevel level, WorldEvents state) {
    }

    @Nullable
    static BlockPos ground(ServerLevel level, BlockPos centre, int nearest, int farthest) {
        for (int i = 0; i < TRIES; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double out = nearest + level.random.nextDouble() * (farthest - nearest);
            int x = centre.getX() + (int) Math.round(Math.cos(angle) * out);
            int z = centre.getZ() + (int) Math.round(Math.sin(angle) * out);
            if (!level.hasChunkAt(new BlockPos(x, centre.getY(), z))) {
                continue;
            }
            BlockPos at = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (level.getFluidState(at.below()).isEmpty()
                    && level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP)) {
                return at;
            }
        }
        return null;
    }

    @Nullable
    public static <T extends Mob> T spawnAround(ServerLevel level, EntityType<T> type, BlockPos centre, int nearest, int farthest,
                                         boolean dark) {
        for (int i = 0; i < TRIES; i++) {
            BlockPos at = ground(level, centre, nearest, farthest);
            if (at == null) {
                return null;
            }
            if (dark && level.getBrightness(LightLayer.BLOCK, at) > DARK_ENOUGH) {
                continue;
            }
            T mob = type.create(level);
            if (mob == null) {
                return null;
            }
            mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
            if (!level.noCollision(mob)) {
                continue;
            }
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
            level.addFreshEntityWithPassengers(mob);
            return mob;
        }
        return null;
    }
}
