package org.gumel.jojoha.stand.passive;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandTypes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of Stand passives, and the tick that runs whichever ones are currently in effect.
 *
 * <p>Driven from the player rather than from the Stand entity so that a passive keeps running
 * through anything that happens to the Stand's own ticking - being piloted, mid-dismissal, off
 * fighting. What matters is whether the user has their Stand out, which is a fact about the user.
 */
public final class StandPassives {
    private static final Map<ResourceLocation, StandPassive> PASSIVES = new LinkedHashMap<>();

    static {
        register(EnhancedReflexesPassive.INSTANCE);
        register(UnwaveringPassive.INSTANCE);
        register(TwoMetersPassive.INSTANCE);
        register(SensoryPerceptionPassive.INSTANCE);
    }

    private StandPassives() {
    }

    private static void register(StandPassive passive) {
        PASSIVES.put(passive.id(), passive);
    }

    public static StandPassive byId(ResourceLocation id) {
        return PASSIVES.get(id);
    }

    public static void init() {
        register(GrapplingVinePassive.INSTANCE);
        register(TangledPassive.INSTANCE);
        register(MadeForThisPassive.INSTANCE);
        register(UnorthodoxMethodPassive.INSTANCE);
        TickEvent.PLAYER_POST.register(StandPassives::onPlayerTick);
    }

    /** The passives a player's Stand currently grants, or empty if they have none manifested. */
    public static List<StandPassive> activeFor(JojohaPlayerData data) {
        if (!data.stand.isPresent() || !data.standSummoned) {
            return List.of();
        }

        return StandTypes.byIdOrDefault(data.stand.standId()).allPassives().stream()
                .map(StandPassives::byId)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Whether a player's Stand currently grants a particular passive.
     *
     * <p>Pure data, and deliberately so: it reads nothing but the player's own record, which means
     * the client can ask it of its synced copy. That is what lets a passive with no server behaviour
     * at all - Sensory Perception - still be a real registered passive rather than a hardcoded check
     * for one Stand's id somewhere in the renderer.
     */
    public static boolean grants(JojohaPlayerData data, ResourceLocation passiveId) {
        if (!data.stand.isPresent() || !data.standSummoned) {
            return false;
        }

        return StandTypes.byIdOrDefault(data.stand.standId()).allPassives().contains(passiveId);
    }

    /**
     * Runs an outgoing hit past every passive in effect.
     *
     * <p>Folded rather than first-wins, so two passives that both want a say both get one - and the
     * order they were registered in decides nothing, because multiplying is commutative and none of
     * them may assume it went first.
     */
    public static float scaleOutgoing(ServerPlayer player, JojohaPlayerData data,
                                      net.minecraft.world.entity.LivingEntity target, float amount) {
        float dealt = amount;
        for (StandPassive passive : activeFor(data)) {
            dealt = passive.onOutgoingDamage(player, data, target, dealt);
        }
        return dealt;
    }

    private static void onPlayerTick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverPlayer);

        // Every tick and for everybody, Stand or no Stand: these are the player's own stats, and a
        // person who has never held an arrow still has five of them. Cheap when nothing changed -
        // see StatEffects.apply.
        org.gumel.jojoha.data.StatEffects.apply(serverPlayer, data);

        // Runs regardless of whether this player has a Stand - being frozen is something done to
        // them, not something their own Stand grants.
        if (data.timeStopHeldTicks > 0) {
            data.timeStopHeldTicks--;
            PlayerDataAccess.set(serverPlayer, data);
            if (data.timeStopHeldTicks == 0) {
                // The pose is a looping trigger, so it has to be released explicitly.
                StandEntity stand = StandSummonHandler.findStand(serverPlayer, data);
                if (stand != null) {
                    stand.stopTimeStopPose();
                }

                serverPlayer.serverLevel().playSound(null, serverPlayer.blockPosition(),
                        ModSounds.TIME_RESUME.get(), SoundSource.PLAYERS, 1.1F, 1.0F);
                PlayerDataAccess.sync(serverPlayer);
            }
        }

        if (data.timeStopFrozenTicks > 0) {
            data.timeStopFrozenTicks--;
            PlayerDataAccess.set(serverPlayer, data);
            if (data.timeStopFrozenTicks == 0) {
                PlayerDataAccess.sync(serverPlayer);
            }
        }

        List<StandPassive> active = activeFor(data);

        if (active.isEmpty()) {
            // Cleaning up unconditionally rather than tracking whether it was ever applied: the
            // remove is a no-op when there is nothing there, and the alternative leaks a permanent
            // buff onto anyone whose Stand vanished by a route we forgot to hook.
            UnwaveringPassive.clear(serverPlayer);
            return;
        }

        for (StandPassive passive : active) {
            passive.tick(serverPlayer, data);
        }

        // Written back because a passive may have changed something on it - Enhanced Reflexes puts
        // itself on cooldown here, and losing that would leave it deflecting on every tick again.
        PlayerDataAccess.set(serverPlayer, data);
    }
}
