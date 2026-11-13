package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
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
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class ThornWhipSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "thorn_whip");

    public static final ThornWhipSkill INSTANCE = new ThornWhipSkill();

    private static final int COOLDOWN_TICKS = 45;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private static final double REACH = 13.0;
    private static final double FORGIVENESS = 2.2;

    private static final float DAMAGE = 4.2F;

    private static final double THROW_AWAY = 3.6;
    private static final double THROW_UP = 0.72;

    private static final int HAUL_TICKS = 8;

    private static final int STUN_TICKS = 25;

    private static final int LASH_STEPS = 10;

    private ThornWhipSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.thorn_whip";
    }

    @Override
    public ResourceLocation replaces() {
        return LassoOfThornsSkill.ID;
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
        MovementFx.pose(player, MovementFx.THROW);
        StandBeat.at(MovementFx.THROW_RELEASE_TICKS, player, () -> crack(player));
        return true;
    }

    private static void crack(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        LivingEntity caught = look(player);
        if (caught == null) {
            lash(level, player.getEyePosition(), Whiff.thrownTo(player, REACH));
            Whiff.stand(player);
            return;
        }

        Vec3 from = player.getEyePosition();
        Vec3 at = caught.position().add(0, caught.getBbHeight() * 0.5, 0);

        NetworkHandler.sendThornLash(level, player, caught,
                HAUL_TICKS + 2);
        lash(level, from, at);

        VineHaul.begin(player, caught, HAUL_TICKS, ThornWhipSkill::fling);
    }

    private static void fling(ServerPlayer player, LivingEntity caught) {
        ServerLevel level = player.serverLevel();
        JojohaPlayerData data = PlayerDataAccess.get(player);

        float damage = StandPassives.scaleOutgoing(
                player, data, caught, DAMAGE * data.stand.powerScale());
        caught.hurt(level.damageSources().playerAttack(player), DamageScale.stand(player, caught, damage));

        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 push = flat.lengthSqr() < 1.0E-4 ? player.getLookAngle() : flat.normalize();

        caught.setDeltaMovement(push.x * THROW_AWAY, THROW_UP, push.z * THROW_AWAY);
        caught.hurtMarked = true;

        caught.addEffect(new MobEffectInstance(ModEffects.stun(), STUN_TICKS, Stun.TIER_TWO,
                false, false, true));

        Impacts.landed(level, caught, push, ImpactSpec.HEAVY, Impacts.colourOf(player));
    }

    private static void lash(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from).scale(1.0 / LASH_STEPS);

        for (int i = 2; i <= LASH_STEPS; i += 3) {
            Vec3 at = from.add(step.scale(i));
            level.sendParticles(ModRegistries.STAND_AURA.get(), at.x, at.y, at.z, 1,
                    0.08, 0.08, 0.08, 0.0);
        }

        level.playSound(null, BlockPos.containing(from),
                SoundEvents.WEEPING_VINES_PLACE, SoundSource.PLAYERS, 0.9F, 1.4F);
    }

    private static LivingEntity look(ServerPlayer player) {
        return VineHaul.look(player, REACH, FORGIVENESS);
    }
}
