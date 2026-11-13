package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Exposed;
import org.gumel.jojoha.combat.GuardBreak;
import org.gumel.jojoha.combat.StatPerkEffects;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StatEffects;
import org.gumel.jojoha.data.StatPerk;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.StandVoices;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class HeavyPunchSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "heavy_punch");
    public static final HeavyPunchSkill INSTANCE = new HeavyPunchSkill();

    private static final double REACH = 3.5;
    private static final float DAMAGE = 4.5F;

    private static final float REINFORCED = 6.0F;

    private static final double KNOCKBACK = 1.1;
    private static final int COOLDOWN_TICKS = 70;
    private static final float ENERGY_COST = EnergyWeight.HEAVY.cost();

    private HeavyPunchSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "move.jojoha.heavy_punch";
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
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 at = eye.add(look.scale(REACH * 0.6));

        LivingEntity target = null;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(at, at).inflate(REACH * 0.6),
                entity -> entity != player && entity.isAlive()
                        && player.distanceTo(entity) <= REACH)) {
            target = candidate;
            break;
        }

        boolean reinforced = data.standSummoned && data.manifestTier().manifestsEntity();
        float damage = Math.min(DamageScale.HIT_CEILING, reinforced ? REINFORCED * data.stand.powerScale()
                : DAMAGE * StatEffects.bodyDamageScale(data.strength));

        player.swing(InteractionHand.MAIN_HAND, true);
        StandType standType = reinforced
                ? StandTypes.byIdOrDefault(data.stand.standId()) : null;
        SoundEvent windUp = StandVoices.heavy(standType, StandVoices.shadowed(data.stand));
        if (windUp != null) {
            level.playSound(null, player.blockPosition(), windUp, SoundSource.PLAYERS, 1.0F, 1.0F);
        }

        if (target == null) {
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 0.6F, 0.7F);
            Exposed.open(player);
            return true;
        }

        if (StatPerk.SHIELDBREAKER.has(data)
                && target.isBlocking()
                && !GuardBreak.isBroken(target)) {
            GuardBreak.apply(target);
        }
        target.hurt(level.damageSources().playerAttack(player), DamageScale.stand(player, target, damage));
        if (StatPerk.STAGGERING_BLOW.has(data) && target.isAlive()) {
            target.addEffect(new MobEffectInstance(
                    ModEffects.stun(),
                    StatPerkEffects.STAGGERING_BLOW_TICKS,
                    Stun.TIER_ONE, false, true, true));
        }
        Vec3 push = target.position().subtract(player.position()).normalize().scale(KNOCKBACK);
        target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.35, push.z));
        target.hurtMarked = true;

        SoundEvent crack = StandVoices.heavyHit(standType, StandVoices.shadowed(data.stand));
        if (crack != null) {
            level.playSound(null, target.blockPosition(), crack, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        Impacts.landed(level, target, push,
                reinforced ? ImpactSpec.HEAVY
                        : ImpactSpec.MEDIUM,
                Impacts.PLAYER);
        return true;
    }
}
