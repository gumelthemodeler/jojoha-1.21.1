package org.gumel.jojoha.network;

import org.gumel.jojoha.entity.VampireEntity;
import net.minecraft.world.entity.npc.Villager;
import org.gumel.jojoha.client.MeteorStreaks;
import org.gumel.jojoha.network.packet.MeteorStreakPacket;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.JojohaAdvancements;
import org.gumel.jojoha.body.Body;
import org.gumel.jojoha.client.AnubisLook;
import org.gumel.jojoha.client.BankedTint;
import org.gumel.jojoha.client.ClientQuests;
import org.gumel.jojoha.client.FactionBanner;
import org.gumel.jojoha.client.NpcDialogueScreen;
import org.gumel.jojoha.client.QuestBanner;
import org.gumel.jojoha.client.TimeStopChargers;
import org.gumel.jojoha.client.BarrageStruggleHud;
import org.gumel.jojoha.client.ClackerClient;
import org.gumel.jojoha.client.Clairvoyance;
import org.gumel.jojoha.client.DowsingFx;
import org.gumel.jojoha.client.DrainPale;
import org.gumel.jojoha.client.DreamSpirit;
import org.gumel.jojoha.client.FamilySenseHud;
import org.gumel.jojoha.client.FleshBudFx;
import org.gumel.jojoha.client.FortuneSense;
import org.gumel.jojoha.client.Gore;
import org.gumel.jojoha.client.GroundBreakerFx;
import org.gumel.jojoha.client.HeadlessView;
import org.gumel.jojoha.client.HeavenFx;
import org.gumel.jojoha.client.MagicianFx;
import org.gumel.jojoha.client.CombatShake;
import org.gumel.jojoha.client.Menacing;
import org.gumel.jojoha.client.MeteorImpactFx;
import org.gumel.jojoha.client.NoiseSense;
import org.gumel.jojoha.client.PaletteSwap;
import org.gumel.jojoha.client.ParryFeel;
import org.gumel.jojoha.client.PlayerMoveAnimation;
import org.gumel.jojoha.client.PossessionClient;
import org.gumel.jojoha.client.PreciseEyesView;
import org.gumel.jojoha.client.SabreArcFx;
import org.gumel.jojoha.client.ScarfClient;
import org.gumel.jojoha.client.SkullFlashFx;
import org.gumel.jojoha.client.SpaceRipperBeam;
import org.gumel.jojoha.client.StandAfterimages;
import org.gumel.jojoha.client.StandFistView;
import org.gumel.jojoha.client.StandIdles;
import org.gumel.jojoha.client.StandRitualEffects;
import org.gumel.jojoha.client.StingFx;
import org.gumel.jojoha.client.StunPose;
import org.gumel.jojoha.client.SubzeroChill;
import org.gumel.jojoha.client.SummonFlair;
import org.gumel.jojoha.client.SunScorch;
import org.gumel.jojoha.client.TangledVines;
import org.gumel.jojoha.client.TendrilStrands;
import org.gumel.jojoha.client.ThornLashFx;
import org.gumel.jojoha.client.ThrownTumble;
import org.gumel.jojoha.client.TimeStopView;
import org.gumel.jojoha.client.TrickTell;
import org.gumel.jojoha.client.LimbRegrowLayer;
import org.gumel.jojoha.client.UltimateWingsLayer;
import org.gumel.jojoha.client.WorldEventSky;
import org.gumel.jojoha.client.ZoomPunchArm;
import org.gumel.jojoha.client.fp.FirstPersonPoses;
import org.gumel.jojoha.client.fx.ImpactDirector;
import org.gumel.jojoha.client.pillar.PillarClient;
import org.gumel.jojoha.combat.Clackers;
import org.gumel.jojoha.combat.Climbing;
import org.gumel.jojoha.combat.Parry;
import org.gumel.jojoha.combat.fx.Palette;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandPlacement;
import org.gumel.jojoha.data.StandPose;
import org.gumel.jojoha.data.StatPoints;
import org.gumel.jojoha.dream.DreamRealm;
import org.gumel.jojoha.entity.npc.JojoNpc;
import org.gumel.jojoha.fortune.Wicked;
import org.gumel.jojoha.hamon.HamonMoves;
import org.gumel.jojoha.hamon.HamonPath;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.network.packet.AnubisWieldersPacket;
import org.gumel.jojoha.network.packet.BarrageStrugglePacket;
import org.gumel.jojoha.network.packet.ChooseDashPacket;
import org.gumel.jojoha.network.packet.ClackerSwingPacket;
import org.gumel.jojoha.network.packet.ClackerThrowPacket;
import org.gumel.jojoha.network.packet.FactionStandingPacket;
import org.gumel.jojoha.network.packet.NpcDialoguePacket;
import org.gumel.jojoha.network.packet.NpcReplyPacket;
import org.gumel.jojoha.town.Residents;
import org.gumel.jojoha.town.VampireTalk;
import org.gumel.jojoha.network.packet.QuestBannerPacket;
import org.gumel.jojoha.network.packet.QuestLogPacket;
import org.gumel.jojoha.network.packet.TimeStopThrowPacket;
import org.gumel.jojoha.network.packet.ClairvoyancePacket;
import org.gumel.jojoha.network.packet.BodyStatePacket;
import org.gumel.jojoha.network.packet.ClimbingPacket;
import org.gumel.jojoha.network.packet.CycleStandModePacket;
import org.gumel.jojoha.network.packet.DowsingPacket;
import org.gumel.jojoha.network.packet.DreamAnswerPacket;
import org.gumel.jojoha.network.packet.DreamSpeechPacket;
import org.gumel.jojoha.network.packet.EngageStandTargetPacket;
import org.gumel.jojoha.network.packet.EquipAdaptationPacket;
import org.gumel.jojoha.network.packet.EquipPassivePacket;
import org.gumel.jojoha.network.packet.EquipSkillPacket;
import org.gumel.jojoha.network.packet.EquipStandSkinPacket;
import org.gumel.jojoha.network.packet.FamilySensePacket;
import org.gumel.jojoha.network.packet.FirstPersonPosePacket;
import org.gumel.jojoha.network.packet.FlairCuePacket;
import org.gumel.jojoha.network.packet.FortuneSensePacket;
import org.gumel.jojoha.network.packet.TrickTellPacket;
import org.gumel.jojoha.network.packet.GroundBreakerFxPacket;
import org.gumel.jojoha.network.packet.HeadlessPacket;
import org.gumel.jojoha.network.packet.HeavenFxPacket;
import org.gumel.jojoha.network.packet.ImpactCuePacket;
import org.gumel.jojoha.network.packet.MagicianFxPacket;
import org.gumel.jojoha.network.packet.MenacePacket;
import org.gumel.jojoha.network.packet.MeteorImpactPacket;
import org.gumel.jojoha.network.packet.MoveInputPacket;
import org.gumel.jojoha.network.packet.MovementCuePacket;
import org.gumel.jojoha.network.packet.PaletteCuePacket;
import org.gumel.jojoha.network.packet.ParryCaughtPacket;
import org.gumel.jojoha.network.packet.PillarFxPacket;
import org.gumel.jojoha.network.packet.PillarStatePacket;
import org.gumel.jojoha.network.packet.PilotPosePacket;
import org.gumel.jojoha.network.packet.PlayerMovePosePacket;
import org.gumel.jojoha.network.packet.PossessionAttackPacket;
import org.gumel.jojoha.network.packet.PossessionInputPacket;
import org.gumel.jojoha.network.packet.PossessionStatePacket;
import org.gumel.jojoha.network.packet.PossessionSteerPacket;
import org.gumel.jojoha.network.packet.PreciseEyesPacket;
import org.gumel.jojoha.network.packet.ReformPacket;
import org.gumel.jojoha.network.packet.RequestDoubleJumpPacket;
import org.gumel.jojoha.network.packet.RequestParryPacket;
import org.gumel.jojoha.network.packet.RequestStandIdlePacket;
import org.gumel.jojoha.network.packet.RequestStandPunchPacket;
import org.gumel.jojoha.network.packet.SabreArcPacket;
import org.gumel.jojoha.network.packet.ScarfStrikePacket;
import org.gumel.jojoha.network.packet.SetStandGuardPacket;
import org.gumel.jojoha.network.packet.SkullFlashPacket;
import org.gumel.jojoha.network.packet.SpaceRipperPacket;
import org.gumel.jojoha.network.packet.SpendStatPointPacket;
import org.gumel.jojoha.network.packet.StandAfterimagePacket;
import org.gumel.jojoha.network.packet.StandChargePacket;
import org.gumel.jojoha.network.packet.StandFistPacket;
import org.gumel.jojoha.network.packet.StandIdleClipPacket;
import org.gumel.jojoha.network.packet.StandIdlePacket;
import org.gumel.jojoha.network.packet.StandNamedPacket;
import org.gumel.jojoha.network.packet.StandPlacementPacket;
import org.gumel.jojoha.network.packet.WorkshopLookPacket;
import org.gumel.jojoha.network.packet.WorkshopPosePacket;
import org.gumel.jojoha.network.packet.WorkshopStylePacket;
import org.gumel.jojoha.network.packet.StandRitualEffectPacket;
import org.gumel.jojoha.network.packet.StandUseItemPacket;
import org.gumel.jojoha.network.packet.StingPacket;
import org.gumel.jojoha.network.packet.StunPosePacket;
import org.gumel.jojoha.network.packet.SubzeroPacket;
import org.gumel.jojoha.network.packet.SunScorchPacket;
import org.gumel.jojoha.network.packet.SupporterChoicePacket;
import org.gumel.jojoha.network.packet.SupporterPacket;
import org.gumel.jojoha.network.packet.SupporterSavePacket;
import org.gumel.jojoha.network.packet.SyncPlayerDataPacket;
import org.gumel.jojoha.network.packet.TangledPacket;
import org.gumel.jojoha.network.packet.TendrilPacket;
import org.gumel.jojoha.network.packet.ThornLashPacket;
import org.gumel.jojoha.network.packet.ThrownPacket;
import org.gumel.jojoha.network.packet.TimeStopStatePacket;
import org.gumel.jojoha.network.packet.ToggleHalfSummonPacket;
import org.gumel.jojoha.network.packet.ToggleStandSummonPacket;
import org.gumel.jojoha.network.packet.UnlockNodePacket;
import org.gumel.jojoha.network.packet.UseHamonMovePacket;
import org.gumel.jojoha.network.packet.UseStandSkillPacket;
import org.gumel.jojoha.network.packet.VampireMarkPacket;
import org.gumel.jojoha.network.packet.WingFlapPacket;
import org.gumel.jojoha.network.packet.WingsStatePacket;
import org.gumel.jojoha.network.packet.WorldEventPacket;
import org.gumel.jojoha.network.packet.ZoomPunchPacket;
import org.gumel.jojoha.pillar.PillarFxKind;
import org.gumel.jojoha.skilltree.SkillNode;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.StandCustomization;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandHands;
import org.gumel.jojoha.stand.StandSkinChoice;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.combat.StandFist;
import org.gumel.jojoha.stand.hierophant.Possession;
import org.gumel.jojoha.stand.passive.PassiveLoadout;
import org.gumel.jojoha.stand.passive.StandPassive;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.MoveInput;
import org.gumel.jojoha.stand.skill.PilotSystem;
import org.gumel.jojoha.stand.skill.SkillLoadout;
import org.gumel.jojoha.stand.skill.StandReinforced;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.stand.skill.TimeStopSystem;
import org.gumel.jojoha.stand.skill.moves.DoubleJumpSkill;
import org.gumel.jojoha.stand.skill.moves.TimeStopThrow;
import org.gumel.jojoha.stand.workshop.Workshop;
import org.gumel.jojoha.supporter.SupporterAuras;
import org.gumel.jojoha.supporter.SupporterChoice;
import org.gumel.jojoha.supporter.SupporterLink;
import org.gumel.jojoha.ultimate.AdaptationLibrary;
import org.gumel.jojoha.ultimate.AdaptiveFlightMove;
import org.gumel.jojoha.vampire.VampireMoves;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class NetworkHandler {

    private NetworkHandler() {
    }

    public static void sendBarrageStruggle(ServerPlayer player,
                                           BarrageStrugglePacket packet) {
        NetworkManager.sendToPlayer(player, packet);
    }

    public static void init() {
        if (Platform.getEnvironment() == Env.CLIENT) {
            NetworkManager.registerReceiver(NetworkManager.s2c(), SyncPlayerDataPacket.TYPE, SyncPlayerDataPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> {
                        JojohaPlayerData data = payload.data();
                        ClientPlayerDataCache.data = data;
                    }));
            NetworkManager.registerReceiver(NetworkManager.s2c(), StandRitualEffectPacket.TYPE, StandRitualEffectPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> StandRitualEffects.handle(payload)));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    FamilySensePacket.TYPE,
                    FamilySensePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(
                            () -> FamilySenseHud.sensed(payload.distance())));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    SupporterPacket.TYPE,
                    SupporterPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> SupporterAuras.received(
                            payload.player(), payload.aura(), payload.skins(),
                            payload.player().equals(Minecraft.getInstance().getUser().getProfileId()))));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    SupporterSavePacket.TYPE,
                    SupporterSavePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(
                            () -> SupporterChoice.save(payload.aura())));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    ClackerSwingPacket.TYPE,
                    ClackerSwingPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(
                            () -> ClackerClient.heard(payload.playerId(), payload.kind(),
                                    payload.targetId())));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    ScarfStrikePacket.TYPE,
                    ScarfStrikePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(
                            () -> ScarfClient.heard(payload.playerId(), payload.targetId())));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    StandNamedPacket.TYPE,
                    StandNamedPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> StandRitualEffects.named(payload)));
            NetworkManager.registerReceiver(NetworkManager.s2c(), StandAfterimagePacket.TYPE, StandAfterimagePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> StandAfterimages.begin(payload)));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    PlayerMovePosePacket.TYPE,
                    PlayerMovePosePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> {
                        Minecraft minecraft = Minecraft.getInstance();
                        if (minecraft.level != null) {
                            PlayerMoveAnimation.begin(payload.entityId(), payload.animation(),
                                    payload.mode(), (float) minecraft.level.getGameTime());
                        }
                    }));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    MovementCuePacket.TYPE,
                    MovementCuePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> ImpactDirector.movement(payload)));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    StandIdleClipPacket.TYPE,
                    StandIdleClipPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(
                            () -> StandIdles.heard(payload.stamp(), payload.clip())));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    StandFistPacket.TYPE,
                    StandFistPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> StandFistView.add(payload)));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    StunPosePacket.TYPE,
                    StunPosePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> StunPose.heard(payload.entityId())));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    HeadlessPacket.TYPE,
                    HeadlessPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(
                            () -> HeadlessView.heard(payload.entityId(), payload.state())));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    ParryCaughtPacket.TYPE,
                    ParryCaughtPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> ParryFeel.heard(
                            payload.entityId(), payload.perfect(), payload.weapon(), payload.side())));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    ThrownPacket.TYPE,
                    ThrownPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> ThrownTumble.heard(
                            payload.entityId(), payload.heading(), payload.spin())));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    ReformPacket.TYPE,
                    ReformPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> Gore.reform(
                            payload.entityId(), payload.left(), payload.total())));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    TangledPacket.TYPE,
                    TangledPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(
                            () -> TangledVines.heard(payload.entityId(), payload.ticks())));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    MenacePacket.TYPE,
                    MenacePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> Menacing.heard(payload.entityId(), payload.kind())));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    StingPacket.TYPE,
                    StingPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> StingFx.heard(payload.kind())));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    PaletteCuePacket.TYPE,
                    PaletteCuePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> PaletteSwap.hold(
                            Palette.byId(payload.palette()), payload.ticks())));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    FlairCuePacket.TYPE,
                    FlairCuePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> {
                        if (payload.kind() == FlairCuePacket.DRIP) {
                            Gore.drip(payload.entityId(), payload.amount());
                        } else if (payload.kind() == FlairCuePacket.SPURT) {
                            Gore.spurt(payload.entityId(), payload.amount());
                        } else if (payload.kind() == FlairCuePacket.SEVER) {
                            Gore.sever(payload.entityId(), payload.amount());
                            LimbRegrowLayer.cut(payload.entityId());
                        } else if (payload.kind() == FlairCuePacket.REGROW) {
                            LimbRegrowLayer.start(payload.entityId(), payload.amount());
                        } else if (payload.kind() == FlairCuePacket.RUPTURE) {
                            Gore.rupture(payload.entityId(), payload.amount());
                        } else if (payload.kind() == FlairCuePacket.BANKED) {
                            BankedTint.heard(payload.entityId(), payload.amount());
                        } else if (payload.kind() == FlairCuePacket.TIME_CHARGE) {
                            TimeStopChargers.heard(payload.entityId(), payload.amount());
                        } else if (payload.kind() == FlairCuePacket.NOISE) {
                            NoiseSense.heard(payload.entityId(), payload.amount());
                        } else if (payload.kind() == FlairCuePacket.ITCH) {
                            CombatShake.itch(payload.amount() / (float) Wicked.ITCH_STEPS);
                        } else {
                            SummonFlair.heard(payload.entityId(), payload.amount(),
                                    payload.kind() == FlairCuePacket.SUMMON);
                        }
                    }));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    MagicianFxPacket.TYPE,
                    MagicianFxPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> MagicianFx.handle(payload)));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    BarrageStrugglePacket.TYPE,
                    BarrageStrugglePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> BarrageStruggleHud.update(payload)));
            NetworkManager.registerReceiver(NetworkManager.s2c(), NpcDialoguePacket.TYPE, NpcDialoguePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> NpcDialogueScreen.open(payload.entityId(), payload.text(), payload.replies(), payload.trust(), payload.canGift(), payload.trait())));
            NetworkManager.registerReceiver(NetworkManager.s2c(), QuestLogPacket.TYPE, QuestLogPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> ClientQuests.set(payload.ids(), payload.done(), payload.needed(), payload.marks(), payload.offering())));
            NetworkManager.registerReceiver(NetworkManager.s2c(), QuestBannerPacket.TYPE, QuestBannerPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> QuestBanner.show(payload)));
            NetworkManager.registerReceiver(NetworkManager.s2c(), FactionStandingPacket.TYPE, FactionStandingPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> FactionBanner.show(payload)));
            NetworkManager.registerReceiver(NetworkManager.s2c(), WorldEventPacket.TYPE, WorldEventPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> WorldEventSky.set(payload.active())));
            NetworkManager.registerReceiver(NetworkManager.s2c(), TimeStopStatePacket.TYPE, TimeStopStatePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> TimeStopView.set(
                            payload.active(), payload.x(), payload.y(), payload.z(),
                            payload.remainingTicks(), payload.frozenPlayers(), payload.radius(),
                            payload.quietTicks(), payload.introTicks())));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    DreamSpeechPacket.TYPE,
                    DreamSpeechPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(
                            () -> DreamSpirit.heard(payload)));
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    AnubisWieldersPacket.TYPE,
                    AnubisWieldersPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(
                            () -> AnubisLook.heard(payload.entityIds())));
        } else {
            NetworkManager.registerS2CPayloadType(AnubisWieldersPacket.TYPE,
                    AnubisWieldersPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    DreamSpeechPacket.TYPE,
                    DreamSpeechPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(SyncPlayerDataPacket.TYPE, SyncPlayerDataPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(FamilySensePacket.TYPE,
                    FamilySensePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(SupporterPacket.TYPE,
                    SupporterPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(SupporterSavePacket.TYPE,
                    SupporterSavePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(ClackerSwingPacket.TYPE,
                    ClackerSwingPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(ScarfStrikePacket.TYPE,
                    ScarfStrikePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(BarrageStrugglePacket.TYPE,
                    BarrageStrugglePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(StunPosePacket.TYPE,
                    StunPosePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(HeadlessPacket.TYPE,
                    HeadlessPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(ParryCaughtPacket.TYPE,
                    ParryCaughtPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(ThrownPacket.TYPE,
                    ThrownPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(ReformPacket.TYPE,
                    ReformPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(TangledPacket.TYPE,
                    TangledPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(MenacePacket.TYPE,
                    MenacePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(StingPacket.TYPE,
                    StingPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(PaletteCuePacket.TYPE,
                    PaletteCuePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(FlairCuePacket.TYPE,
                    FlairCuePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(MagicianFxPacket.TYPE,
                    MagicianFxPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(StandRitualEffectPacket.TYPE, StandRitualEffectPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(StandAfterimagePacket.TYPE, StandAfterimagePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(StandNamedPacket.TYPE,
                    StandNamedPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(PlayerMovePosePacket.TYPE,
                    PlayerMovePosePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(MovementCuePacket.TYPE,
                    MovementCuePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    StandFistPacket.TYPE,
                    StandFistPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    StandIdleClipPacket.TYPE,
                    StandIdleClipPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(TimeStopStatePacket.TYPE, TimeStopStatePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(WorldEventPacket.TYPE, WorldEventPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(QuestLogPacket.TYPE, QuestLogPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(QuestBannerPacket.TYPE, QuestBannerPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(FactionStandingPacket.TYPE, FactionStandingPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(NpcDialoguePacket.TYPE, NpcDialoguePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(SkullFlashPacket.TYPE, SkullFlashPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(HeavenFxPacket.TYPE,
                    HeavenFxPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    MeteorImpactPacket.TYPE,
                    MeteorImpactPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    GroundBreakerFxPacket.TYPE,
                    GroundBreakerFxPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    ImpactCuePacket.TYPE,
                    ImpactCuePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    SabreArcPacket.TYPE,
                    SabreArcPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    ThornLashPacket.TYPE,
                    ThornLashPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    ClairvoyancePacket.TYPE,
                    ClairvoyancePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    DowsingPacket.TYPE,
                    DowsingPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    PreciseEyesPacket.TYPE,
                    PreciseEyesPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    FirstPersonPosePacket.TYPE,
                    FirstPersonPosePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    ZoomPunchPacket.TYPE,
                    ZoomPunchPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    MeteorStreakPacket.TYPE,
                    MeteorStreakPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    SpaceRipperPacket.TYPE,
                    SpaceRipperPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(TendrilPacket.TYPE, TendrilPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(PillarFxPacket.TYPE,
                    PillarFxPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(PillarStatePacket.TYPE,
                    PillarStatePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(WingsStatePacket.TYPE,
                    WingsStatePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(PossessionStatePacket.TYPE,
                    PossessionStatePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(PossessionInputPacket.TYPE,
                    PossessionInputPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    SubzeroPacket.TYPE,
                    SubzeroPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    FortuneSensePacket.TYPE,
                    FortuneSensePacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    TrickTellPacket.TYPE,
                    TrickTellPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    VampireMarkPacket.TYPE,
                    VampireMarkPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    SunScorchPacket.TYPE,
                    SunScorchPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    ClimbingPacket.TYPE,
                    ClimbingPacket.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(
                    BodyStatePacket.TYPE,
                    BodyStatePacket.STREAM_CODEC);
        }

        NetworkManager.registerReceiver(NetworkManager.c2s(), UseHamonMovePacket.TYPE, UseHamonMovePacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        if (!VampireMoves.handleUseRequest(
                                serverPlayer, payload.moveId())) {
                            HamonMoves.handleUseRequest(serverPlayer, payload.moveId());
                        }
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                ToggleHalfSummonPacket.TYPE,
                ToggleHalfSummonPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandSummonHandler.handleHalfSummonRequest(serverPlayer);
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                ClackerThrowPacket.TYPE,
                ClackerThrowPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        Clackers.handleThrow(serverPlayer);
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                TimeStopThrowPacket.TYPE,
                TimeStopThrowPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        TimeStopThrow.handleThrow(serverPlayer);
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), ToggleStandSummonPacket.TYPE, ToggleStandSummonPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandSummonHandler.handleToggleRequest(serverPlayer);
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                RequestDoubleJumpPacket.TYPE,
                RequestDoubleJumpPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        DoubleJumpSkill.report(payload.x(), payload.z());
                        try {
                            StandSkills.usePassive(serverPlayer,
                                    DoubleJumpSkill.INSTANCE);
                        } finally {
                            DoubleJumpSkill.forget();
                        }
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                MoveInputPacket.TYPE,
                MoveInputPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        MoveInput.report(serverPlayer, payload.forward(), payload.left());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), NpcReplyPacket.TYPE, NpcReplyPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
                        return;
                    }
                    Entity talker = serverPlayer.level().getEntity(payload.entityId());
                    if (talker instanceof JojoNpc npc) {
                        npc.replied(serverPlayer, payload.text(), payload.reply());
                    } else if (talker instanceof Villager villager) {
                        Residents.replied(serverPlayer, villager, payload.text(), payload.reply());
                    } else if (talker instanceof VampireEntity vampire) {
                        VampireTalk.replied(serverPlayer, vampire, payload.text(), payload.reply());
                    }
                }));
        NetworkManager.registerReceiver(NetworkManager.c2s(), RequestParryPacket.TYPE, RequestParryPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        Parry.request(serverPlayer, payload.variant());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                DreamAnswerPacket.TYPE,
                DreamAnswerPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        DreamRealm.answer(serverPlayer, payload.turn(), payload.answer());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                EquipStandSkinPacket.TYPE,
                EquipStandSkinPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandSkinChoice.equip(serverPlayer, payload.skin());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                WorkshopStylePacket.TYPE,
                WorkshopStylePacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        Workshop.style(serverPlayer, payload.summon(), payload.shout(), payload.rest());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                WorkshopLookPacket.TYPE,
                WorkshopLookPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        Workshop.look(serverPlayer, payload.slot(), payload.save(), payload.skin());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                WorkshopPosePacket.TYPE,
                WorkshopPosePacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        Workshop.pose(serverPlayer, payload.slot(), payload.pose());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                StandPlacementPacket.TYPE,
                StandPlacementPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandCustomization.place(serverPlayer,
                                new StandPlacement(payload.side(), payload.height(),
                                        payload.ahead(), payload.turn()));
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                WingFlapPacket.TYPE,
                WingFlapPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        AdaptiveFlightMove.flap(serverPlayer);
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                EquipAdaptationPacket.TYPE,
                EquipAdaptationPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        AdaptationLibrary.equip(serverPlayer, payload.adaptation(),
                                payload.active(), payload.slot());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                ChooseDashPacket.TYPE,
                ChooseDashPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandReinforced.chooseDash(serverPlayer, payload.timeSkip());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                StandIdlePacket.TYPE,
                StandIdlePacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandCustomization.idle(serverPlayer, payload.key(), payload.clip());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                RequestStandIdlePacket.TYPE,
                RequestStandIdlePacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    String clip = StandCustomization.clip(payload.stamp());
                    if (clip != null && context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        NetworkManager.sendToPlayer(serverPlayer,
                                new StandIdleClipPacket(payload.stamp(), clip));
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), RequestStandPunchPacket.TYPE, RequestStandPunchPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandCombatHandler.handlePunchRequest(serverPlayer, payload.aimedId());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), StandUseItemPacket.TYPE, StandUseItemPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandHands.handleUseRequest(serverPlayer, payload.stretchAnchor(),
                                payload.turns());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                StandChargePacket.TYPE,
                StandChargePacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandEntity stand =
                                StandEntity.findFor(serverPlayer);
                        if (stand != null) {
                            stand.setCharging(payload.charging());
                        }
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                EquipPassivePacket.TYPE,
                EquipPassivePacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
                        return;
                    }

                    ResourceLocation id =
                            ResourceLocation.tryParse(payload.passiveId());
                    StandPassive passive =
                            id == null ? null : StandPassives.byId(id);
                    if (passive == null) {
                        return;
                    }

                    JojohaPlayerData data =
                            PlayerDataAccess.get(serverPlayer);

                    if (!StandTypes.byIdOrDefault(data.stand.standId())
                            .allPassives().contains(id)
                            || !SkillTrees.skillUnlocked(data, id)) {
                        return;
                    }

                    PassiveLoadout.choose(data, id);
                    PlayerDataAccess.set(serverPlayer, data);
                    PlayerDataAccess.sync(serverPlayer);
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(),
                SupporterChoicePacket.TYPE,
                SupporterChoicePacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        SupporterLink.chose(serverPlayer, payload.aura());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), UseStandSkillPacket.TYPE, UseStandSkillPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandSkills.handleUseRequest(serverPlayer, payload.slot(), payload.chargeTicks());
                    }
                }));

        if (Platform.getEnvironment() == Env.CLIENT) {
            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    ImpactCuePacket.TYPE,
                    ImpactCuePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            ImpactDirector.cue(payload)));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    GroundBreakerFxPacket.TYPE,
                    GroundBreakerFxPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            GroundBreakerFx.begin(payload.throwerId(),
                                    payload.x(), payload.y(), payload.z(), payload.radius(),
                                    payload.phase(), payload.ticks())));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    MeteorImpactPacket.TYPE,
                    MeteorImpactPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            MeteorImpactFx.impact(
                                    payload.x(), payload.y(), payload.z(), payload.scale())));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    ClairvoyancePacket.TYPE,
                    ClairvoyancePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            Clairvoyance.begin(payload.ticks())));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    PreciseEyesPacket.TYPE,
                    PreciseEyesPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            PreciseEyesView.set(payload.on())));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    DowsingPacket.TYPE,
                    DowsingPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            DowsingFx.begin(payload.playerId(), payload.target(),
                                    payload.ticks())));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    FirstPersonPosePacket.TYPE,
                    FirstPersonPosePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            FirstPersonPoses.heard(
                                    payload.pose(), payload.ticks())));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    HeavenFxPacket.TYPE,
                    HeavenFxPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            HeavenFx.heard(payload)));

            NetworkManager.registerReceiver(NetworkManager.s2c(), SkullFlashPacket.TYPE,
                    SkullFlashPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            SkullFlashFx.begin(payload.victimId(),
                                    payload.attackerId(), payload.phase(), payload.ticks())));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    SabreArcPacket.TYPE,
                    SabreArcPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            SabreArcFx.begin(payload.x(), payload.y(), payload.z(),
                                    payload.radius(), payload.start(), payload.charge())));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    ThornLashPacket.TYPE,
                    ThornLashPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            ThornLashFx.begin(payload.fromId(),
                                    payload.toId(), payload.ticks(), payload.coilAfter())));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    ZoomPunchPacket.TYPE,
                    ZoomPunchPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            ZoomPunchArm.begin(payload.playerId(),
                                    payload.x(), payload.y(), payload.z(), payload.ticks())));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    MeteorStreakPacket.TYPE,
                    MeteorStreakPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            MeteorStreaks.begin(payload.x(), payload.y(), payload.z(),
                                    payload.vx(), payload.vy(), payload.vz(), payload.life(), payload.big())));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    ClimbingPacket.TYPE,
                    ClimbingPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> {
                        Climbing.setMirror(
                                payload.player(),
                                Climbing.Grip.byOrdinal(payload.grip()),
                                payload.climbing());
                        PillarClient.refreshSize(payload.player());
                    }));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    BodyStatePacket.TYPE,
                    BodyStatePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            Body.heard(payload.player(), payload.state())));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    SunScorchPacket.TYPE,
                    SunScorchPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            SunScorch.set(payload.exposure())));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    VampireMarkPacket.TYPE,
                    VampireMarkPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> {
                        Minecraft minecraft =
                                Minecraft.getInstance();
                        if (minecraft.level == null) {
                            return;
                        }
                        long now = minecraft.level.getGameTime();
                        switch (payload.mark()) {
                            case BUD -> FleshBudFx.set(payload.entityId(),
                                    payload.ticks(), now);
                            case DRAINED -> DrainPale.set(payload.entityId(),
                                    payload.ticks(), (float) now);
                        }
                    }));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    FortuneSensePacket.TYPE,
                    FortuneSensePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> {
                        if (payload.kind() == FortuneSensePacket.DIM) {
                            FortuneSense.dim(payload.ticks());
                        } else {
                            FortuneSense.glow(payload.entityIds(), payload.ticks());
                        }
                    }));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    TrickTellPacket.TYPE,
                    TrickTellPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            TrickTell.show(payload.entityId(), payload.move(), payload.ticks())));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    SubzeroPacket.TYPE,
                    SubzeroPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> {
                        Minecraft minecraft =
                                Minecraft.getInstance();
                        if (minecraft.level == null) {
                            return;
                        }
                        float now = (float) minecraft.level.getGameTime();
                        if (payload.ticks() > 0) {
                            SubzeroChill.begin(payload.playerId(),
                                    payload.ticks(), now);
                        } else {
                            SubzeroChill.end(payload.playerId(), now);
                        }
                    }));

            NetworkManager.registerReceiver(NetworkManager.s2c(),
                    SpaceRipperPacket.TYPE,
                    SpaceRipperPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() ->
                            SpaceRipperBeam.begin(payload.playerId(),
                                    payload.x(), payload.y(), payload.z(), payload.offsets(),
                                    payload.eye(), payload.fireTicks(), payload.ticks())));

            NetworkManager.registerReceiver(NetworkManager.s2c(), TendrilPacket.TYPE,
                    TendrilPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> TendrilStrands.show(
                            payload.key(), payload.x(), payload.y(), payload.z(), payload.offsets(),
                            payload.width(), payload.ticks(), payload.tint())));
            NetworkManager.registerReceiver(NetworkManager.s2c(), PillarFxPacket.TYPE,
                    PillarFxPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> PillarClient.fx(payload)));
            NetworkManager.registerReceiver(NetworkManager.s2c(), PillarStatePacket.TYPE,
                    PillarStatePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> PillarClient.state(
                            payload.entityId(), payload.toggles(), payload.stone())));
            NetworkManager.registerReceiver(NetworkManager.s2c(), WingsStatePacket.TYPE,
                    WingsStatePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> UltimateWingsLayer.state(
                            payload.entityId(), payload.out(), payload.flap())));
            NetworkManager.registerReceiver(NetworkManager.s2c(), PossessionStatePacket.TYPE,
                    PossessionStatePacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> PossessionClient.state(
                            payload.entityId(), payload.asVictim(), payload.on(), payload.merged())));
            NetworkManager.registerReceiver(NetworkManager.s2c(), PossessionInputPacket.TYPE,
                    PossessionInputPacket.STREAM_CODEC,
                    (payload, context) -> context.queue(() -> PossessionClient.relayed(
                            payload.yaw(), payload.pitch(), payload.x(), payload.y(), payload.z())));
        }

        NetworkManager.registerReceiver(NetworkManager.c2s(), UnlockNodePacket.TYPE, UnlockNodePacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
                        return;
                    }

                    SkillNode node = SkillTrees
                            .byId(ResourceLocation.tryParse(payload.nodeId()));

                    JojohaPlayerData data =
                            PlayerDataAccess.get(serverPlayer);

                    if (!SkillTrees.canUnlock(serverPlayer, data, node)) {
                        return;
                    }

                    for (SkillNode.ItemRequirement cost : node.items()) {
                        cost.consume(serverPlayer);
                    }

                    data.grantNode(node.id());

                    HamonPath path =
                            SkillTrees.pathOf(node);
                    if (path != null) {
                        data.grantHamonPath(path.id());
                        JojohaAdvancements.award(serverPlayer,
                                "part2/path_" + path.id().getPath());
                        if (HamonPaths.all().stream()
                                .allMatch(known -> data.hasHamonPath(known.id()))) {
                            JojohaAdvancements.award(serverPlayer, "part2/every_path");
                        }
                    }

                    PlayerDataAccess.set(serverPlayer, data);
                    PlayerDataAccess.sync(serverPlayer);
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), PossessionSteerPacket.TYPE,
                PossessionSteerPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        Possession.steer(serverPlayer, payload.yaw(), payload.pitch(),
                                payload.x(), payload.y(), payload.z());
                    }
                }));
        NetworkManager.registerReceiver(NetworkManager.c2s(), PossessionAttackPacket.TYPE,
                PossessionAttackPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        Possession.attack(serverPlayer);
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), PilotPosePacket.TYPE, PilotPosePacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        PilotSystem.applyClientPose(serverPlayer, payload.x(), payload.y(),
                                payload.z(), payload.yRot(), payload.xRot());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), EngageStandTargetPacket.TYPE, EngageStandTargetPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandCombatHandler.handleEngageTarget(serverPlayer, payload.targetId());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), EquipSkillPacket.TYPE, EquipSkillPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
                        return;
                    }

                    if (payload.skillId().isEmpty()) {
                        SkillLoadout.clear(serverPlayer, payload.slot());
                        return;
                    }

                    ResourceLocation id =
                            ResourceLocation.tryParse(payload.skillId());
                    if (id == null) {
                        return;
                    }

                    if (payload.slot() == EquipSkillPacket.UNEQUIP) {
                        SkillLoadout.unequip(serverPlayer, id);
                    } else {
                        SkillLoadout.equip(serverPlayer, id, payload.slot());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), SpendStatPointPacket.TYPE, SpendStatPointPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StatPoints.spend(serverPlayer, payload.stand(),
                                payload.stat(), payload.amount());
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), CycleStandModePacket.TYPE, CycleStandModePacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandCombatHandler.handleCycleMode(serverPlayer);
                    }
                }));

        NetworkManager.registerReceiver(NetworkManager.c2s(), SetStandGuardPacket.TYPE, SetStandGuardPacket.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer serverPlayer) {
                        StandCombatHandler.handleSetGuard(serverPlayer, payload.guarding());
                    }
                }));
    }

    private static final double RITUAL_EFFECT_RADIUS = 96.0;

    public static void broadcastStandNamed(ServerPlayer subject, ResourceLocation standId) {
        broadcastNearby(subject, new StandNamedPacket(subject.getUUID(), standId));
    }

    public static void broadcastRitualEffect(ServerPlayer subject, StandRitualEffectPacket.Effect effect) {
        broadcastNearby(subject, new StandRitualEffectPacket(subject.getUUID(), effect));
    }

    public static void broadcastAfterimages(ServerPlayer subject, int color, int durationTicks) {
        broadcastNearby(subject, new StandAfterimagePacket(subject.getUUID(), color, durationTicks));
    }

    public static void broadcastMovePose(ServerPlayer subject, String animation) {
        broadcastMovePose(subject, animation, PlayerMovePosePacket.ONCE);
    }

    public static void broadcastMovePose(ServerPlayer subject, String animation, int mode) {
        broadcastNearby(subject,
                new PlayerMovePosePacket(subject.getId(), animation, mode));
    }

    public static void broadcastEntityPose(Entity subject, String animation, int mode) {
        if (!(subject.level() instanceof ServerLevel level)) {
            return;
        }
        double radiusSqr = RITUAL_EFFECT_RADIUS * RITUAL_EFFECT_RADIUS;
        PlayerMovePosePacket packet = new PlayerMovePosePacket(subject.getId(), animation, mode);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(subject) <= radiusSqr) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    public static void broadcastParryCaught(ServerPlayer defender, Vec3 from, boolean perfect, boolean weapon) {
        Vec3 look = defender.getLookAngle();
        Vec3 toward = from.subtract(defender.position());
        float side = (float) Math.signum(look.x * toward.z - look.z * toward.x);
        broadcastNearby(defender, new ParryCaughtPacket(defender.getId(), perfect,
                weapon, side == 0F ? 1F : side));
    }

    public static void broadcastMovementCue(ServerPlayer subject, MovementCuePacket cue) {
        broadcastNearby(subject, cue);
    }

    public static void broadcastStunned(LivingEntity subject) {
        if (!(subject.level() instanceof ServerLevel level)) {
            return;
        }
        double radiusSqr = RITUAL_EFFECT_RADIUS * RITUAL_EFFECT_RADIUS;
        StunPosePacket packet =
                new StunPosePacket(subject.getId());
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(subject) <= radiusSqr) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    public static void broadcastTangled(LivingEntity subject, int ticks) {
        if (!(subject.level() instanceof ServerLevel level)) {
            return;
        }
        double radiusSqr = RITUAL_EFFECT_RADIUS * RITUAL_EFFECT_RADIUS;
        TangledPacket packet =
                new TangledPacket(subject.getId(), ticks);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(subject) <= radiusSqr) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    public static void broadcastMenace(ServerPlayer subject) {
        broadcastNearby(subject, new MenacePacket(subject.getId(), 0));
    }

    public static void broadcastTension(ServerPlayer subject) {
        broadcastNearby(subject, new MenacePacket(subject.getId(), 1));
    }

    public static void sendPalette(ServerPlayer player, Palette palette, int ticks) {
        NetworkManager.sendToPlayer(player,
                new PaletteCuePacket(palette.ordinal(), ticks));
    }

    public static void broadcastFlair(LivingEntity subject, int kind, int amount) {
        if (!(subject.level() instanceof ServerLevel level)) {
            return;
        }
        double radiusSqr = RITUAL_EFFECT_RADIUS * RITUAL_EFFECT_RADIUS;
        FlairCuePacket packet =
                new FlairCuePacket(subject.getId(), kind, amount);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(subject) <= radiusSqr) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    private static void broadcastNearby(ServerPlayer subject, CustomPacketPayload packet) {
        double radiusSqr = RITUAL_EFFECT_RADIUS * RITUAL_EFFECT_RADIUS;

        for (ServerPlayer viewer : subject.serverLevel().players()) {
            if (viewer == subject || viewer.distanceToSqr(subject) <= radiusSqr) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    public static void sendHeavenFx(ServerPlayer player, int kind, int ticks) {
        NetworkManager.sendToPlayer(player, HeavenFxPacket.screen(kind, ticks));
    }

    public static void sendPlayerData(ServerPlayer player, JojohaPlayerData data) {
        NetworkManager.sendToPlayer(player, new SyncPlayerDataPacket(data));
    }

    public static void sendUseMove(ResourceLocation moveId) {
        NetworkManager.sendToServer(new UseHamonMovePacket(moveId));
    }

    public static void sendToggleStandSummon() {
        NetworkManager.sendToServer(new ToggleStandSummonPacket());
    }

    public static void sendDreamSpeech(ServerPlayer player,
                                       DreamSpeechPacket speech) {
        NetworkManager.sendToPlayer(player, speech);
    }

    public static void sendClackerThrow() {
        NetworkManager.sendToServer(new ClackerThrowPacket());
    }

    public static void sendTimeStopThrow() {
        NetworkManager.sendToServer(new TimeStopThrowPacket());
    }

    public static void sendClackerSwing(ServerPlayer viewer, ServerPlayer swinger, int kind, int targetId) {
        NetworkManager.sendToPlayer(viewer,
                new ClackerSwingPacket(swinger.getId(), kind, targetId));
    }

    public static void sendScarfStrike(ServerPlayer viewer, ServerPlayer striker, int targetId) {
        NetworkManager.sendToPlayer(viewer,
                new ScarfStrikePacket(striker.getId(), targetId));
    }

    public static void sendMoveInput(int forward, int left) {
        NetworkManager.sendToServer(new MoveInputPacket((byte) forward, (byte) left));
    }

    public static void sendRequestDoubleJump(Vec3 travel) {
        NetworkManager.sendToServer(new RequestDoubleJumpPacket(travel.x, travel.z));
    }

    public static void sendRequestParry(int variant) {
        NetworkManager.sendToServer(new RequestParryPacket(variant));
    }

    public static void sendDreamAnswer(int turn, int answer) {
        NetworkManager.sendToServer(new DreamAnswerPacket(turn, answer));
    }

    public static void sendEquipStandSkin(int skin) {
        NetworkManager.sendToServer(new EquipStandSkinPacket(skin));
    }

    public static void sendWorkshopStyle(int summon, int shout, int rest) {
        NetworkManager.sendToServer(new WorkshopStylePacket(summon, shout, rest));
    }

    public static void sendWorkshopPose(int slot, StandPose pose) {
        NetworkManager.sendToServer(new WorkshopPosePacket(slot, pose));
    }

    public static void sendWorkshopLook(int slot, boolean save, int skin) {
        NetworkManager.sendToServer(new WorkshopLookPacket(slot, save, skin));
    }

    public static void sendStandPlacement(StandPlacement placement) {
        NetworkManager.sendToServer(new StandPlacementPacket(placement.side(),
                placement.height(), placement.ahead(), placement.turn()));
    }

    public static void broadcastWings(ServerPlayer subject, boolean out, boolean flap) {
        broadcastNearby(subject, new WingsStatePacket(subject.getId(), out, flap));
    }

    public static void sendWingFlap() {
        NetworkManager.sendToServer(new WingFlapPacket());
    }

    public static void sendEquipAdaptation(String adaptation, boolean active, int slot) {
        NetworkManager.sendToServer(new EquipAdaptationPacket(adaptation, active, slot));
    }

    public static void sendChooseDash(boolean timeSkip) {
        NetworkManager.sendToServer(new ChooseDashPacket(timeSkip));
    }

    public static void sendStandIdle(String key, String clip) {
        NetworkManager.sendToServer(new StandIdlePacket(key, clip));
    }

    public static void sendRequestStandIdle(int stamp) {
        NetworkManager.sendToServer(new RequestStandIdlePacket(stamp));
    }

    public static void sendRequestStandPunch(int aimedId) {
        NetworkManager.sendToServer(new RequestStandPunchPacket(aimedId));
    }

    public static void sendStandUseItem(Optional<BlockPos> stretchAnchor,
                                       int turns) {
        NetworkManager.sendToServer(new StandUseItemPacket(stretchAnchor, turns));
    }

    public static void sendSetStandGuard(boolean guarding) {
        NetworkManager.sendToServer(new SetStandGuardPacket(guarding));
    }

    public static void sendEquipPassive(ResourceLocation passiveId) {
        NetworkManager.sendToServer(
                new EquipPassivePacket(passiveId.toString()));
    }

    public static void sendStandCharge(boolean charging) {
        NetworkManager.sendToServer(new StandChargePacket(charging));
    }

    public static void sendUseStandSkill(int slot, int chargeTicks) {
        NetworkManager.sendToServer(new UseStandSkillPacket(slot, chargeTicks));
    }

    public static void sendFirstPersonPose(ServerPlayer player, String pose) {
        sendFirstPersonPose(player, pose, 0);
    }

    public static void sendFirstPersonPose(ServerPlayer player, String pose, int ticks) {
        NetworkManager.sendToPlayer(player,
                new FirstPersonPosePacket(pose, ticks));
    }

    public static void clearFirstPersonPose(ServerPlayer player) {
        sendFirstPersonPose(player, "", 0);
    }

    public static void releaseFirstPersonPose(ServerPlayer player, String pose) {
        sendFirstPersonPose(player, pose, FirstPersonPosePacket.RELEASE);
    }

    public static void sendDowsing(ServerLevel level, ServerPlayer player,
                                   BlockPos target, int ticks) {
        DowsingPacket packet =
                new DowsingPacket(player.getId(), target, ticks);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(player) < 64 * 64) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    public static void sendPreciseEyes(ServerPlayer player, boolean on) {
        NetworkManager.sendToPlayer(player, new PreciseEyesPacket(on));
    }

    public static void sendClairvoyance(ServerPlayer player, int ticks) {
        NetworkManager.sendToPlayer(player,
                new ClairvoyancePacket(ticks));
    }

    private static final double FIST_VIEW_RANGE = 64.0;

    public static void broadcastThrown(ServerLevel level, Entity body, float heading,
                                       float spin) {
        ThrownPacket packet =
                new ThrownPacket(body.getId(), heading, spin);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(body) <= FIST_VIEW_RANGE * FIST_VIEW_RANGE) {
                NetworkManager.sendToPlayer(player, packet);
            }
        }
    }

    public static void broadcastStandFist(ServerLevel level, Entity stand,
                                          StandFist fist) {
        StandFistPacket packet =
                StandFistPacket.of(stand.getId(), fist);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(stand) <= FIST_VIEW_RANGE * FIST_VIEW_RANGE) {
                NetworkManager.sendToPlayer(player, packet);
            }
        }
    }

    public static void broadcastMagician(ServerLevel level, Vec3 near, double range,
                                         MagicianFxPacket packet) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(near) <= range * range) {
                NetworkManager.sendToPlayer(player, packet);
            }
        }
    }

    public static void broadcastSabreArc(ServerLevel level, Vec3 at, float radius, float start, float charge) {
        SabreArcPacket packet =
                new SabreArcPacket(at.x, at.y, at.z, radius, start, charge);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at) <= 96.0 * 96.0) {
                NetworkManager.sendToPlayer(player, packet);
            }
        }
    }

    public static void broadcastGroundBreaker(ServerLevel level, Entity thrower,
                                              Vec3 at, float radius, int phase, int ticks) {
        GroundBreakerFxPacket packet =
                new GroundBreakerFxPacket(
                        thrower.getId(), at.x, at.y, at.z, radius, phase, ticks);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at) <= 96.0 * 96.0) {
                NetworkManager.sendToPlayer(player, packet);
            }
        }
    }

    public static void broadcastMeteorImpact(ServerLevel level, BlockPos at,
                                             double range, float scale) {
        MeteorImpactPacket packet =
                new MeteorImpactPacket(
                        at.getX() + 0.5, at.getY(), at.getZ() + 0.5, scale);

        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at.getX() + 0.5, at.getY(), at.getZ() + 0.5) <= range * range) {
                NetworkManager.sendToPlayer(player, packet);
            }
        }
    }

    public static void broadcastTimeStop(ServerLevel level, Vec3 centre, int remainingTicks,
                                         boolean active, List<Integer> frozenPlayers, double radius) {
        broadcastTimeStop(level, centre, remainingTicks, active, frozenPlayers, radius, 0, 0);
    }

    public static void broadcastTimeStop(ServerLevel level, Vec3 centre, int remainingTicks,
                                         boolean active, List<Integer> frozenPlayers, double radius,
                                         int quietTicks, int introTicks) {
        TimeStopStatePacket packet = active
                ? new TimeStopStatePacket(true, centre.x, centre.y, centre.z, remainingTicks,
                        List.copyOf(frozenPlayers), radius, quietTicks, introTicks)
                : TimeStopStatePacket.ended();
        boolean everywhere = TimeStopSystem.global(radius);

        for (ServerPlayer player : level.players()) {
            if (everywhere || player.distanceToSqr(centre) <= TIME_STOP_VIEW_RANGE * TIME_STOP_VIEW_RANGE) {
                NetworkManager.sendToPlayer(player, packet);
            }
        }
    }

    private static final double TIME_STOP_VIEW_RANGE = 96.0;

    public static void sendThornLash(ServerLevel level,
                                     Entity from,
                                     Entity to, int ticks) {
        sendThornLash(level, from, to, ticks, -1);
    }

    public static void sendThornLash(ServerLevel level,
                                     Entity from,
                                     Entity to, int ticks, int coilAfter) {
        ThornLashPacket packet =
                new ThornLashPacket(from.getId(), to.getId(), ticks, coilAfter);

        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(from) < 64 * 64) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    public static void sendMeteorStreak(ServerPlayer viewer, Vec3 from, Vec3 velocity, int life, boolean big) {
        NetworkManager.sendToPlayer(viewer, new MeteorStreakPacket(from.x, from.y, from.z,
                (float) velocity.x, (float) velocity.y, (float) velocity.z, life, big));
    }

    public static void sendZoomPunch(ServerLevel level,
                                     Entity from, Vec3 to,
                                     int ticks) {
        ZoomPunchPacket packet =
                new ZoomPunchPacket(from.getId(), to.x, to.y, to.z,
                        ticks);

        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(from) < 64 * 64) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    public static void sendClimbing(ServerPlayer player,
                                    Climbing.Grip grip,
                                    boolean climbing) {
        ClimbingPacket packet =
                new ClimbingPacket(
                        player.getUUID(), grip.ordinal(), climbing);
        for (ServerPlayer viewer : player.serverLevel().players()) {
            if (viewer.distanceToSqr(player) < 64 * 64) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    private static final double PILLAR_FX_RANGE = 96.0;

    public static void sendPillarFx(ServerLevel level, PillarFxKind kind,
                                    int a, int b, int ticks) {
        Entity at = level.getEntity(a);
        Vec3 where = at == null ? Vec3.ZERO : at.position();
        sendPillarFx(level, kind, a, b, ticks, where, Vec3.ZERO);
    }

    public static void sendPillarFx(ServerLevel level, PillarFxKind kind,
                                    int a, int b, int ticks, Vec3 at,
                                    Vec3 dir) {
        PillarFxPacket packet = new PillarFxPacket(
                kind.ordinal(), a, b, ticks, at.x, at.y, at.z, (float) dir.x, (float) dir.y, (float) dir.z);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.position().distanceToSqr(at) < PILLAR_FX_RANGE * PILLAR_FX_RANGE) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    public static void sendPillarState(ServerPlayer player, int toggles, int stone) {
        PillarStatePacket packet =
                new PillarStatePacket(player.getId(), toggles, stone);
        for (ServerPlayer viewer : player.serverLevel().players()) {
            if (viewer == player || viewer.distanceToSqr(player) < PILLAR_FX_RANGE * PILLAR_FX_RANGE) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    public static void sendSunScorch(ServerPlayer player, float exposure) {
        NetworkManager.sendToPlayer(player,
                new SunScorchPacket(exposure));
    }

    public static void sendSpaceRipper(ServerLevel level,
                                       Entity from,
                                       List<Vec3> points,
                                       int eye, int fireTicks, int ticks) {
        if (points.size() < 2) {
            return;
        }

        Vec3 start = points.get(0);
        List<Float> offsets = new ArrayList<>();
        for (int i = 1; i < points.size(); i++) {
            Vec3 point = points.get(i);
            offsets.add((float) (point.x - start.x));
            offsets.add((float) (point.y - start.y));
            offsets.add((float) (point.z - start.z));
        }

        SpaceRipperPacket packet =
                new SpaceRipperPacket(from.getId(),
                        start.x, start.y, start.z, offsets, eye, fireTicks, ticks);

        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(from) < BEAM_VIEW_RANGE * BEAM_VIEW_RANGE) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    public static void sendFortuneSense(ServerPlayer player, int kind, int ticks, List<Integer> ids) {
        NetworkManager.sendToPlayer(player,
                new FortuneSensePacket(kind, ticks, ids));
    }

    public static void sendSubzero(ServerPlayer player, int ticks) {
        broadcastNearby(player,
                new SubzeroPacket(player.getUUID(), ticks));
    }

    public static void sendVampireMark(ServerLevel level,
                                       LivingEntity victim,
                                       VampireMarkPacket.Mark mark,
                                       int ticks) {
        VampireMarkPacket packet =
                new VampireMarkPacket(victim.getId(), mark, ticks);

        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(victim) < MARK_VIEW_RANGE * MARK_VIEW_RANGE) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    private static final double MARK_VIEW_RANGE = 64.0;

    private static final double BEAM_VIEW_RANGE = 96.0;

    public static void sendAnubisWielders(ServerPlayer viewer, List<Integer> ids) {
        NetworkManager.sendToPlayer(viewer, new AnubisWieldersPacket(ids));
    }

    public static void sendSkullFlash(ServerLevel level,
                                      LivingEntity victim,
                                      ServerPlayer attacker, int phase, int ticks) {
        SkullFlashPacket packet =
                new SkullFlashPacket(victim.getId(), attacker.getId(), phase, ticks);
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(victim) < 64 * 64) {
                NetworkManager.sendToPlayer(viewer, packet);
            }
        }
    }

    public static void sendUnlockNode(ResourceLocation nodeId) {
        NetworkManager.sendToServer(new UnlockNodePacket(nodeId.toString()));
    }

    public static void sendPilotPose(double x, double y, double z, float yRot, float xRot) {
        NetworkManager.sendToServer(new PilotPosePacket(x, y, z, yRot, xRot));
    }

    public static void sendEngageStandTarget(int targetId) {
        NetworkManager.sendToServer(new EngageStandTargetPacket(targetId));
    }

    public static void sendEquipSkill(ResourceLocation skillId, int slot) {
        NetworkManager.sendToServer(new EquipSkillPacket(skillId.toString(), slot));
    }

    public static void sendClearSlot(int slot) {
        NetworkManager.sendToServer(new EquipSkillPacket(EquipSkillPacket.CLEAR, slot));
    }

    public static void sendUnequipSkill(ResourceLocation skillId) {
        NetworkManager.sendToServer(new EquipSkillPacket(skillId.toString(), EquipSkillPacket.UNEQUIP));
    }

    public static void sendSpendStatPoint(boolean stand, int stat, int amount) {
        NetworkManager.sendToServer(new SpendStatPointPacket(stand, stat, amount));
    }

    public static void sendToggleHalfSummon() {
        NetworkManager.sendToServer(new ToggleHalfSummonPacket());
    }

    public static void sendCycleStandMode() {
        NetworkManager.sendToServer(new CycleStandModePacket());
    }
}
