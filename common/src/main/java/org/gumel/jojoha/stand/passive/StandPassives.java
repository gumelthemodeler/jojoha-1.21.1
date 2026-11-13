package org.gumel.jojoha.stand.passive;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.StandWeaponAffinity;
import org.gumel.jojoha.combat.StatPerkEffects;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StatEffects;
import org.gumel.jojoha.speech.Speech;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.StandVoices;
import org.gumel.jojoha.stand.anubis.Anubis;
import org.gumel.jojoha.stand.magician.MagicianPassive;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

        register(GrapplingVinePassive.KEEN);
        register(ThornbindPassive.INSTANCE);
        register(RippleConductionPassive.INSTANCE);
        register(SecondSightPassive.INSTANCE);
        register(MasterOfTimePassive.INSTANCE);
        register(CrushingForcePassive.INSTANCE);
        register(DeadeyePassive.INSTANCE);
        register(OpportunistPassive.INSTANCE);
        register(SplashFragmentsPassive.INSTANCE);
        register(ReboundPassive.INSTANCE);
        register(PiercingThrustPassive.INSTANCE);
        register(DuelistsFocusPassive.INSTANCE);
        register(MagicianPassive.TERRITORIAL);
        register(MagicianPassive.ENCROACHMENT);
        register(MagicianPassive.FLAME_IMMUNITY);
        register(MagicianPassive.BURNING_PRESSURE);
        register(SwordsmanshipPassive.INSTANCE);
        register(CombatAdaptationPassive.INSTANCE);
        register(BossPresencePassive.INSTANCE);
        register(HeavenTierPassive.ASCENDED);
        register(HeavenTierPassive.TRANSCENDENT);
        register(HeavenTierPassive.HEAVEN);
        register(AscensionGaugePassive.INSTANCE);
        register(RealityIsMinePassive.INSTANCE);
        TickEvent.PLAYER_POST.register(StandPassives::onPlayerTick);
    }

    public static List<StandPassive> activeFor(JojohaPlayerData data) {
        if (!data.stand.isPresent() || !data.standSummoned) {
            return List.of();
        }

        return PassiveLoadout.inEffect(data).stream()
                .map(StandPassives::byId)
                .filter(Objects::nonNull)
                .toList();
    }

    public static boolean grants(JojohaPlayerData data, ResourceLocation passiveId) {
        if (!data.stand.isPresent() || !data.standSummoned) {
            return false;
        }
        return PassiveLoadout.isInEffect(data, passiveId);
    }

    public static float scaleOutgoing(ServerPlayer player, JojohaPlayerData data,
                                      LivingEntity target, float amount) {
        float dealt = amount;
        for (StandPassive passive : activeFor(data)) {
            dealt = passive.onOutgoingDamage(player, data, target, dealt);
        }
        dealt = StatPerkEffects.standBlow(player, data, target, dealt);
        return dealt * StandWeaponAffinity.multiplier(player, data);
    }

    public static float scaleRanged(ServerPlayer player, LivingEntity target,
                                    Vec3 from, float amount) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        float dealt = amount;
        for (StandPassive passive : activeFor(data)) {
            dealt = passive.onRangedHit(player, data, target, from, dealt);
        }
        return Math.min(Math.max(amount, 10F), dealt);
    }

    private static void onPlayerTick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverPlayer);

        StatEffects.apply(serverPlayer, data);

        if (data.timeStopHeldTicks > 0) {
            data.timeStopHeldTicks--;
            PlayerDataAccess.set(serverPlayer, data);
            if (data.timeStopHeldTicks == 0) {
                StandEntity stand = StandSummonHandler.findStand(serverPlayer, data);
                if (stand != null) {
                    stand.stopTimeStopPose();
                }

                serverPlayer.serverLevel().playSound(null, serverPlayer.blockPosition(),
                        StandVoices.timeResume(
                                StandTypes.byId(data.stand.standId())),
                        SoundSource.PLAYERS, 1.1F, 1.0F);
                Speech.timeResumed(serverPlayer);
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
            UnwaveringPassive.clear(serverPlayer);
            if (!Anubis.out(serverPlayer)) {
                SwordsmanshipPassive.clear(serverPlayer);
            }
            return;
        }

        for (StandPassive passive : active) {
            passive.tick(serverPlayer, data);
        }
        if (!active.contains(SwordsmanshipPassive.INSTANCE)
                && !Anubis.out(serverPlayer)) {
            SwordsmanshipPassive.clear(serverPlayer);
        }

        PlayerDataAccess.set(serverPlayer, data);
    }
}
