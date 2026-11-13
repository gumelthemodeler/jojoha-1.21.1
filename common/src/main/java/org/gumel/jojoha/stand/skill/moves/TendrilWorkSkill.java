package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.hierophant.HierophantTraps;
import org.gumel.jojoha.stand.hierophant.Tendrils;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class TendrilWorkSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tendril_work");

    public static final TendrilWorkSkill INSTANCE = new TendrilWorkSkill();

    private static final int COOLDOWN_TICKS = 10;
    private static final int SEEK = 3;
    private static final int SHOW_TICKS = 20;

    private static final double NET_RADIUS = 3.0;
    private static final int NET_TICKS = 6000;

    private TendrilWorkSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.tendril_work";
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
        return EnergyWeight.TRIVIAL.cost();
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        BlockHitResult hit = ChoreSkill.aim(player, data);
        if (player.isShiftKeyDown()) {
            return hit.getType() == HitResult.Type.BLOCK ? null : "message.jojoha.tendril_work.no_ground";
        }
        return hit.getType() == HitResult.Type.BLOCK && hidden(player.serverLevel(), hit) != null
                ? null : "message.jojoha.chore.nothing_to_reach";
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        BlockHitResult hit = ChoreSkill.aim(player, data);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        int tint = Tendrils.tintOf(player);
        Vec3 hand = player.getEyePosition().subtract(0, 0.4, 0);
        Vec3 elbow = hit.getLocation();

        if (player.isShiftKeyDown()) {
            Vec3 centre = Vec3.atBottomCenterOf(hit.getBlockPos().relative(hit.getDirection()));
            HierophantTraps.alarm(level, player, centre, NET_RADIUS, NET_TICKS);
            Tendrils.line(level, Tendrils.newKey(), hand, centre, Tendrils.THIN, SHOW_TICKS, tint);
            level.playSound(null, BlockPos.containing(centre), SoundEvents.VINE_STEP, SoundSource.PLAYERS, 1F, 1.3F);
            player.displayClientMessage(Component.translatable(
                    "message.jojoha.tendril_work.net_laid"), true);
            return true;
        }

        BlockPos target = hidden(level, hit);
        if (target == null) {
            return false;
        }
        Vec3 at = Vec3.atCenterOf(target);
        BlockHitResult touch = new BlockHitResult(at, Direction.getNearest(at.subtract(elbow)).getOpposite(),
                target, false);
        boolean worked = level.getBlockState(target).useWithoutItem(level, player, touch).consumesAction();
        if (worked) {
            Tendrils.line(level, Tendrils.newKey(), hand, elbow, Tendrils.THIN, SHOW_TICKS, tint);
            Tendrils.line(level, Tendrils.newKey(), elbow, at, Tendrils.THIN, SHOW_TICKS, tint);
            if (stand != null) {
                stand.reachTowards(elbow);
            }
        }
        return worked;
    }

    private static BlockPos hidden(ServerLevel level, BlockHitResult hit) {
        BlockPos centre = hit.getBlockPos();
        BlockPos best = null;
        double bestSqr = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-SEEK, -SEEK, -SEEK), centre.offset(SEEK, SEEK, SEEK))) {
            if (!ChoreSkill.workable(level.getBlockState(pos))) {
                continue;
            }
            double sqr = pos.distSqr(centre);
            if (sqr < bestSqr) {
                bestSqr = sqr;
                best = pos.immutable();
            }
        }
        return best;
    }
}
