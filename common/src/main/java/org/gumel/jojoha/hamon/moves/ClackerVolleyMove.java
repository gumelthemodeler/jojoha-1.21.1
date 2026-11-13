package org.gumel.jojoha.hamon.moves;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.ClackerPhysics;
import org.gumel.jojoha.combat.Clackers;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.combat.Hitbox;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonCombat;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.item.ClackersItem;

public final class ClackerVolleyMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "clacker_volley");
    public static final ClackerVolleyMove INSTANCE = new ClackerVolleyMove();

    public static final float HIT_DAMAGE = 1.2F;
    public static final float UNDEAD_MULTIPLIER = 2.0F;

    private static final double AIM_HALF_WIDTH = 1.0;
    private static final double KNOCKBACK = 0.08;

    private static final int COOLDOWN_TICKS = 120;
    private static final float ENERGY_COST = 25F;

    private ClackerVolleyMove() {
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
        if (!ClackersItem.held(player)) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.hamon.no_clackers"), true);
            return false;
        }
        ClackerPhysics.State state = Clackers.ready(player);
        if (state == null) {
            return false;
        }
        LivingEntity target = aim(player);
        if (target == null) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.hamon.clacker_volley_no_target"), true);
            return false;
        }

        Clackers.volley(player, state, target);

        ServerLevel level = player.serverLevel();
        Vec3 hand = state.lastAnchor;
        HamonFx.ring(level, hand.x, hand.y, hand.z, 0.9, 1);
        HamonFx.ripple(level, hand.x, hand.y, hand.z, 3);
        HamonFx.fineSpark(level, hand.x, hand.y, hand.z, 8, 0.2, 0.04);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.9F, 1.6F);
        HamonFx.sound(level, player, 0.8F, 1.3F);
        return true;
    }

    private static LivingEntity aim(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double reach = ClackerPhysics.VOLLEY_REACH;
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

    public static void strike(ServerPlayer player, ClackerPhysics.State state, ClackerPhysics.Head head) {
        ServerLevel level = player.serverLevel();
        Entity found = level.getEntity(state.volleyTarget);
        if (!(found instanceof LivingEntity target) || !target.isAlive()) {
            return;
        }
        float damage = HamonCombat.scaled(player, HIT_DAMAGE);
        if (HamonCombat.isUndead(target)) {
            damage *= UNDEAD_MULTIPLIER;
        }
        boolean last = state.volleyAge >= ClackerPhysics.VOLLEY_HITS * ClackerPhysics.VOLLEY_PERIOD
                / ClackerPhysics.HEADS;
        Vec3 from = head.pos.subtract(head.vel);
        Vec3 direction = head.pos.subtract(state.lastAnchor);
        direction = direction.lengthSqr() < 1.0E-6 ? player.getLookAngle() : direction.normalize();
        if (target.hurt(level.damageSources().playerAttack(player),
                DamageScale.hamon(player, target, damage))) {
            target.push(direction.x * KNOCKBACK, 0.02, direction.z * KNOCKBACK);
        }
        HitWindow.open(target);
        Impacts.landed(level, target, head.pos, direction,
                last ? ImpactSpec.FINISHER : ImpactSpec.LIGHT,
                Impacts.HAMON);
        HamonFx.fineSpark(level, head.pos.x, head.pos.y, head.pos.z, 5, 0.12, 0.04);
        HamonFx.spark(level, from.x, from.y, from.z, 2, 0.08, 0.02);
        if (last) {
            HamonFx.ring(level, head.pos.x, head.pos.y, head.pos.z, 1.2, 1);
            HamonFx.ripple(level, target.getX(), target.getY(0.5), target.getZ(), 4);
        }
        player.swing(state.hand, true);
        level.playSound(null, head.pos.x, head.pos.y, head.pos.z, SoundEvents.BAMBOO_HIT, SoundSource.PLAYERS,
                1.0F, 1.3F + level.random.nextFloat() * 0.4F);
    }
}
