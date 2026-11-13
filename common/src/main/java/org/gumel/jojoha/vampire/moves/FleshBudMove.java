package org.gumel.jojoha.vampire.moves;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.VampireMarkPacket;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.vampire.VampireAim;
import org.gumel.jojoha.vampire.VampireFx;
import org.gumel.jojoha.vampire.VampireMove;
import org.gumel.jojoha.vampire.VampirePoses;
import org.gumel.jojoha.vampire.VampireState;

public final class FleshBudMove implements VampireMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "flesh_bud");
    public static final FleshBudMove INSTANCE = new FleshBudMove();

    private static final double REACH = 5.0;
    private static final double RADIUS = 1.0;

    private static final int BUD_TICKS = 1200;

    private static final float DETONATE_DAMAGE = 10.0F;
    private static final double DETONATE_RADIUS = 3.5;
    private static final float DETONATE_SPLASH = 6.0F;

    private static final int COOLDOWN_TICKS = 200;
    private static final float ENERGY_COST = 30F;

    public static final int CONTACT_TICKS = 18;

    private static final double SLACK = 1.5;

    private FleshBudMove() {
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
    public VampireStage minimumStage() {
        return VampireStage.VAMPIRE;
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        LivingEntity target = VampireAim.firstAlong(player, REACH, RADIUS);

        if (target == null) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.flesh_bud.nothing"), true);
            return;
        }

        boolean setOff = VampireState.hasBud(target);
        if (setOff && !VampireState.plantedBy(target, player)) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.flesh_bud.not_yours"), true);
            return;
        }

        MovementFx.pose(player, VampirePoses.FLESH_BUD);
        StandBeat.after(CONTACT_TICKS, player, target, (caster, body) -> {
            if (caster.distanceTo(body) > REACH + SLACK || VampireState.hasBud(body) != setOff) {
                return;
            }
            if (setOff) {
                if (VampireState.plantedBy(body, caster)) {
                    detonate(caster.serverLevel(), caster, body);
                }
            } else {
                plant(caster.serverLevel(), caster, body);
            }
        });
    }

    private void plant(ServerLevel level, ServerPlayer player, LivingEntity target) {
        VampireState.plantBud(target, player, BUD_TICKS);

        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.setPersistenceRequired();
        } else if (target instanceof Player victim) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BUD_TICKS, 1,
                    false, true, true));
            victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, BUD_TICKS, 1,
                    false, true, true));
            victim.displayClientMessage(
                    Component.translatable("message.jojoha.flesh_bud.planted_on_you"), true);
        }

        NetworkHandler.sendVampireMark(level, target,
                VampireMarkPacket.Mark.BUD, BUD_TICKS);

        VampireFx.blood(level, target.getX(), target.getY() + target.getBbHeight() * 0.6,
                target.getZ(), 6, 0.18, 0.01);
        VampireFx.ring(level, target.getX(), target.getY() + target.getBbHeight() * 0.6,
                target.getZ(), 0.8, 0);

        level.sendParticles(ModRegistries.BLOOD_MOTE.get(),
                target.getX(), target.getY() + 1.2, target.getZ(), 20, 0.3, 0.4, 0.3, 0.01);
        level.playSound(null, target.blockPosition(), SoundEvents.SLIME_SQUISH,
                SoundSource.PLAYERS, 0.8F, 0.6F);
    }

    private void detonate(ServerLevel level, ServerPlayer player, LivingEntity target) {
        VampireState.removeBud(target);
        NetworkHandler.sendVampireMark(level, target,
                VampireMarkPacket.Mark.BUD, 0);

        target.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, target, DETONATE_DAMAGE));
        Impacts.landed(level, target, new Vec3(0.0, 1.0, 0.0), ImpactSpec.HEAVY.silent(), Impacts.BLOOD);

        for (LivingEntity bystander : level.getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(DETONATE_RADIUS),
                entity -> entity != target && entity.isAlive())) {
            bystander.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, bystander, DETONATE_SPLASH));
            Impacts.landed(level, bystander, bystander.position().subtract(target.position()),
                    ImpactSpec.MEDIUM.silent(), Impacts.BLOOD);
        }

        for (int i = 0; i < 3; i++) {
            VampireFx.ring(level, target.getX(), target.getY() + 0.9, target.getZ(),
                    1.4 + i * 1.1, 2);
        }
        VampireFx.blood(level, target.getX(), target.getY() + 0.9, target.getZ(), 40, 0.5, 0.35);

        level.sendParticles(ParticleTypes.EXPLOSION,
                target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.6, 0.6, 0.6, 0.0);
        level.sendParticles(ModRegistries.BLOOD_MOTE.get(),
                target.getX(), target.getY() + 1.0, target.getZ(), 60, 1.2, 1.2, 1.2, 0.06);
        level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 1.0F, 1.4F);
    }
}
