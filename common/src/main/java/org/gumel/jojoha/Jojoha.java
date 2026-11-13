package org.gumel.jojoha;

import org.gumel.jojoha.town.VampireTalk;
import org.gumel.jojoha.town.Residents;
import org.gumel.jojoha.town.Settlements;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.DispenserBlock;
import org.gumel.jojoha.accessory.Relics;
import org.gumel.jojoha.block.RespawnTotems;
import org.gumel.jojoha.body.Limbs;
import org.gumel.jojoha.combat.BarrageBank;
import org.gumel.jojoha.combat.Clackers;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.combat.EnergySystem;
import org.gumel.jojoha.combat.JojohaEnchantments;
import org.gumel.jojoha.combat.Scarves;
import org.gumel.jojoha.combat.SlamState;
import org.gumel.jojoha.combat.StatPerkEffects;
import org.gumel.jojoha.combat.VampireBlood;
import org.gumel.jojoha.combat.VampireTraits;
import org.gumel.jojoha.combat.fx.MotionWake;
import org.gumel.jojoha.command.JojohaCommands;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.data.StatSources;
import org.gumel.jojoha.dream.DreamRealm;
import org.gumel.jojoha.entity.VampireSpawns;
import org.gumel.jojoha.entity.WillowSpawner;
import org.gumel.jojoha.entity.npc.NpcStanding;
import org.gumel.jojoha.entity.npc.Quests;
import org.gumel.jojoha.entity.npc.StandUserNpc;
import org.gumel.jojoha.family.FamilyLine;
import org.gumel.jojoha.fortune.Fortunes;
import org.gumel.jojoha.hamon.BubbleVolley;
import org.gumel.jojoha.hamon.HamonCombat;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.item.MaskBlood;
import org.gumel.jojoha.item.Provisions;
import org.gumel.jojoha.item.StandArrowRitual;
import org.gumel.jojoha.item.StoneMaskRitual;
import org.gumel.jojoha.labyrinth.CatacombSeal;
import org.gumel.jojoha.level.MeteorFall;
import org.gumel.jojoha.level.MeteorWorldgen;
import org.gumel.jojoha.loot.ArrowLoot;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.pillar.PillarBirth;
import org.gumel.jojoha.pillar.PillarMen;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.DiscToss;
import org.gumel.jojoha.stand.mob.StandPets;
import org.gumel.jojoha.stand.FightStings;
import org.gumel.jojoha.stand.LedgeCatch;
import org.gumel.jojoha.stand.ReflexGuard;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.StandUtilityWork;
import org.gumel.jojoha.stand.Standoff;
import org.gumel.jojoha.stand.ThrownBodies;
import org.gumel.jojoha.stand.anubis.Anubis;
import org.gumel.jojoha.stand.grapple.GrappleGrace;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.hierophant.HierophantTraps;
import org.gumel.jojoha.stand.hierophant.Possession;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.MoveInput;
import org.gumel.jojoha.stand.skill.PilotSystem;
import org.gumel.jojoha.stand.skill.SkillLoadout;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.stand.skill.TimeStopCast;
import org.gumel.jojoha.stand.skill.TimeStopCharging;
import org.gumel.jojoha.stand.skill.TimeStopSettle;
import org.gumel.jojoha.stand.skill.TimeStopSystem;
import org.gumel.jojoha.stand.skill.moves.ChoreSkill;
import org.gumel.jojoha.stand.skill.moves.PreciseEyesSkill;
import org.gumel.jojoha.stand.skill.moves.TwistingGutPunchSkill;
import org.gumel.jojoha.stand.skill.moves.VineHaul;
import org.gumel.jojoha.stand.workshop.Workshop;
import org.gumel.jojoha.supporter.SupporterGate;
import org.gumel.jojoha.supporter.SupporterLink;
import org.gumel.jojoha.trait.Traits;
import org.gumel.jojoha.ultimate.UltimateDiet;
import org.gumel.jojoha.vampire.Headless;
import org.gumel.jojoha.vampire.VampireCombat;
import org.gumel.jojoha.vampire.VampireMinions;
import org.gumel.jojoha.worldevent.MinorEvents;
import org.gumel.jojoha.worldevent.WorldEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Jojoha {
    public static final String MOD_ID = "jojoha";

    public static final String BUILD = "2026-09-26 Anubis, fishing arrows";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final long FOREIGN_COOLDOWN_TICKS = 24000L;

    public static void init() {
        LOGGER.info("JoJo: Heaven's Arrow - build {}, {} side", BUILD,
                Platform.getEnvironment());
        GrappleGrace.init();
        TwistingGutPunchSkill.init();
        VineHaul.init();
        ModRegistries.init();
        HamonPaths.bootstrap();
        StandTypes.bootstrap();
        JojohaServerConfig.bootstrap();
        NetworkHandler.init();
        JojohaCommands.init();
        EnergySystem.init();
        VampireTraits.init();
        PillarBirth.init();
        Headless.init();
        SlamState.register();
        VampireMinions.register();
        StandArrowRitual.init();
        FamilyLine.init();
        SupporterLink.init();
        SupporterGate.init();
        StoneMaskRitual.init();
        HierophantTraps.init();
        Possession.init();
        MaskBlood.init();
        TimeStopSystem.init();
        TimeStopCast.init();
        TimeStopCharging.init();
        TimeStopSettle.init();
        PilotSystem.init();
        Traits.init();
        Fortunes.init();
        Provisions.init();
        Limbs.init();
        SkillLoadout.init();
        CatacombSeal.init();
        DreamRealm.init();
        RespawnTotems.init();
        StandSkills.init();
        StandPassives.init();
        Heaven.init();
        Anubis.init();
        LifecycleEvent.SERVER_BEFORE_START.register(server ->
                JojohaServerConfig.FILE.reload());

        LifecycleEvent.SETUP.register(() -> {
            DispenserBlock.registerBehavior(ModItems.STAND_ARROW.get(),
                    new DefaultDispenseItemBehavior());
            DispenserBlock.registerBehavior(ModItems.STAND_ARROW_SHARD.get(),
                    new DefaultDispenseItemBehavior());
        });

        StandUtilityWork.init();
        LedgeCatch.init();
        DiscToss.init();
        StandPets.init();
        BarrageBank.init();
        ChoreSkill.init();
        PreciseEyesSkill.init();
        StandBeat.init();
        Standoff.init();
        FightStings.init();
        ThrownBodies.init();

        PlayerEvent.PLAYER_JOIN.register(player -> {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            data.standSummoned = false;
            data.summonedStandEntityUuid = null;
            data.standGuarding = false;

            data.standPiloting = false;
            StandUtilityWork.forget(player.getUUID());

            if (player instanceof ServerPlayer arriving) {
                Traits.rollIfUnset(arriving, data);
                Fortunes.rollIfUnset(arriving, data);
                PillarBirth.rollIfUnset(arriving, data);
            }

            long now = player.level().getGameTime();
            data.moveCooldowns.entrySet().removeIf(entry -> {
                long left = entry.getValue() - now;
                return left <= 0L || left > FOREIGN_COOLDOWN_TICKS;
            });

            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
        });
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> {
            JojohaPlayerData data = PlayerDataAccess.get(player);
            data.standSummoned = false;
            data.summonedStandEntityUuid = null;
            data.standGuarding = false;
            data.standPiloting = false;
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
        });

        PlayerEvent.PLAYER_QUIT.register(player -> {
            MoveInput.forget(player.getUUID());
            Standoff.forget(player.getUUID());
            ReflexGuard.forget(player.getUUID());
            if (player instanceof ServerPlayer leaving) {
                DreamRealm.forget(leaving);
            }
            JojohaPlayerData data = PlayerDataAccess.get(player);
            if (data.standSummoned) {
                StandSummonHandler.dismissImmediately(player, data);
                PlayerDataAccess.set(player, data);
            }
        });

        ReloadListenerRegistry.register(
                PackType.SERVER_DATA,
                new StandTuning(),
                StandTuning.ID);

        Defence.init();
        HitWindow.init();
        StatSources.init();
        StatPerkEffects.init();
        ArrowLoot.init();
        WillowSpawner.init();
        VampireSpawns.init();
        MeteorWorldgen.init();
        MeteorFall.init();
        WorldEvents.init();
        MinorEvents.init();
        NpcStanding.init();
        Quests.init();
        Settlements.init();
        Residents.init();
        VampireTalk.init();
        StandUserNpc.init();
        JojohaEnchantments.init();
        VampireBlood.init();
        UltimateDiet.init();
        HamonCombat.init();
        BubbleVolley.init();
        Clackers.init();
        Relics.init();
        Workshop.init();
        PillarMen.init();
        Scarves.init();
        MotionWake.init();
        VampireCombat.init();

        PlayerEvent.ATTACK_ENTITY.register((player, level, entity, hand, hitResult) -> {
            if (TimeStopSettle.settling(player)) {
                return EventResult.interruptFalse();
            }
            if (entity instanceof StandEntity stand && stand.getOwner() == player) {
                return EventResult.interruptFalse();
            }
            return EventResult.pass();
        });
    }
}
