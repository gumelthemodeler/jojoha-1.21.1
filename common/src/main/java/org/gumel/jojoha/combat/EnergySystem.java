package org.gumel.jojoha.combat;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.StandAuraEffect;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;

/**
 * Passive regeneration for the combat bar's spec/stand energy pools, and Stand energy drain
 * while a Stand is summoned - per the design doc's Stand Balance section, drain is "extremely
 * little" out of combat and "far more evident" in active combat. {@code combatTicks} (refreshed
 * whenever the player deals or receives damage - see the {@code EntityEvent.LIVING_HURT} hook
 * in {@code Jojoha.init()}) is what distinguishes the two. Also ticks down the guard-break
 * lockout. Runs server-side only (gated on {@code ServerPlayer} so the client-side tick of the
 * same event, which fires for the client's own player instance, is ignored) - the server is the
 * sole source of truth, mirrored to the client via the periodic sync below.
 */
public final class EnergySystem {
    // Full regen from empty in 15 seconds.
    private static final float SPEC_REGEN_PER_TICK = JojohaPlayerData.MAX_SPEC_ENERGY / (15 * 20);
    // Stand rates are expressed as how long a *full* pool takes rather than as a fixed amount per
    // tick, because the pool itself grows with trust. Holding the durations constant is the point:
    // a Bonded Stand should last as long per cast as a Partial one but be able to spend far more on
    // moves in that time, which is what a bigger pool at the same drain rate actually means.
    private static final int STAND_REGEN_SECONDS = 15;

    /**
     * How long a full pool lasts while the Stand is simply out, and while it is fighting.
     *
     * <p>The combat figure was twenty seconds, and it was the reason a Stand seemed to vanish the
     * moment anything started. Twenty is the whole pool, and the combat timer is refreshed by
     * <em>any</em> damage dealt or taken and runs five seconds from each - so a fight of any length
     * keeps the fast drain switched on continuously. Twenty seconds of Stand per fight is not a
     * resource to manage, it is a countdown. A minute is long enough to see a fight through and
     * still short enough that leaving it out costs something.
     *
     * <p>Both are expressed as how long a <em>full</em> pool takes rather than as an amount per
     * tick, because the pool grows with trust. Holding the durations constant is the point: a
     * Bonded Stand should last as long per cast as a Partial one and be able to spend far more on
     * moves within that time, which is what a bigger pool at the same rate actually buys.
     */
    private static final int STAND_DRAIN_PASSIVE_SECONDS = 240;
    private static final int STAND_DRAIN_COMBAT_SECONDS = 60;

    /**
     * How often the client is told what its pools are at.
     *
     * <p>Halved, because this interval was the reason the bar looked like it lost energy in lumps.
     * The drain itself is per tick and perfectly smooth; the client simply only heard about it once
     * a second, so it redrew the same width twenty times and then jumped. Twice a second, with the
     * gauge easing between what it is told (see CentralBarOverlay), is continuous to look at.
     */
    private static final int SYNC_INTERVAL_TICKS = 10;

    private EnergySystem() {
    }

    /**
     * Debug switch: while set, both bars are held at full.
     *
     * <p>Static and server-wide, which is what a debug switch should be - it exists to keep a Stand
     * out indefinitely while something else is being tested, not to be a per-player state anyone
     * has to reason about.
     */
    private static boolean frozen;

    public static void setFrozen(boolean value) {
        frozen = value;
    }

    public static boolean frozen() {
        return frozen;
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(EnergySystem::onPlayerTick);
    }

    private static void onPlayerTick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverPlayer);

        // A Stand does not follow anybody into spectator. Checked here rather than on the mode
        // change because there is no event for one, and a Stand left standing in the world while
        // its user watches through walls is a Stand that still guards them, still blocks arrows and
        // still answers a key - all of which a spectator is not supposed to be able to do.
        if (serverPlayer.isSpectator() && (data.standSummoned || data.standPiloting)) {
            StandSummonHandler.dismissImmediately(serverPlayer, data);
            data.standPiloting = false;
            PlayerDataAccess.set(serverPlayer, data);
            PlayerDataAccess.sync(serverPlayer);
            return;
        }

        // Debug freeze: top both bars up before anything else looks at them, so a Stand can be
        // held out indefinitely. Refilling rather than suppressing each drain is deliberate -
        // energy is spent in a dozen places and a new move would quietly escape a flag, whereas
        // putting it back covers everything including moves written later.
        if (frozen) {
            data.specEnergy = JojohaPlayerData.MAX_SPEC_ENERGY;
            data.standEnergy = data.maxStandEnergy();
        }

        trackMovement(serverPlayer, data);
        tickLeapGrabWindow(data);
        tickInhaleIFrames(data);
        tickTimeStopFightUses(serverPlayer, data);

        if (data.specEnergy < JojohaPlayerData.MAX_SPEC_ENERGY) {
            data.specEnergy = Math.min(JojohaPlayerData.MAX_SPEC_ENERGY,
                    data.specEnergy + SPEC_REGEN_PER_TICK
                            * org.gumel.jojoha.data.StatEffects.regenScale(data));
        }

        if (data.combatTicks > 0) {
            data.combatTicks--;
        }
        tickGuardBreakCooldown(serverPlayer, data);

        // A DORMANT cast holds no form, so it costs nothing to sustain and regenerates as though
        // the Stand were put away - the aura is all it produces.
        boolean sustainingManifestation = data.standSummoned && data.stand.trust().manifestsEntity();

        if (sustainingManifestation) {
            // An EMERGING manifestation collapses on its own timer rather than lasting until
            // the energy pool empties - the Stand can hold form, just not for long.
            StandEntity stand = StandSummonHandler.findStand(serverPlayer, data);
            if (stand != null && stand.hasOutlivedEmergingWindow()) {
                StandSummonHandler.dismiss(serverPlayer, data);
                PlayerDataAccess.set(serverPlayer, data);
                PlayerDataAccess.sync(serverPlayer);
                return;
            }

            float drain = perTick(data.maxStandEnergy(),
                    data.combatTicks > 0 ? STAND_DRAIN_COMBAT_SECONDS : STAND_DRAIN_PASSIVE_SECONDS);
            data.standEnergy = Math.max(0F, data.standEnergy - drain * data.stand.trust().energyDrainMultiplier());
            if (data.standEnergy <= 0F) {
                // Running dry breaks the cast rather than releasing it, so it gets the collapse
                // cue - the same event the Stand reports when it has absorbed too much.
                StandSummonHandler.collapse(serverPlayer, data);
                PlayerDataAccess.set(serverPlayer, data);
                PlayerDataAccess.sync(serverPlayer);
                return;
            }
        } else if (data.standEnergy < data.maxStandEnergy()) {
            data.standEnergy = Math.min(data.maxStandEnergy(),
                    data.standEnergy + perTick(data.maxStandEnergy(), STAND_REGEN_SECONDS)
                            * org.gumel.jojoha.data.StatEffects.regenScale(data));
        }

        // The aura answers the cast at every tier, whether or not anything took form - at DORMANT
        // it's the entire effect (design doc's Trust Tiers: "you won't see the stand but you'll
        // have the particles").
        if (data.standSummoned) {
            StandAuraEffect.tick(serverPlayer);
        }

        PlayerDataAccess.set(serverPlayer, data);

        if (serverPlayer.level().getGameTime() % SYNC_INTERVAL_TICKS == 0) {
            PlayerDataAccess.sync(serverPlayer);
        }
    }

    /** A full pool spread over the given number of seconds. */
    private static float perTick(float pool, int seconds) {
        return pool / (seconds * 20F);
    }

    /**
     * Remembers which way the player is travelling, for moves that fire in that direction.
     *
     * <p>Runs on the player tick, which is the one place a real position delta can be observed -
     * see the fields it writes for why a packet handler cannot do this itself.
     */
    private static void trackMovement(ServerPlayer player, JojohaPlayerData data) {
        double dx = player.getX() - data.lastTickX;
        double dz = player.getZ() - data.lastTickZ;
        data.lastTickX = player.getX();
        data.lastTickZ = player.getZ();

        double travelledSqr = dx * dx + dz * dz;

        // A jump that large was not walked: it is the first tick after login with the baseline
        // still at zero, a respawn, or a teleport. Any of those would otherwise be recorded as a
        // heading pointing wherever the player came from.
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

    /** Below this much ground covered in a tick, the player counts as standing still. */
    private static final double MOVING_THRESHOLD_SQR = 0.0015 * 0.0015;
    /** How long a heading stays usable after the player stops feeding it. */
    private static final int MOVE_MEMORY_TICKS = 6;
    /** Beyond this in one tick the player was moved, not moving - see trackMovement. */
    private static final double TELEPORT_THRESHOLD_SQR = 4.0;

    /** Wipes the per-fight time stop count once it no longer belongs to anything. */
    private static void tickTimeStopFightUses(ServerPlayer player, JojohaPlayerData data) {
        if (org.gumel.jojoha.stand.skill.moves.TimeStopSkill.shouldClearFightUses(
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
