package org.gumel.jojoha.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.StandEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class RapierThrust {
    public static final int COOLDOWN_TICKS = 40;

    public static final float DAMAGE_MULTIPLIER = 1.4F;

    public static final float PIERCE = 0.30F;

    public static final double EXTRA_REACH = 1.6;

    private static final double LUNGE = 1.9;

    private static final double AIM = 0.35;

    private static final int WEAR = 2;

    private static final Set<UUID> THRUSTING = new HashSet<>();

    private RapierThrust() {
    }

    public static boolean isThrusting(LivingEntity attacker) {
        return THRUSTING.contains(attacker.getUUID());
    }

    public static boolean perform(ServerPlayer player, ItemStack rapier) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();

        player.setDeltaMovement(player.getDeltaMovement().add(look.x * LUNGE, 0.0, look.z * LUNGE));
        player.hurtMarked = true;

        MovementFx.pose(player, MovementFx.RAPIER_LUNGE);

        rapier.hurtAndBreak(WEAR, player, EquipmentSlot.MAINHAND);
        drawLine(level, player, look);
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(),
                SoundSource.PLAYERS, 0.6F, 1.5F);

        StandBeat.at(MovementFx.RAPIER_LUNGE_CONTACT_TICKS, player, () -> pierce(player, look));
        return true;
    }

    private static void pierce(ServerPlayer player, Vec3 look) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = inFront(player, look, reach(player));
        if (target == null) {
            return;
        }

        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_MULTIPLIER;

        THRUSTING.add(player.getUUID());
        try {
            target.hurt(level.damageSources().playerAttack(player), DamageScale.combat(player, target, damage));
        } finally {
            THRUSTING.remove(player.getUUID());
        }

        level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.8F, 1.6F);
        Impacts.landed(level, target, look, ImpactSpec.MEDIUM.silent(), Impacts.STEEL);
    }

    private static double reach(ServerPlayer player) {
        return player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) + EXTRA_REACH;
    }

    private static LivingEntity inFront(ServerPlayer player, Vec3 look, double reach) {
        Vec3 from = player.getEyePosition();
        HitResult wall = player.level().clip(new ClipContext(
                from, from.add(look.scale(reach)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double clear = wall.getType() == HitResult.Type.MISS
                ? reach : from.distanceTo(wall.getLocation());

        LivingEntity found = null;
        double nearest = Double.MAX_VALUE;

        AABB probe = player.getBoundingBox().inflate(reach);
        for (LivingEntity candidate : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                probe, entity -> entity != player && entity.isAlive() && entity.isAttackable()
                        && !(entity instanceof StandEntity))) {
            double away = Hitbox.distanceAlong(from, look, clear, AIM, candidate);
            if (away >= 0 && away < nearest) {
                nearest = away;
                found = candidate;
            }
        }
        return found;
    }

    private static void drawLine(ServerLevel level, ServerPlayer player, Vec3 look) {
        Vec3 from = player.getEyePosition().add(0.0, -0.2, 0.0);
        int steps = 12;
        for (int step = 1; step <= steps; step++) {
            Vec3 at = from.add(look.scale(step * 0.32));
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
