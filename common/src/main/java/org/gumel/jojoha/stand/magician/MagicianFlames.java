package org.gumel.jojoha.stand.magician;

import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MagicianFlames {
    public static final double SNAP_RADIUS = 32.0;
    public static final int ANKH_SPREAD = 2;
    public static final int BLAZE_SPREAD = 2;
    public static final int WALL_SPREAD = 1;
    public static final int DOME_SPREAD = 3;
    public static final int DOME_SEEDS = 14;
    private static final int SEEK_DOWN = 3;

    private static final Map<ResourceKey<Level>, Set<BlockPos>> LIT = new HashMap<>();

    private MagicianFlames() {
    }

    public static boolean on() {
        return JojohaServerConfig.MAGICIAN_FLAMES.on();
    }

    static void placed(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            LIT.computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(pos.immutable());
        }
    }

    static void removed(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            Set<BlockPos> lit = LIT.get(level.dimension());
            if (lit != null) {
                lit.remove(pos);
            }
        }
    }

    public static boolean immune(Player player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        return data.stand.isPresent() && StandTypes.MAGICIANS_RED_ID.equals(data.stand.standId());
    }

    public static boolean kindle(ServerLevel level, Vec3 at, int spread) {
        if (!on()) {
            return false;
        }
        BlockPos pos = BlockPos.containing(at);
        for (int down = 0; down <= SEEK_DOWN; down++) {
            BlockPos here = pos.below(down);
            if (MagicianFlameBlock.room(level, here)) {
                level.setBlock(here, flame(spread), Block.UPDATE_ALL);
                return true;
            }
            if (!level.getBlockState(here).isAir()) {
                BlockPos above = here.above();
                if (MagicianFlameBlock.room(level, above)) {
                    level.setBlock(above, flame(spread), Block.UPDATE_ALL);
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    public static void ring(ServerLevel level, Vec3 centre, double radius, int spread) {
        int around = Math.max(8, (int) Math.round(radius * Math.PI * 2.0));
        for (int i = 0; i < around; i++) {
            double angle = Math.PI * 2.0 * i / around;
            kindle(level, centre.add(Math.cos(angle) * radius, 1.0, Math.sin(angle) * radius), spread);
        }
    }

    public static void scatter(ServerLevel level, Vec3 centre, double radius, int seeds, int spread) {
        for (int i = 0; i < seeds; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double out = Math.sqrt(level.random.nextDouble()) * radius;
            kindle(level, centre.add(Math.cos(angle) * out, 1.0, Math.sin(angle) * out), spread);
        }
    }

    private static BlockState flame(int spread) {
        return ModRegistries.MAGICIAN_FLAME.get().defaultBlockState()
                .setValue(MagicianFlameBlock.SPREAD, Math.min(MagicianFlameBlock.MAX_SPREAD, spread));
    }

    public static int snap(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Set<BlockPos> lit = LIT.get(level.dimension());
        if (lit == null || lit.isEmpty()) {
            return 0;
        }
        double reach = SNAP_RADIUS * SNAP_RADIUS;
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos pos : lit) {
            if (pos.distToCenterSqr(player.position()) <= reach) {
                out.add(pos);
            }
        }
        for (BlockPos pos : out) {
            if (level.getBlockState(pos).is(ModRegistries.MAGICIAN_FLAME.get())) {
                level.removeBlock(pos, false);
                level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                        3, 0.25, 0.1, 0.25, 0.01);
            } else {
                lit.remove(pos);
            }
        }
        if (!out.isEmpty()) {
            level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
                    0.8F, 1.2F);
        }
        return out.size();
    }

    public static void init() {
        LifecycleEvent.SERVER_STOPPED.register(server -> LIT.clear());
    }
}
