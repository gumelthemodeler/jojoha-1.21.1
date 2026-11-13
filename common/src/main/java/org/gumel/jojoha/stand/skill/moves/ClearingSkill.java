package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandHands;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.ArrayList;
import java.util.List;

public final class ClearingSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "clearing");

    public static final ClearingSkill INSTANCE = new ClearingSkill();

    private static final int COOLDOWN_TICKS = 20;
    private static final double STRIP_REACH = 6.0;
    private static final int RADIUS = 4;
    private static final int BELOW = 1;
    private static final int ABOVE = 2;
    private static final double ARC = Math.cos(Math.toRadians(60.0));
    private static final int LIMIT = 64;

    private ClearingSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.clearing";
    }

    @Override
    public boolean isUnlocked(JojohaPlayerData data) {
        return data.stand.isPresent();
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return EnergyWeight.LIGHT.cost();
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        return strippable(player) == null && growth(player).isEmpty() ? "message.jojoha.clearing.nothing" : null;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();
        BlockPos log = strippable(player);
        if (log != null) {
            BlockState state = level.getBlockState(log);
            level.setBlock(log, stripped(state).withPropertiesOf(state), Block.UPDATE_ALL_IMMEDIATE);
            level.playSound(null, log, SoundEvents.AXE_STRIP, SoundSource.PLAYERS, 1F, 1F);
            if (stand != null) {
                stand.reachTowards(Vec3.atCenterOf(log));
            }
            return true;
        }
        List<BlockPos> cut = growth(player);
        if (cut.isEmpty()) {
            return false;
        }
        int cleared = 0;
        for (BlockPos pos : cut) {
            if (player.gameMode.destroyBlock(pos)) {
                cleared++;
            }
        }
        if (stand != null) {
            stand.reachTowards(player.getEyePosition().add(player.getLookAngle().scale(RADIUS * 0.5)));
        }
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1F, 1.2F);
        return cleared > 0;
    }

    private static boolean clearable(BlockState state) {
        if (state.is(BlockTags.LEAVES) || state.is(Blocks.COBWEB)) {
            return true;
        }
        return !state.isAir() && state.canBeReplaced() && state.getFluidState().isEmpty()
                && !state.is(Blocks.SNOW) && !state.is(BlockTags.FIRE);
    }

    private static List<BlockPos> growth(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().multiply(1, 0, 1);
        look = look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : look.normalize();
        BlockPos feet = player.blockPosition();
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-RADIUS, -BELOW, -RADIUS),
                feet.offset(RADIUS, ABOVE, RADIUS))) {
            double dx = pos.getX() + 0.5 - player.getX();
            double dz = pos.getZ() + 0.5 - player.getZ();
            double flat = Math.sqrt(dx * dx + dz * dz);
            if (flat > RADIUS + 0.5 || flat < 0.5 || (dx * look.x + dz * look.z) / flat < ARC) {
                continue;
            }
            if (clearable(level.getBlockState(pos)) && level.mayInteract(player, pos)) {
                out.add(pos.immutable());
                if (out.size() >= LIMIT) {
                    break;
                }
            }
        }
        return out;
    }

    private static BlockPos strippable(ServerPlayer player) {
        BlockHitResult hit = StandHands.aimFrom(player, player.getEyePosition(),
                player.getViewVector(1.0F), STRIP_REACH);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockState state = player.level().getBlockState(hit.getBlockPos());
        return state.is(BlockTags.LOGS) && stripped(state) != null
                && player.serverLevel().mayInteract(player, hit.getBlockPos()) ? hit.getBlockPos() : null;
    }

    private static Block stripped(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key.getPath().startsWith("stripped_")) {
            return null;
        }
        ResourceLocation bare = ResourceLocation.fromNamespaceAndPath(key.getNamespace(), "stripped_" + key.getPath());
        return BuiltInRegistries.BLOCK.containsKey(bare) ? BuiltInRegistries.BLOCK.get(bare) : null;
    }
}
