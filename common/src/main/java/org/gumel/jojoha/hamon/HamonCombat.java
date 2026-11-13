package org.gumel.jojoha.hamon;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.BonusHit;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.EnergySystem;
import org.gumel.jojoha.combat.JojohaEnchantments;
import org.gumel.jojoha.combat.JojohaUndead;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.hamon.moves.BubbleCutterMove;
import org.gumel.jojoha.hamon.moves.ForcedRippleBreathingMove;
import org.gumel.jojoha.hamon.moves.NextLineMove;
import org.gumel.jojoha.hamon.moves.RippleBarrierMove;
import org.gumel.jojoha.hamon.moves.RippleRebukeMove;
import org.gumel.jojoha.hamon.moves.RippleWalkMove;
import org.gumel.jojoha.stand.StandCombatHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HamonCombat {
    private static final float BREATHING_PER_SECOND = 1.2F;

    private static final int CALM_TICKS_REQUIRED = 60;

    private static final float STRIKE_ENERGY = 2.5F;
    private static final int STRIKE_EVERY = 10;

    private record Swing(int target, float strength, long tick) {
    }

    private static final Map<UUID, Swing> SWINGS = new HashMap<>();
    private static final Map<UUID, Long> LAST_STRIKE_PAID = new HashMap<>();
    private static final Set<UUID> BREATH_OWED = new HashSet<>();

    private static final float OVERDRIVE_BONUS = 4.5F;
    private static final float OVERDRIVE_UNDEAD_BONUS = 9.0F;

    private static final float BARRIER_DAMAGE = 5.0F;

    private static final int BARRIER_SYNC_EVERY = 5;

    private static final float CHANNEL_UNDEAD_BONUS = 2.5F;
    private static final float CHANNEL_PER_CONDUCTIVITY = 2.5F;

    private static final int BLOOM_ONE_SECOND_IN = 6;
    private static final int BLOOM_RADIUS = 4;
    private static final int BLOOM_ATTEMPTS = 10;
    private static final int BLOOM_MAX_PER_BREATH = 3;

    private static final int BREATH_HELD_EVERY = 3;
    private static final int BREATH_HELD_BACK = 2;

    private HamonCombat() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(HamonCombat::tick);
        EntityEvent.LIVING_HURT.register(HamonCombat::onHurt);
        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            HamonState.clear(entity);
            RippleRebukeMove.forget(entity.getUUID());
            NextLineMove.forget(entity.getUUID());
            SWINGS.remove(entity.getUUID());
            LAST_STRIKE_PAID.remove(entity.getUUID());
            BREATH_OWED.remove(entity.getUUID());
            return EventResult.pass();
        });

        PlayerEvent.ATTACK_ENTITY.register(
                (player, level, target, hand, hit) -> {
                    if (player instanceof ServerPlayer serverPlayer) {
                        SWINGS.put(serverPlayer.getUUID(), new Swing(target.getId(),
                                serverPlayer.getAttackStrengthScale(0.5F), level.getGameTime()));
                    }
                    return EventResult.pass();
                });
    }

    public static boolean isUndead(LivingEntity entity) {
        return JojohaUndead.is(entity);
    }

    public static float scaled(Player player, float base) {
        return base * Conductivity.multiplier(player);
    }

    private static void tick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverPlayer);
        if (!Fate.breathesHamon(data)) {
            return;
        }

        holdBreath(serverPlayer, data);

        drainBarrier(serverPlayer, data);

        mend(serverPlayer, data);
        walk(serverPlayer, data);
        BubbleCutterMove.tick(serverPlayer, data);

        int calm = StandCombatHandler.COMBAT_TIMER_TICKS - data.combatTicks;
        boolean charging = calm >= CALM_TICKS_REQUIRED
                && data.specEnergy < JojohaPlayerData.MAX_SPEC_ENERGY
                && !EnergySystem.spendingHeld(serverPlayer);

        if (data.specEnergy >= JojohaPlayerData.MAX_SPEC_ENERGY) {
            BREATH_OWED.remove(serverPlayer.getUUID());
        }
        boolean shown = charging && (!Fate.ultimate(data)
                || BREATH_OWED.contains(serverPlayer.getUUID()));

        if (shown) {
            HamonFx.aura(serverPlayer);
            HamonFx.passiveBreath(serverPlayer.serverLevel(), serverPlayer, 0.35F, 1.0F);
        } else {
            HamonFx.passiveBreathEnded(serverPlayer);
        }

        sustainedFx(serverPlayer);

        if (serverPlayer.level().getGameTime() % 20 != 0 || !charging) {
            return;
        }

        data.specEnergy = Math.min(JojohaPlayerData.MAX_SPEC_ENERGY,
                data.specEnergy + BREATHING_PER_SECOND);
        PlayerDataAccess.set(serverPlayer, data);

        bloom(serverPlayer, data);
    }

    private static void drainBarrier(ServerPlayer player, JojohaPlayerData data) {
        if (HamonState.barrierRunOut(player)) {
            RippleBarrierMove.ranOut(player);
            return;
        }
        if (!HamonState.hasBarrier(player)) {
            return;
        }

        data.specEnergy = Math.max(0F,
                data.specEnergy - RippleBarrierMove.DRAIN_PER_TICK);
        HamonMastery.spent(player, data, RippleBarrierMove.DRAIN_PER_TICK);
        PlayerDataAccess.set(player, data);

        if (player.tickCount % BARRIER_SYNC_EVERY == 0) {
            PlayerDataAccess.sync(player);
        }

        if (data.specEnergy <= 0F) {
            RippleBarrierMove.drop(player);
            PlayerDataAccess.sync(player);
        }
    }

    private static void sustainedFx(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();

        if (HamonState.isChannelling(player) && !player.getMainHandItem().isEmpty()
                && now % CHANNEL_FX_EVERY == 0) {
            Vec3 hand = HamonFx.mainHand(player);

            HamonFx.fineMote(level, hand.x, hand.y, hand.z, 1, CHANNEL_FX_SPREAD);

            if (now % (CHANNEL_FX_EVERY * 3L) == 0) {
                HamonFx.fineSpark(level, hand.x, hand.y, hand.z, 1, CHANNEL_FX_SPREAD, 0.01);
            }
        }

        if (HamonState.hasBarrier(player) && now % BARRIER_FX_EVERY == 0) {
            for (int i = 0; i < BARRIER_FX_POINTS; i++) {
                double angle = (Math.PI * 2 * ((now / BARRIER_FX_EVERY + i * 3L) % 12L)) / 12.0;
                HamonFx.spark(level,
                        player.getX() + Math.cos(angle) * BARRIER_FX_RADIUS,
                        player.getY() + 1.0,
                        player.getZ() + Math.sin(angle) * BARRIER_FX_RADIUS,
                        1, 0.0, 0.0);
            }
        }
    }

    private static final int CHANNEL_FX_EVERY = 3;

    private static final double CHANNEL_FX_SPREAD = 0.09;
    private static final int BARRIER_FX_EVERY = 4;
    private static final int BARRIER_FX_POINTS = 2;
    private static final double BARRIER_FX_RADIUS = 1.2;

    private static void bloom(ServerPlayer player, JojohaPlayerData data) {
        if (!data.hasHamonPath(HamonPaths.HERMIT.id())) {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (level.random.nextInt(BLOOM_ONE_SECOND_IN) != 0) {
            return;
        }

        int grown = 0;
        for (int attempt = 0; attempt < BLOOM_ATTEMPTS && grown < BLOOM_MAX_PER_BREATH; attempt++) {
            BlockPos pos = player.blockPosition().offset(
                    level.random.nextInt(BLOOM_RADIUS * 2 + 1) - BLOOM_RADIUS,
                    level.random.nextInt(3) - 1,
                    level.random.nextInt(BLOOM_RADIUS * 2 + 1) - BLOOM_RADIUS);

            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BonemealableBlock growable)
                    || !growable.isValidBonemealTarget(level, pos, state)) {
                continue;
            }
            if (!growable.isBonemealSuccess(level, level.random, pos, state)) {
                continue;
            }

            growable.performBonemeal(level, level.random, pos, state);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 6, 0.35, 0.3, 0.35, 0.0);
            HamonFx.mote(level, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 3, 0.35);
            grown++;
        }
    }

    private static void holdBreath(ServerPlayer player, JojohaPlayerData data) {
        if (player.getAirSupply() <= 0 || player.getAirSupply() >= player.getMaxAirSupply()) {
            return;
        }
        if (player.tickCount % BREATH_HELD_EVERY != 0) {
            return;
        }
        if (!data.hasHamonPath(HamonPaths.HERMIT.id())) {
            return;
        }
        if (!player.isEyeInFluid(FluidTags.WATER)) {
            return;
        }

        player.setAirSupply(Math.min(player.getMaxAirSupply(),
                player.getAirSupply() + BREATH_HELD_BACK));
    }

    private static boolean dealingHamonDamage;

    private static void hurtWithHamon(LivingEntity victim, DamageSource source, float amount) {
        dealingHamonDamage = true;
        try {
            BonusHit.land(victim, source, DamageScale.hamon(source.getEntity(), victim, amount));
        } finally {
            dealingHamonDamage = false;
        }
    }

    private static void mend(ServerPlayer player, JojohaPlayerData data) {
        if (!HamonState.isBreathing(player)) {
            return;
        }

        data.specEnergy -=
                ForcedRippleBreathingMove.DRAIN_PER_TICK;
        HamonMastery.spent(player, data, ForcedRippleBreathingMove.DRAIN_PER_TICK);
        if (data.specEnergy <= 0F) {
            data.specEnergy = 0F;
            PlayerDataAccess.set(player, data);
            ForcedRippleBreathingMove.stop(player);
            PlayerDataAccess.sync(player);
            return;
        }
        PlayerDataAccess.set(player, data);

        if (player.tickCount % BARRIER_SYNC_EVERY == 0) {
            PlayerDataAccess.sync(player);
        }

        HamonFx.chargeLoop(player.serverLevel(), player, 0.5F, 1.2F);

        if (player.tickCount
                % ForcedRippleBreathingMove.BEAT_TICKS == 0) {
            ForcedRippleBreathingMove.beat(player);
        }
    }

    private static void walk(ServerPlayer player, JojohaPlayerData data) {
        if (!HamonState.isWalking(player)) {
            return;
        }

        data.specEnergy -= RippleWalkMove.DRAIN_PER_TICK;
        HamonMastery.spent(player, data, RippleWalkMove.DRAIN_PER_TICK);
        if (data.specEnergy <= 0F) {
            data.specEnergy = 0F;
            HamonState.setWalking(player, false);
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            return;
        }
        PlayerDataAccess.set(player, data);
        if (player.tickCount % BARRIER_SYNC_EVERY == 0) {
            PlayerDataAccess.sync(player);
        }

    }

    private static EventResult onHurt(LivingEntity victim, DamageSource source, float amount) {
        if (dealingHamonDamage) {
            return EventResult.pass();
        }

        if (source.getEntity() instanceof ServerPlayer attacker) {
            struck(attacker, victim, source);
        }

        if (victim instanceof ServerPlayer defender && HamonState.hasBarrier(defender)) {
            if (source.getEntity() instanceof LivingEntity assailant
                    && source.getDirectEntity() == assailant
                    && isUndead(assailant)) {
                hurtWithHamon(assailant,
                        defender.serverLevel().damageSources().playerAttack(defender),
                        BARRIER_DAMAGE);
            }

            if (source.getDirectEntity() != null) {
                ServerLevel barrierLevel = defender.serverLevel();
                Vec3 at = source.getDirectEntity().position();
                HamonFx.spark(barrierLevel, at.x, at.y + 1.0, at.z, 8, 0.3, 0.05);
                HamonFx.ripple(barrierLevel, at.x, at.y + 1.0, at.z, 3);
                HamonFx.ring(barrierLevel, at.x, at.y + 1.0, at.z, 1.2, 1);
                return EventResult.interruptFalse();
            }
        }

        if (victim instanceof ServerPlayer guard
                && RippleRebukeMove.rebuke(guard, source)) {
            return EventResult.interruptFalse();
        }

        if (victim instanceof ServerPlayer reader
                && NextLineMove.dodges(reader, source)) {
            return EventResult.interruptFalse();
        }

        return EventResult.pass();
    }

    public static void owed(ServerPlayer player) {
        BREATH_OWED.add(player.getUUID());
    }

    private static float meleeRefund(ServerPlayer attacker, LivingEntity victim, DamageSource source) {
        Swing swing = SWINGS.get(attacker.getUUID());
        long now = attacker.level().getGameTime();
        if (swing == null || swing.tick() != now || swing.target() != victim.getId()
                || source.getDirectEntity() != attacker) {
            return 0F;
        }
        SWINGS.remove(attacker.getUUID());

        Long paid = LAST_STRIKE_PAID.get(attacker.getUUID());
        if (paid != null && now - paid < STRIKE_EVERY) {
            return 0F;
        }
        LAST_STRIKE_PAID.put(attacker.getUUID(), now);
        return STRIKE_ENERGY * swing.strength();
    }

    private static void struck(ServerPlayer attacker, LivingEntity victim, DamageSource source) {
        JojohaPlayerData data = PlayerDataAccess.get(attacker);
        if (!Fate.breathesHamon(data)) {
            return;
        }

        float refund = meleeRefund(attacker, victim, source);
        if (refund > 0F && data.specEnergy < JojohaPlayerData.MAX_SPEC_ENERGY) {
            data.specEnergy = Math.min(JojohaPlayerData.MAX_SPEC_ENERGY,
                    data.specEnergy + refund);
            PlayerDataAccess.set(attacker, data);
        }

        if (HamonState.isChannelling(attacker) && isUndead(victim)) {
            float channelled = scaled(attacker, CHANNEL_UNDEAD_BONUS
                    + JojohaEnchantments.conductivityLevel(attacker)
                            * CHANNEL_PER_CONDUCTIVITY);
            ServerLevel channelLevel = attacker.serverLevel();
            hurtWithHamon(victim, channelLevel.damageSources().playerAttack(attacker), channelled);
            HamonFx.spark(channelLevel, victim.getX(), victim.getY() + 1.0, victim.getZ(),
                    8, 0.3, 0.05);
            HamonFx.ring(channelLevel, victim.getX(), victim.getY() + 1.0, victim.getZ(), 1.0, 0);
        }

        if (!HamonState.hasOverdrive(attacker)) {
            return;
        }

        float bonus = scaled(attacker, isUndead(victim) ? OVERDRIVE_UNDEAD_BONUS : OVERDRIVE_BONUS);
        HamonState.consumeOverdrive(attacker);

        ServerLevel level = attacker.serverLevel();
        hurtWithHamon(victim, level.damageSources().playerAttack(attacker), bonus);
        Impacts.landed(level, victim, victim.position().subtract(attacker.position()),
                ImpactSpec.HEAVY, Impacts.HAMON);

        HamonFx.spark(level, victim.getX(), victim.getY() + 1.0, victim.getZ(), 14, 0.35, 0.08);
        HamonFx.ripple(level, victim.getX(), victim.getY() + 1.0, victim.getZ(), 5);
        HamonFx.ring(level, victim.getX(), victim.getY() + 1.0, victim.getZ(), 1.6, 2);
        level.playSound(null, victim.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(),
                SoundSource.PLAYERS, 0.5F, 1.9F);
        HamonFx.sound(level, attacker, 1.0F, 0.9F);
    }
}
