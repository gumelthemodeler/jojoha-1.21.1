package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.chariot.Chariot;
import org.gumel.jojoha.stand.combat.StandFist;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class BarrageAir {
    private static final int LEAN_TICKS = 10;

    private static final double RANGE = 48.0;

    private static final int CYCLE_BEAT_TICKS = 3;

    private static final double LANE_NEAR = 1.3;
    private static final double LANE_FAR = 2.1;
    private static final double LANE_WIDTH = 0.9;

    private static final int RING_EVERY = 4;
    private static final double RING_SCALE = 0.35;

    private static final int WHOOSH_GAP_TICKS = 2;
    private static final float WHOOSH_VOLUME = 0.2F;
    private static final float WHOOSH_PITCH = 1.5F;
    private static final float WHOOSH_PITCH_SPREAD = 0.4F;

    private static final int GROUND_SEARCH = 3;
    private static final int LEAF_BITS = 3;
    private static final int SPLASHES = 3;
    private static final double SCATTER = 0.3;

    private static final Map<Integer, Integer> BARRAGING = new HashMap<>();
    private static final Map<Integer, Integer> PUFFS = new HashMap<>();
    private static final Map<Integer, Long> WHOOSHED = new HashMap<>();

    private BarrageAir() {
    }

    public static void clear() {
        BARRAGING.clear();
        PUFFS.clear();
        WHOOSHED.clear();
    }

    public static float lean(StandEntity stand, float partialTick) {
        Integer ticks = BARRAGING.get(stand.getId());
        return ticks == null ? 0F : Math.min(1F, (ticks + partialTick) / LEAN_TICKS);
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            clear();
            return;
        }
        if (minecraft.isPaused()) {
            return;
        }

        Set<Integer> seen = new HashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof StandEntity stand) || !stand.isBarraging()) {
                continue;
            }
            seen.add(stand.getId());
            BARRAGING.merge(stand.getId(), 1, Integer::sum);
            if (stand.distanceToSqr(minecraft.player) > RANGE * RANGE) {
                continue;
            }
            if (JojohaClientConfig.BARRAGE_AIR.on()) {
                blows(level, stand);
            }
        }
        BARRAGING.keySet().retainAll(seen);
        PUFFS.keySet().retainAll(seen);
        WHOOSHED.keySet().retainAll(seen);
    }

    private static void blows(ClientLevel level, StandEntity stand) {
        long now = level.getGameTime();
        Vec3 chest = stand.position().add(0.0, stand.getBbHeight() * 0.6, 0.0);
        boolean fists = false;
        for (int slot = 0; slot < StandFist.SLOTS; slot++) {
            StandFist fist = StandFistView.live(stand, slot, 0F);
            if (fist == null) {
                continue;
            }
            fists = true;
            if (now - fist.spawnTick() == StandFist.OUT_TICKS) {
                puff(level, stand, fist.aim(), fist.aim().subtract(chest), true);
            }
        }
        if (fists) {
            return;
        }

        boolean kicking = stand.isKickBarrage();
        boolean blades = Chariot.is(stand);
        int beat = kicking ? StandEntity.KICK_HIT_INTERVAL : blades ? StandEntity.SWORD_HIT_INTERVAL : CYCLE_BEAT_TICKS;
        if (stand.tickCount % beat != 0) {
            return;
        }
        RandomSource random = level.getRandom();
        float yaw = stand.yBodyRot;
        Vec3 forward = StandFist.toWorld(Vec3.ZERO, yaw, new Vec3(0, 0, -16));
        Vec3 right = StandFist.right(yaw);
        double height = stand.getBbHeight() * (kicking
                ? 0.35 + random.nextDouble() * 0.25
                : 0.5 + random.nextDouble() * 0.3);
        double reach = LANE_NEAR + random.nextDouble() * (LANE_FAR - LANE_NEAR);
        Vec3 at = stand.position().add(forward.scale(reach))
                .add(right.scale((random.nextDouble() - 0.5) * LANE_WIDTH))
                .add(0.0, height, 0.0);
        puff(level, stand, at, forward, !blades);
    }

    private static void puff(ClientLevel level, StandEntity stand, Vec3 at, Vec3 heading, boolean whoosh) {
        RandomSource random = level.getRandom();
        Vec3 dir = heading.lengthSqr() < 1.0E-6 ? Vec3.ZERO : heading.normalize();
        if (PUFFS.merge(stand.getId(), 1, Integer::sum) % RING_EVERY == 0) {
            level.addParticle(ModRegistries.IMPACT_RING.get(), at.x, at.y, at.z, RING_SCALE, 0.0, 0.0);
        }
        stir(level, at, dir, random);

        long now = level.getGameTime();
        Long last = WHOOSHED.get(stand.getId());
        if (whoosh && (last == null || now - last >= WHOOSH_GAP_TICKS)) {
            WHOOSHED.put(stand.getId(), now);
            level.playLocalSound(at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS,
                    WHOOSH_VOLUME, WHOOSH_PITCH + random.nextFloat() * WHOOSH_PITCH_SPREAD, false);
        }
    }

    static void dust(ClientLevel level, Vec3 at, Vec3 dir, RandomSource random) {
        stir(level, at, dir, random);
    }

    private static void stir(ClientLevel level, Vec3 at, Vec3 dir, RandomSource random) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(at).mutable();
        BlockState inside = level.getBlockState(pos);
        if (inside.is(BlockTags.LEAVES)) {
            bits(level, inside, at.x, at.y, at.z, dir, random, LEAF_BITS);
        }

        for (int down = 0; down <= GROUND_SEARCH; down++) {
            FluidState fluid = level.getFluidState(pos);
            if (fluid.is(FluidTags.WATER)) {
                double surface = pos.getY() + fluid.getHeight(level, pos);
                for (int i = 0; i < SPLASHES; i++) {
                    level.addParticle(ParticleTypes.SPLASH, at.x + scatter(random), surface, at.z + scatter(random),
                            dir.x * 0.1, 0.1, dir.z * 0.1);
                }
                return;
            }
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return;
            }
            pos.move(Direction.DOWN);
        }
    }

    private static void bits(ClientLevel level, BlockState state, double x, double y, double z, Vec3 dir,
                             RandomSource random, int count) {
        BlockParticleOption chip = new BlockParticleOption(ParticleTypes.BLOCK, state);
        for (int i = 0; i < count; i++) {
            level.addParticle(chip, x + scatter(random), y, z + scatter(random),
                    dir.x * 0.15, 0.08, dir.z * 0.15);
        }
    }

    private static double scatter(RandomSource random) {
        return (random.nextDouble() - 0.5) * 2.0 * SCATTER;
    }

}
