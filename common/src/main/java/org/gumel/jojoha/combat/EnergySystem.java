package org.gumel.jojoha.combat;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.accessory.Charms;
import org.gumel.jojoha.accessory.Relics;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandGrowth;
import org.gumel.jojoha.data.StatEffects;
import org.gumel.jojoha.data.StatPerk;
import org.gumel.jojoha.hamon.HamonState;
import org.gumel.jojoha.item.Curios;
import org.gumel.jojoha.pillar.PillarMen;
import org.gumel.jojoha.speech.Speech;
import org.gumel.jojoha.stand.StandAuraEffect;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.skill.Heading;
import org.gumel.jojoha.stand.skill.TimeStopSystem;
import org.gumel.jojoha.stand.skill.moves.TimeStopSkill;
import org.gumel.jojoha.trait.TraitEffects;
import org.gumel.jojoha.ultimate.AdaptiveFlightMove;
import org.gumel.jojoha.vampire.VampireState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EnergySystem {
    private static final float SPEC_REGEN_PER_TICK = JojohaPlayerData.MAX_SPEC_ENERGY / (15 * 20);
    private static final float SPEC_FIGHT_REGEN_PER_TICK = JojohaPlayerData.MAX_SPEC_ENERGY / (60 * 20);

    private static final int STAND_REGEN_SECONDS = 35;
    private static final int STAND_FIGHT_REGEN_SECONDS = 130;

    public static final int STAND_DRAIN_COMBAT_SECONDS = 110;

    public static final int STAND_DRAIN_IDLE_SECONDS = 300;

    public static final float SUMMON_COST_FRACTION = 0.10F;
    public static final float SUMMON_MINIMUM_FRACTION = 0.25F;
    public static final int RESUMMON_COOLDOWN_TICKS = 40;

    public static final float STAND_BLOW_RESTORE = 0.5F;

    public static final float SPENT_FRACTION = 0.01F;

    public static final int RECOVERY_SECONDS = 30;
    public static final int STAND_BLOW_RESTORE_EVERY_TICKS = 10;

    private static final float SPEC_REGEN_WHILE_STAND_OUT = 0.5F;
    private static final float SPEC_REGEN_WHILE_STAND_AWAY = 1.5F;

    private static final Map<UUID, Long> AWAY_SINCE = new HashMap<>();
    private static final Map<UUID, Long> BLOW_RESTORED = new HashMap<>();

    private static final int SYNC_INTERVAL_TICKS = 10;

    private EnergySystem() {
    }

    private static boolean frozen;

    public static void setFrozen(boolean value) {
        frozen = value;
    }

    public static boolean frozen() {
        return frozen;
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(EnergySystem::onPlayerTick);
        PlayerEvent.PLAYER_QUIT.register(player -> {
            AWAY_SINCE.remove(player.getUUID());
            BLOW_RESTORED.remove(player.getUUID());
        });
    }

    public static boolean inCombat(JojohaPlayerData data) {
        return data.combatTicks > 0;
    }

    public static void standWentAway(ServerPlayer player) {
        AWAY_SINCE.put(player.getUUID(), player.level().getGameTime());
    }

    public static int recallWaitTicks(ServerPlayer player) {
        Long since = AWAY_SINCE.get(player.getUUID());
        long now = player.level().getGameTime();
        if (since == null || now < since) {
            return 0;
        }
        return (int) Math.max(0, RESUMMON_COOLDOWN_TICKS - (now - since));
    }

    public static float summonCost(JojohaPlayerData data) {
        return data.maxStandEnergy() * SUMMON_COST_FRACTION;
    }

    public static boolean canAffordSummon(JojohaPlayerData data) {
        return data.standEnergy >= data.maxStandEnergy() * SUMMON_MINIMUM_FRACTION;
    }

    public static void standBlowLanded(ServerPlayer player, JojohaPlayerData data) {
        long now = player.level().getGameTime();
        Long paid = BLOW_RESTORED.get(player.getUUID());
        if (paid != null && now >= paid && now - paid < STAND_BLOW_RESTORE_EVERY_TICKS) {
            return;
        }
        BLOW_RESTORED.put(player.getUUID(), now);
        float restore = STAND_BLOW_RESTORE * (StatPerk.BOUNDLESS.has(data)
                ? StatPerkEffects.BOUNDLESS_RESTORE_SCALE : 1F);
        restoreStand(data, restore);
    }

    public static float spentAt(JojohaPlayerData data) {
        return data.maxStandEnergy() * SPENT_FRACTION;
    }

    public static void restoreStand(JojohaPlayerData data, float amount) {
        if (data.standSummoned && data.standEnergy <= spentAt(data)) {
            return;
        }
        data.standEnergy = Math.min(data.maxStandEnergy(), data.standEnergy + amount);
    }

    private static boolean recovering(ServerPlayer player, JojohaPlayerData data) {
        return data.halfSummon && data.standGuarding;
    }

    private static void onPlayerTick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverPlayer);
        if (Float.isNaN(data.standEnergy)) {
            data.standEnergy = 0F;
        }

        if (serverPlayer.isSpectator() && (data.standSummoned || data.standPiloting)) {
            StandSummonHandler.dismissImmediately(serverPlayer, data);
            data.standPiloting = false;
            PlayerDataAccess.set(serverPlayer, data);
            PlayerDataAccess.sync(serverPlayer);
            return;
        }

        if (frozen) {
            data.specEnergy = JojohaPlayerData.MAX_SPEC_ENERGY;
            data.standEnergy = data.maxStandEnergy();
        }

        trackMovement(serverPlayer, data);
        tickLeapGrabWindow(data);
        tickInhaleIFrames(data);
        tickTimeStopFightUses(serverPlayer, data);

        if (data.specEnergy < JojohaPlayerData.MAX_SPEC_ENERGY && !spendingHeld(serverPlayer)) {
            float regen = data.combatTicks > 0 ? SPEC_FIGHT_REGEN_PER_TICK : SPEC_REGEN_PER_TICK;
            data.specEnergy = Math.min(JojohaPlayerData.MAX_SPEC_ENERGY,
                    data.specEnergy + regen * StatEffects.regenScale(data)
                            * disciplineRegenScale(data)
                            * Relics.breathScale(serverPlayer, data)
                            * Curios.breathScale(serverPlayer)
                            * TraitEffects.breathScale(data));
        }

        if (data.combatTicks > 0) {
            data.combatTicks--;
        }
        tickGuardBreakCooldown(serverPlayer, data);
        StandCombatHandler.tickGuardRecovery(serverPlayer, data);

        boolean sustainingManifestation = data.standSummoned && data.stand.trust().manifestsEntity()
                && StandTypes.hasBody(data);

        if (sustainingManifestation) {
            StandEntity stand = StandSummonHandler.findStand(serverPlayer, data);
            if (stand != null && stand.hasOutlivedEmergingWindow() && !inCombat(data)) {
                data.standEnergy = 0F;
                STAND_MISSING.remove(serverPlayer.getUUID());
                StandSummonHandler.dismiss(serverPlayer, data);
                PlayerDataAccess.set(serverPlayer, data);
                PlayerDataAccess.sync(serverPlayer);
                return;
            }

            if (stand == null) {
                int missing = STAND_MISSING.merge(serverPlayer.getUUID(), 1, Integer::sum);
                if (missing > STAND_MISSING_GRACE_TICKS) {
                    STAND_MISSING.remove(serverPlayer.getUUID());
                    StandSummonHandler.dismiss(serverPlayer, data);
                    PlayerDataAccess.set(serverPlayer, data);
                    PlayerDataAccess.sync(serverPlayer);
                    return;
                }
            } else {
                STAND_MISSING.remove(serverPlayer.getUUID());
            }

            if (serverPlayer.tickCount % 20 == 0) {
                StandGrowth.heldForm(serverPlayer);
            }

            float drain = perTick(data.maxStandEnergy(),
                    inCombat(data) ? JojohaServerConfig.STAND_DRAIN_COMBAT.whole()
                            : JojohaServerConfig.STAND_DRAIN_IDLE.whole())
                    * data.manifestTier().energyDrainMultiplier()
                    * Heaven.drainScale(data);
            if (StatPerk.UNWAVERING_FORM.has(data)) {
                drain *= StatPerkEffects.UNWAVERING_FORM_DRAIN_SCALE;
            }
            if (StatPerk.UNBREAKABLE_FORM.has(data)) {
                drain = Math.min(drain, Math.max(0F,
                        data.standEnergy - data.maxStandEnergy() * StatPerkEffects.UNBREAKABLE_FORM_RESERVE));
            }
            drain *= Charms.drainScale(serverPlayer);
            if (Charms.freeUtility(serverPlayer, data)) {
                drain = 0F;
            }
            boolean recovering = recovering(serverPlayer, data);
            if (data.halfSummon && (!inCombat(data) || recovering)) {
                drain = 0F;
            }
            float window = emergingDrain(data, stand);
            data.standEnergy = Math.max(0F, data.standEnergy - Math.max(drain, window));
            if (recovering && window <= 0F && data.standEnergy > spentAt(data)) {
                int seconds = JojohaServerConfig.HALF_SUMMON_RECOVERY.whole();
                if (seconds > 0) {
                    data.standEnergy = Math.min(data.maxStandEnergy(),
                            data.standEnergy + perTick(data.maxStandEnergy(), seconds));
                }
            }

            if (data.standEnergy <= spentAt(data)) {
                StandSummonHandler.collapse(serverPlayer, data);
                Speech.spent(serverPlayer);
                STAND_MISSING.remove(serverPlayer.getUUID());
                PlayerDataAccess.set(serverPlayer, data);
                PlayerDataAccess.sync(serverPlayer);
                return;
            }
        } else if (data.standEnergy < data.maxStandEnergy() && recallWaitTicks(serverPlayer) <= 0) {
            data.standEnergy = Math.min(data.maxStandEnergy(),
                    data.standEnergy + perTick(data.maxStandEnergy(),
                            data.combatTicks > 0 ? STAND_FIGHT_REGEN_SECONDS : STAND_REGEN_SECONDS)
                            * (StatPerk.WELLSPRING.has(data)
                                    ? StatPerkEffects.WELLSPRING_REGEN_SCALE : 1F)
                            * (data.combatTicks <= 0 && StatPerk.STEADY_FORM.has(data)
                                    ? StatPerkEffects.STEADY_FORM_REGEN_SCALE : 1F)
                            * StatEffects.regenScale(data)
                            * Curios.standRegenScale(serverPlayer));
        }

        if (data.standSummoned) {
            StandAuraEffect.tick(serverPlayer);
        }

        PlayerDataAccess.set(serverPlayer, data);

        if (serverPlayer.level().getGameTime() % SYNC_INTERVAL_TICKS == 0) {
            PlayerDataAccess.sync(serverPlayer);
        } else {
            PlayerDataAccess.flushSoon(serverPlayer);
        }
    }

    public static boolean spendingHeld(ServerPlayer player) {
        return HamonState.hasBarrier(player)
                || HamonState.isBreathing(player)
                || HamonState.isWalking(player)
                || HamonState.isFiring(player)
                || VampireState.isCrawling(player)
                || AdaptiveFlightMove.gliding(player)
                || PillarMen.holding(player);
    }

    private static final int STAND_MISSING_GRACE_TICKS = 10;
    private static final Map<UUID, Integer> STAND_MISSING = new HashMap<>();

    private static float emergingDrain(JojohaPlayerData data, StandEntity stand) {
        if (stand == null || !stand.getTrustTier().isTimeLimited()
                || TimeStopSystem.freezes(stand)) {
            return 0F;
        }
        if (inCombat(data)) {
            return 0F;
        }
        int left = Math.max(0, TrustTier.EMERGING_DURATION_TICKS - stand.tickCount);
        return data.standEnergy / (left + 1);
    }

    static float disciplineRegenScale(JojohaPlayerData data) {
        if (!inCombat(data) || !data.stand.isPresent() || !data.stand.trust().manifestsEntity()) {
            return 1F;
        }
        return data.standSummoned ? SPEC_REGEN_WHILE_STAND_OUT : SPEC_REGEN_WHILE_STAND_AWAY;
    }

    private static float perTick(float pool, int seconds) {
        return pool / (seconds * 20F);
    }

    private static void trackMovement(ServerPlayer player, JojohaPlayerData data) {
        double dx = player.getX() - data.lastTickX;
        double dz = player.getZ() - data.lastTickZ;
        data.lastTickX = player.getX();
        data.lastTickZ = player.getZ();

        double travelledSqr = dx * dx + dz * dz;

        if (travelledSqr > TELEPORT_THRESHOLD_SQR) {
            return;
        }

        if (travelledSqr > MOVING_THRESHOLD_SQR) {
            data.recentMoveX = dx;
            data.recentMoveZ = dz;
            data.recentMoveTicks = MOVE_MEMORY_TICKS;
        } else if (data.recentMoveTicks > 0) {
            data.recentMoveTicks--;
        }
    }

    private static final double MOVING_THRESHOLD_SQR =
            Heading.MOVING_THRESHOLD
                    * Heading.MOVING_THRESHOLD;
    private static final int MOVE_MEMORY_TICKS = 6;
    private static final double TELEPORT_THRESHOLD_SQR = 4.0;

    private static void tickTimeStopFightUses(ServerPlayer player, JojohaPlayerData data) {
        if (TimeStopSkill.shouldClearFightUses(
                data, player.level().getGameTime())) {
            data.timeStopUsesThisFight = 0;
        }
    }

    private static void tickInhaleIFrames(JojohaPlayerData data) {
        if (data.inhaleIFrameTicks > 0) {
            data.inhaleIFrameTicks--;
        }
    }

    private static void tickLeapGrabWindow(JojohaPlayerData data) {
        if (data.standLeapGrabTicks > 0) {
            data.standLeapGrabTicks--;
        }
    }

    private static void tickGuardBreakCooldown(ServerPlayer player, JojohaPlayerData data) {
        if (data.guardBreakCooldownTicks <= 0) {
            return;
        }
        data.guardBreakCooldownTicks--;
        if (data.guardBreakCooldownTicks == 0 && data.summonedStandEntityUuid != null) {
            Entity entity = player.serverLevel().getEntity(data.summonedStandEntityUuid);
            if (entity instanceof StandEntity stand) {
                stand.recoverFromGuardBreak();
            }
        }
    }
}
