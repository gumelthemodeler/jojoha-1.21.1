package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.combat.fx.StandFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class GutPunchSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "gut_punch");
    public static final GutPunchSkill INSTANCE = new GutPunchSkill();

    private static final float DAMAGE = 4.0F;

    private static final double KNOCKBACK = 1.4;

    private static final double LIFT = 0.28;

    private static final double REACH = 3.2;
    private static final double WIDTH = 1.1;

    private static final int COOLDOWN_TICKS = 55;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private GutPunchSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "move.jojoha.gut_punch";
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
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();

        Vec3 look = player.getLookAngle();

        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0E-8) {
            return false;
        }
        flat = flat.normalize();

        MovementFx.pose(player, MovementFx.GUT_PUNCH);
        player.swing(InteractionHand.MAIN_HAND, true);

        Vec3 heading = flat;
        StandBeat.at(MovementFx.GUT_PUNCH_CONTACT_TICKS, player, () -> punch(player, heading));
        return true;
    }

    private static void punch(ServerPlayer player, Vec3 flat) {
        ServerLevel level = player.serverLevel();
        Vec3 at = player.position().add(flat.scale(REACH * 0.5)).add(0.0, player.getBbHeight() * 0.5, 0.0);
        AABB probe = new AABB(at.subtract(WIDTH, WIDTH, WIDTH), at.add(WIDTH, WIDTH, WIDTH));

        LivingEntity struck = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, probe,
                entity -> entity != player && entity.isAlive())) {
            double distance = candidate.distanceToSqr(player);
            if (distance < nearest) {
                nearest = distance;
                struck = candidate;
            }
        }

        if (struck == null) {
            Whiff.body(player);
            return;
        }
        struck.hurt(level.damageSources().playerAttack(player), DamageScale.stand(player, struck, DAMAGE));

        Bleed.apply(struck);

        struck.setDeltaMovement(flat.scale(KNOCKBACK).add(0.0, LIFT, 0.0));
        struck.hurtMarked = true;

        struck.attackAnim = 0F;
        struck.oAttackAnim = 0F;

        Vec3 landed =
                struck.position().add(0.0, struck.getBbHeight() * 0.5, 0.0);
        Impacts.landed(level, landed, flat,
                ImpactSpec.HEAVY, Impacts.PLAYER);

        StandFx.trail(level, landed, landed.add(flat.scale(2.0)), Impacts.PLAYER);
    }
}
