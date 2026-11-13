package org.gumel.jojoha.hamon.moves;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Clackers;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Hitbox;
import org.gumel.jojoha.combat.ScarfPhysics;
import org.gumel.jojoha.combat.Scarves;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonCombat;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;

public final class SnakeMufflerMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "snake_muffler");
    public static final SnakeMufflerMove INSTANCE = new SnakeMufflerMove();

    private static final float DAMAGE = 5.5F;

    private static final double AIM_HALF_WIDTH = 0.9;

    private static final double PULL = 0.85;
    private static final double PULL_LIFT = 0.28;

    private static final double TOO_CLOSE = 2.0;

    private static final int COOLDOWN_TICKS = 70;
    private static final float ENERGY_COST = 14F;

    private SnakeMufflerMove() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data) {
        if (!Scarves.worn(player)) {
            player.displayClientMessage(Component.translatable("message.jojoha.hamon.no_scarf"), true);
            return false;
        }
        ScarfPhysics.State state = Scarves.ready(player);
        if (state == null) {
            return false;
        }
        LivingEntity target = aim(player);
        if (target == null) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.hamon.snake_muffler_no_target"), true);
            return false;
        }

        Scarves.strike(player, state, target);

        ServerLevel level = player.serverLevel();
        Vec3 neck = state.lastAnchor != null ? state.lastAnchor : ScarfPhysics.anchor(player, 1F);
        HamonFx.ripple(level, neck.x, neck.y, neck.z, 3);
        HamonFx.fineSpark(level, neck.x, neck.y, neck.z, 6, 0.2, 0.04);
        player.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.8F, 0.8F);
        HamonFx.sound(level, player, 0.7F, 1.0F);
        return true;
    }

    private static LivingEntity aim(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double reach = ScarfPhysics.REACH;
        AABB area = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(AIM_HALF_WIDTH);
        LivingEntity best = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity candidate : player.serverLevel().getEntitiesOfClass(LivingEntity.class, area,
                e -> Clackers.hittable(player, e))) {
            if (!Hitbox.inLane(eye, look, reach, AIM_HALF_WIDTH, candidate)
                    || !player.hasLineOfSight(candidate)) {
                continue;
            }
            double away = candidate.distanceToSqr(player);
            if (away < nearest) {
                nearest = away;
                best = candidate;
            }
        }
        return best;
    }

    public static void strike(ServerPlayer player, ScarfPhysics.State state, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        Vec3 at = state.contact != null ? state.contact : target.getBoundingBox().getCenter();
        Vec3 direction = at.subtract(ScarfPhysics.shoulderPoint(state, player));
        direction = direction.lengthSqr() < 1.0E-6 ? player.getLookAngle() : direction.normalize();

        if (target.hurt(level.damageSources().playerAttack(player),
                DamageScale.hamon(player, target, HamonCombat.scaled(player, DAMAGE)))) {
            Vec3 back = player.position().subtract(target.position());
            if (back.length() > TOO_CLOSE) {
                target.setDeltaMovement(back.normalize().scale(PULL).add(0.0, PULL_LIFT, 0.0));
                target.hurtMarked = true;
            }
        }
        Impacts.landed(level, target, at, direction,
                ImpactSpec.MEDIUM, Impacts.HAMON);
        HamonFx.fineSpark(level, at.x, at.y, at.z, 8, 0.15, 0.05);
        HamonFx.ripple(level, target.getX(), target.getY(0.5), target.getZ(), 3);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LEASH_KNOT_PLACE, SoundSource.PLAYERS, 0.9F, 1.3F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.7F, 1.2F);
    }
}
