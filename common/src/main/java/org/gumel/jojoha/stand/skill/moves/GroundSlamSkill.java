package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.SlamState;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.StandFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class GroundSlamSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "ground_slam");
    public static final GroundSlamSkill INSTANCE = new GroundSlamSkill();

    private static final double DRIVE = 1.6;

    private static final float DAMAGE_PER_BLOCK = 1.6F;
    private static final float MAX_DAMAGE = 12F;

    private static final float MIN_FALL = 2.0F;

    private static final double MIN_RADIUS = 2.0;
    private static final double MAX_RADIUS = 5.5;
    private static final float RADIUS_FULL_AT = 12F;

    private static final double KNOCKBACK = 0.9;

    private static final int COOLDOWN_TICKS = 60;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private GroundSlamSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "move.jojoha.ground_slam";
    }

    @Override
    public boolean requiresStand() {
        return false;
    }

    @Override
    public TrustTier minimumTrust() {
        return TrustTier.DORMANT;
    }

    @Override
    public boolean movesTheUsersBody() {
        return true;
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
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (player.onGround()) {
            return false;
        }

        ServerLevel level = player.serverLevel();

        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x * 0.35, -DRIVE, movement.z * 0.35);
        player.hurtMarked = true;

        SlamState.begin(player);

        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 0.7F, 0.6F);
        return true;
    }

    public static void land(ServerPlayer player, float fallDistance) {
        if (fallDistance < MIN_FALL) {
            return;
        }

        ServerLevel level = player.serverLevel();
        Vec3 at = player.position();

        float damage = Math.min(MAX_DAMAGE, fallDistance * DAMAGE_PER_BLOCK);

        double share = Math.min(1.0, fallDistance / RADIUS_FULL_AT);
        double radius = MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * share;

        AABB probe = new AABB(at.subtract(radius, 2.0, radius), at.add(radius, 2.0, radius));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, probe,
                entity -> entity != player && entity.isAlive())) {
            double away = target.distanceToSqr(at.x, target.getY(), at.z);
            if (away > radius * radius) {
                continue;
            }

            target.hurt(level.damageSources().playerAttack(player), DamageScale.scale(DamageScale.Kind.STAND, player, target, damage, MAX_DAMAGE));

            Vec3 push = new Vec3(target.getX() - at.x, 0.0, target.getZ() - at.z);
            if (push.lengthSqr() < 1.0E-6) {
                push = new Vec3(0.0, 0.0, 1.0);
            }
            target.setDeltaMovement(push.normalize().scale(KNOCKBACK).add(0.0, 0.45, 0.0));
            target.hurtMarked = true;
            Impacts.landed(level, target, push.normalize().add(0.0, 0.5, 0.0),
                    ImpactSpec.MEDIUM.silent(), Impacts.PLAYER);
        }

        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                        level.getBlockState(player.blockPosition().below())),
                at.x, at.y + 0.1, at.z, (int) (radius * 14), radius * 0.5, 0.1, radius * 0.5, 0.25);

        int colour = Impacts.PLAYER;
        for (double out = 1.0; out <= radius; out += 1.2) {
            StandFx.ring(level, at.add(0.0, 0.15, 0.0), out, out > radius * 0.6 ? 2 : 1);
        }
        Impacts.landed(level, at.add(0.0, 0.2, 0.0), new Vec3(0.0, 1.0, 0.0),
                share >= 1.0 ? ImpactSpec.FINISHER : ImpactSpec.HEAVY, colour);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 0.7F, 1.5F);
    }
}
