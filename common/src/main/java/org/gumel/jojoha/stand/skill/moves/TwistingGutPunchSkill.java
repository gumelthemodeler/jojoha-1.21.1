package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class TwistingGutPunchSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "twisting_gut_punch");

    public static final TwistingGutPunchSkill INSTANCE = new TwistingGutPunchSkill();

    private static final int COOLDOWN_TICKS = 70;
    private static final float ENERGY_COST = EnergyWeight.HEAVY.cost();

    private static final double REACH = 11.0;
    private static final double FORGIVENESS = 2.0;

    private static final int REACH_TICKS = 5;

    private static final int WIND_TICKS = 6;

    private static final int PULL_TICKS = 10;

    private static final float DAMAGE = 6.0F;
    private static final double PUNCH_AWAY = 0.85;
    private static final double PUNCH_UP = 0.28;
    private static final int WINDED_TICKS = 45;

    private TwistingGutPunchSkill() {
    }

    public static void init() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.twisting_gut_punch";
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
        LivingEntity caught = look(player);

        ServerLevel level = player.serverLevel();
        if (caught == null) {
            vine(level, player.getEyePosition(), Whiff.thrownTo(player, REACH));

            MovementFx.pose(player,
                    MovementFx.TWISTING_GUT_PUNCH_START);
            Whiff.stand(player);
            return true;
        }

        VineHaul.begin(player, caught, new VineHaul.Beats(REACH_TICKS, PULL_TICKS, WIND_TICKS,
                TwistingGutPunchSkill::took, TwistingGutPunchSkill::wound,
                TwistingGutPunchSkill::punch, TwistingGutPunchSkill::lost));

        NetworkHandler.sendThornLash(level, player, caught,
                REACH_TICKS + PULL_TICKS + WIND_TICKS + 2, REACH_TICKS);

        vine(level, player.getEyePosition(),
                caught.position().add(0, caught.getBbHeight() * 0.5, 0));

        MovementFx.hold(player,
                MovementFx.TWISTING_GUT_PUNCH_START);
        return true;
    }

    private static void vine(ServerLevel level, Vec3 from, Vec3 to) {
        int steps = 10;
        Vec3 step = to.subtract(from).scale(1.0 / steps);

        for (int i = 2; i <= steps; i += 3) {
            Vec3 at = from.add(step.scale(i));
            level.sendParticles(ModRegistries.STAND_AURA.get(), at.x, at.y, at.z, 1,
                    0.08, 0.08, 0.08, 0.0);
        }

        level.playSound(null, BlockPos.containing(from), SoundEvents.WEEPING_VINES_PLACE,
                SoundSource.PLAYERS, 0.85F, 1.25F);
    }

    private static void took(ServerPlayer holder, LivingEntity target) {
        holder.serverLevel().playSound(null, target.blockPosition(),
                SoundEvents.WEEPING_VINES_BREAK, SoundSource.PLAYERS, 0.7F, 0.8F);
    }

    private static void wound(ServerPlayer holder, LivingEntity target) {
        MovementFx.pose(holder,
                MovementFx.TWISTING_GUT_PUNCH_END);
    }

    private static void lost(ServerPlayer holder, LivingEntity target) {
        MovementFx.drop(holder);
        NetworkHandler.sendThornLash(holder.serverLevel(), holder, target, 0);
    }

    private static void punch(ServerPlayer holder, LivingEntity target) {
        ServerLevel level = holder.serverLevel();
        JojohaPlayerData data = PlayerDataAccess.get(holder);
        Vec3 gut = target.position().add(0, target.getBbHeight() * 0.45, 0);

        float damage = StandPassives.scaleOutgoing(
                holder, data, target, DAMAGE * data.stand.powerScale());
        target.hurt(level.damageSources().playerAttack(holder), DamageScale.stand(holder, target, damage));

        Vec3 away = target.position().subtract(holder.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        Vec3 push = flat.lengthSqr() < 1.0E-4 ? holder.getLookAngle() : flat.normalize();

        target.setDeltaMovement(push.x * PUNCH_AWAY, PUNCH_UP, push.z * PUNCH_AWAY);
        target.hurtMarked = true;

        target.addEffect(new MobEffectInstance(ModEffects.stun(), WINDED_TICKS, Stun.TIER_TWO,
                false, false, true));

        NetworkHandler.sendThornLash(level, holder, target, 0);

        Impacts.landed(level, gut, push, ImpactSpec.HEAVY, Impacts.colourOf(holder));
        level.sendParticles(ParticleTypes.CLOUD, gut.x, gut.y, gut.z, 10, 0.25, 0.2, 0.25, 0.14);
    }

    private static LivingEntity look(ServerPlayer player) {
        return VineHaul.look(player, REACH, FORGIVENESS);
    }
}
