package org.gumel.jojoha.stand.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.labyrinth.TntBarrelBlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BarrageDemolition {
    public static final TagKey<Block> PROOF = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "barrage_proof"));

    public static final float HARDNESS_CAP = 4.0F;

    public static final int MAX_PER_BEAT = 2;

    private static final double STEP = 0.5;

    private BarrageDemolition() {
    }

    public static int strike(ServerLevel level, ServerPlayer owner, Vec3 from, Vec3 look, double reach,
                             double halfWidth) {
        if (!owner.mayBuild() || owner.isSpectator() || !JojohaServerConfig.standsMayBreak(level)) {
            return 0;
        }

        List<BlockPos> candidates = inLane(level, owner, from, look, reach, halfWidth);
        int broken = 0;
        for (BlockPos pos : candidates) {
            if (broken >= JojohaServerConfig.BLOCKS_PER_BLOW.whole()) {
                break;
            }
            if (level.destroyBlock(pos, false, owner)) {
                broken++;
            }
        }
        return broken;
    }

    private static List<BlockPos> inLane(ServerLevel level, ServerPlayer owner, Vec3 from, Vec3 look,
                                         double reach, double halfWidth) {
        List<BlockPos> found = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        int steps = Math.max(1, (int) Math.ceil(reach / STEP));
        for (int i = 0; i <= steps; i++) {
            Vec3 at = from.add(look.scale(reach * i / steps));
            AABB around = new AABB(at, at).inflate(halfWidth);
            for (BlockPos pos : BlockPos.betweenClosed(
                    BlockPos.containing(around.minX, around.minY, around.minZ),
                    BlockPos.containing(around.maxX, around.maxY, around.maxZ))) {
                if (!seen.add(pos.asLong()) || !breakable(level, owner, pos)) {
                    continue;
                }
                found.add(pos.immutable());
            }
        }
        return found;
    }

    public static boolean breakable(ServerLevel level, ServerPlayer owner, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof TntBarrelBlock barrel) {
            barrel.prime(level, pos.immutable(), state, TntBarrelBlock.STRUCK_FUSE);
            return false;
        }
        if (!JojohaServerConfig.standsMayBreak(level)) {
            return false;
        }
        if (state.isAir() || state.is(PROOF) || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.hasBlockEntity()) {
            return false;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0F || hardness > JojohaServerConfig.HARDEST_BREAKABLE.value()) {
            return false;
        }
        return level.mayInteract(owner, pos);
    }
}
