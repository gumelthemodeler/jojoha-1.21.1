package org.gumel.jojoha.stand.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import org.gumel.jojoha.accessory.Charms;
import org.gumel.jojoha.combat.ComboLink;
import org.gumel.jojoha.combat.MovementCharges;
import org.gumel.jojoha.combat.StatPerkEffects;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandGrowth;
import org.gumel.jojoha.data.StatEffects;
import org.gumel.jojoha.data.StatPerk;
import org.gumel.jojoha.fortune.Trickster;
import org.gumel.jojoha.hamon.moves.NextLineMove;
import org.gumel.jojoha.pillar.PillarMen;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.skill.CastDing;
import org.gumel.jojoha.skill.Moves;
import org.gumel.jojoha.skilltree.SkillTrees;
import org.gumel.jojoha.stand.DividedAttention;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandRange;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.StandUtilityWork;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.hierophant.Possession;
import org.gumel.jojoha.stand.magician.MagicianMove;
import org.gumel.jojoha.stand.passive.TangledPassive;
import org.gumel.jojoha.stand.skill.moves.AfterimagesSkill;
import org.gumel.jojoha.stand.skill.moves.ArmorReleaseSkill;
import org.gumel.jojoha.stand.skill.moves.BalestraSkill;
import org.gumel.jojoha.stand.skill.moves.BarrageSkill;
import org.gumel.jojoha.stand.skill.moves.BeatdownSkill;
import org.gumel.jojoha.stand.skill.moves.BladeLaunchSkill;
import org.gumel.jojoha.stand.skill.moves.BodyPossessionSkill;
import org.gumel.jojoha.stand.skill.moves.BuildModeSkill;
import org.gumel.jojoha.stand.skill.moves.BurstSkill;
import org.gumel.jojoha.stand.skill.moves.CameraCrushSkill;
import org.gumel.jojoha.stand.skill.moves.ChoreSkill;
import org.gumel.jojoha.stand.skill.moves.ClairvoyanceSkill;
import org.gumel.jojoha.stand.skill.moves.ClearingSkill;
import org.gumel.jojoha.stand.skill.moves.DeflectionSkill;
import org.gumel.jojoha.stand.skill.moves.DeliverySkill;
import org.gumel.jojoha.stand.skill.moves.DodgeSkill;
import org.gumel.jojoha.stand.skill.moves.DoubleJumpSkill;
import org.gumel.jojoha.stand.skill.moves.DowsingSkill;
import org.gumel.jojoha.stand.skill.moves.DrawSkill;
import org.gumel.jojoha.stand.skill.moves.EmeraldRadiusSkill;
import org.gumel.jojoha.stand.skill.moves.EmeraldSplashBarrageSkill;
import org.gumel.jojoha.stand.skill.moves.EmeraldSplashShotgunSkill;
import org.gumel.jojoha.stand.skill.moves.ErasureSkill;
import org.gumel.jojoha.stand.skill.moves.GrabSkill;
import org.gumel.jojoha.stand.skill.moves.GreatestHighSkill;
import org.gumel.jojoha.stand.skill.moves.GroundBreakerSkill;
import org.gumel.jojoha.stand.skill.moves.GroundSlamSkill;
import org.gumel.jojoha.stand.skill.moves.GutPunchSkill;
import org.gumel.jojoha.stand.skill.moves.HeavenStepSkill;
import org.gumel.jojoha.stand.skill.moves.HeavenTimeStopSkill;
import org.gumel.jojoha.stand.skill.moves.HeavensUppercutSkill;
import org.gumel.jojoha.stand.skill.moves.HeavyPunchSkill;
import org.gumel.jojoha.stand.skill.moves.HermitGrappleSkill;
import org.gumel.jojoha.stand.skill.moves.InfiniteServitudeSkill;
import org.gumel.jojoha.stand.skill.moves.InhaleSkill;
import org.gumel.jojoha.stand.skill.moves.JudgementFistSkill;
import org.gumel.jojoha.stand.skill.moves.KickFlurrySkill;
import org.gumel.jojoha.stand.skill.moves.KilnSkill;
import org.gumel.jojoha.stand.skill.moves.KnifeBarrageSkill;
import org.gumel.jojoha.stand.skill.moves.KnifeHaloSkill;
import org.gumel.jojoha.stand.skill.moves.LassoOfThornsSkill;
import org.gumel.jojoha.stand.skill.moves.MiniatureTrapSkill;
import org.gumel.jojoha.stand.skill.moves.MoveTickers;
import org.gumel.jojoha.stand.skill.moves.MudaRushSkill;
import org.gumel.jojoha.stand.skill.moves.OverwriteSkill;
import org.gumel.jojoha.stand.skill.moves.PilotSkill;
import org.gumel.jojoha.stand.skill.moves.PreciseEyesSkill;
import org.gumel.jojoha.stand.skill.moves.RealityBendSkill;
import org.gumel.jojoha.stand.skill.moves.RecallStandSkill;
import org.gumel.jojoha.stand.skill.moves.RestorationSkill;
import org.gumel.jojoha.stand.skill.moves.RiposteSkill;
import org.gumel.jojoha.stand.skill.moves.SabreCycleSkill;
import org.gumel.jojoha.stand.skill.moves.SkullCrusherSkill;
import org.gumel.jojoha.stand.skill.moves.StandDashSkill;
import org.gumel.jojoha.stand.skill.moves.StandLeapSkill;
import org.gumel.jojoha.stand.skill.moves.StarFingerSkill;
import org.gumel.jojoha.stand.skill.moves.SwordFlurrySkill;
import org.gumel.jojoha.stand.skill.moves.TendrilSpikeSkill;
import org.gumel.jojoha.stand.skill.moves.TendrilWorkSkill;
import org.gumel.jojoha.stand.skill.moves.ThornWhipSkill;
import org.gumel.jojoha.stand.skill.moves.ThornZipSkill;
import org.gumel.jojoha.stand.skill.moves.TimeSkipSkill;
import org.gumel.jojoha.stand.skill.moves.TimeStopSkill;
import org.gumel.jojoha.stand.skill.moves.TimeStopUpgradeSkill;
import org.gumel.jojoha.stand.skill.moves.TwistingGutPunchSkill;
import org.gumel.jojoha.stand.skill.moves.UnravelTrapSkill;
import org.gumel.jojoha.stand.skill.moves.UppercutSkill;
import org.gumel.jojoha.stand.skill.moves.WebSwingSkill;
import org.gumel.jojoha.vampire.Headless;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StandSkills {
    public static final int RELEASE = -1;

    public static final int CHARGE_BEGIN = -2;

    public static final int CHARGE_BROKEN = -3;

    public static final int CHARGE_DROPPED = -4;

    public static final int SLOTS_PER_PAGE = 8;
    public static final int PAGE_COUNT = 2;
    public static final int SLOT_COUNT = SLOTS_PER_PAGE * PAGE_COUNT;

    private static final Map<ResourceLocation, StandSkill> SKILLS = new LinkedHashMap<>();

    private static final List<StandSkill> CLOSE_RANGE_GENERICS = List.of(
            BarrageSkill.INSTANCE, UppercutSkill.INSTANCE, GrabSkill.INSTANCE,
            StandLeapSkill.INSTANCE, StandDashSkill.INSTANCE, TimeSkipSkill.INSTANCE);
    private static final List<StandSkill> LONG_RANGE_GENERICS = List.of(
            BarrageSkill.INSTANCE, UppercutSkill.INSTANCE, GrabSkill.INSTANCE,
            PilotSkill.INSTANCE, StandDashSkill.INSTANCE, TimeSkipSkill.INSTANCE);

    static {
        register(BarrageSkill.INSTANCE);
        register(UppercutSkill.INSTANCE);
        register(GrabSkill.INSTANCE);
        register(StandLeapSkill.INSTANCE);
        register(StandDashSkill.INSTANCE);
        register(PilotSkill.INSTANCE);
        register(HermitGrappleSkill.INSTANCE);
        register(ThornZipSkill.INSTANCE);
        register(LassoOfThornsSkill.INSTANCE);
        register(CameraCrushSkill.INSTANCE);
        register(ClairvoyanceSkill.INSTANCE);
        register(ThornWhipSkill.INSTANCE);
        register(TwistingGutPunchSkill.INSTANCE);
        register(InhaleSkill.INSTANCE);
        register(StarFingerSkill.INSTANCE);
        register(HeavyPunchSkill.INSTANCE);
        register(DodgeSkill.INSTANCE);
        register(DoubleJumpSkill.INSTANCE);
        register(DrawSkill.INSTANCE);
        register(GutPunchSkill.INSTANCE);
        register(GroundSlamSkill.INSTANCE);
        register(TimeStopSkill.INSTANCE);
        register(TimeSkipSkill.INSTANCE);
        register(TimeStopUpgradeSkill.EXTENDED);
        register(SkullCrusherSkill.INSTANCE);

        register(GroundBreakerSkill.INSTANCE);
        register(KnifeBarrageSkill.INSTANCE);
        register(BurstSkill.INSTANCE);
        register(KickFlurrySkill.INSTANCE);
        register(BeatdownSkill.INSTANCE);
        register(TimeStopUpgradeSkill.PERFECT);

        register(EmeraldSplashShotgunSkill.INSTANCE);
        register(EmeraldSplashBarrageSkill.INSTANCE);
        register(UnravelTrapSkill.INSTANCE);
        register(WebSwingSkill.INSTANCE);
        register(BodyPossessionSkill.INSTANCE);
        register(MiniatureTrapSkill.INSTANCE);
        register(EmeraldRadiusSkill.INSTANCE);
        register(TendrilSpikeSkill.INSTANCE);

        register(SwordFlurrySkill.INSTANCE);
        register(BalestraSkill.INSTANCE);
        register(BladeLaunchSkill.INSTANCE);
        register(SabreCycleSkill.INSTANCE);
        register(DeflectionSkill.INSTANCE);
        register(RiposteSkill.INSTANCE);
        register(ArmorReleaseSkill.INSTANCE);
        register(AfterimagesSkill.INSTANCE);

        register(MagicianMove.CONTROLLED_BLAZE);
        register(MagicianMove.SWEEP_KICK);
        register(MagicianMove.CROSSFIRE_HURRICANE);
        register(MagicianMove.CROSSFIRE_HURRICANE_SPECIAL);
        register(MagicianMove.RED_BIND);
        register(MagicianMove.INFERNO);
        register(MagicianMove.CONTAINMENT);
        register(MagicianMove.LIFE_SENSE);
        register(MagicianMove.INCREASE_TEMPERATURE);
        register(MagicianMove.INFINITE_INFERNO_FIRE);
        register(MagicianMove.FINGER_SNAP);

        register(MudaRushSkill.INSTANCE);
        register(GreatestHighSkill.INSTANCE);
        register(HeavenTimeStopSkill.INSTANCE);
        register(InfiniteServitudeSkill.INSTANCE);
        register(RestorationSkill.INSTANCE);
        register(ErasureSkill.INSTANCE);
        register(OverwriteSkill.INSTANCE);
        register(RealityBendSkill.INSTANCE);
        register(HeavensUppercutSkill.INSTANCE);
        register(JudgementFistSkill.INSTANCE);
        register(HeavenStepSkill.INSTANCE);
        register(KnifeHaloSkill.INSTANCE);

        register(BuildModeSkill.SINGLE);
        register(BuildModeSkill.ROW);
        register(BuildModeSkill.COLUMN);
        register(BuildModeSkill.FREE);
        register(BuildModeSkill.SELECTION);
        register(RecallStandSkill.INSTANCE);
        register(DowsingSkill.INSTANCE);
        register(ChoreSkill.FETCH);
        register(ChoreSkill.HARVEST);
        register(ChoreSkill.REACH);
        register(ChoreSkill.FELL);
        register(ChoreSkill.CARRY);
        register(PreciseEyesSkill.INSTANCE);
        register(DeliverySkill.INSTANCE);
        register(TendrilWorkSkill.INSTANCE);
        register(ClearingSkill.INSTANCE);
        register(KilnSkill.INSTANCE);
    }

    private StandSkills() {
    }

    public static void init() {
        MoveTickers.init();
    }

    private static void register(StandSkill skill) {
        SKILLS.put(skill.id(), skill);
    }

    public static Collection<StandSkill> all() {
        return SKILLS.values();
    }

    public static StandSkill byId(ResourceLocation id) {
        return SKILLS.get(id);
    }

    private static StandRange rangeOverride;

    public static void setRangeOverride(StandRange range) {
        rangeOverride = range;
    }

    public static final List<StandSkill> UTILITY_TOOLS = List.of(
            BuildModeSkill.SINGLE, BuildModeSkill.ROW, BuildModeSkill.COLUMN, BuildModeSkill.FREE,
            BuildModeSkill.SELECTION,
            RecallStandSkill.INSTANCE, BarrageSkill.INSTANCE,
            ChoreSkill.FETCH, ChoreSkill.HARVEST,
            ChoreSkill.REACH, ChoreSkill.FELL,
            ChoreSkill.CARRY);

    public static final Map<ResourceLocation, StandSkill> SIGNATURE_TOOLS = Map.of(
            StandTypes.STAR_PLATINUM_ID, PreciseEyesSkill.INSTANCE,
            StandTypes.THE_WORLD_ID, DeliverySkill.INSTANCE,
            StandTypes.HERMIT_PURPLE_ID, DowsingSkill.INSTANCE,
            StandTypes.HIEROPHANT_GREEN_ID, TendrilWorkSkill.INSTANCE,
            StandTypes.SILVER_CHARIOT_ID, ClearingSkill.INSTANCE,
            StandTypes.MAGICIANS_RED_ID, KilnSkill.INSTANCE);

    public static List<StandSkill> movesetFor(JojohaPlayerData data) {
        StandType type = StandTypes.byIdOrDefault(data.stand.standId());
        return data.standMode.handlesItems() ? utilityToolsFor(type) : movesetFor(type);
    }

    public static List<StandSkill> utilityToolsFor(StandType type) {
        List<StandSkill> moveset = movesetFor(type);
        List<StandSkill> tools = new ArrayList<>(UTILITY_TOOLS.size());
        for (StandSkill tool : UTILITY_TOOLS) {
            StandSkill own = byId(supplanting(moveset, tool.id()));
            tools.add(own == null ? tool : own);
        }
        StandSkill signature = SIGNATURE_TOOLS.get(type.id());
        if (signature != null) {
            tools.add(signature);
        }
        return tools;
    }

    public static List<StandSkill> movesetFor(StandType type) {
        StandRange range = rangeOverride != null ? rangeOverride : type.range();
        List<StandSkill> moveset = new ArrayList<>(type.form().isFreeStanding()
                ? (range == StandRange.LONG ? LONG_RANGE_GENERICS : CLOSE_RANGE_GENERICS)
                : List.<StandSkill>of());

        for (ResourceLocation id : type.signatureSkills()) {
            StandSkill skill = byId(id);
            if (skill == null || moveset.contains(skill)) {
                continue;
            }
            int supplanted = skill.supplants() ? moveset.indexOf(byId(skill.replaces())) : -1;
            if (supplanted >= 0) {
                moveset.set(supplanted, skill);
            } else {
                moveset.add(skill);
            }
        }

        return moveset;
    }

    public static StandSkill skillInSlot(JojohaPlayerData data, int slot) {
        return byId(idInSlot(data, slot));
    }

    public static ResourceLocation idInSlot(JojohaPlayerData data, int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return null;
        }

        ResourceLocation equipped = data.equippedSkill(slot);
        if (equipped == null) {
            return null;
        }

        if (data.standOut() && data.stand.isPresent()) {
            equipped = supplanting(StandTypes.byIdOrDefault(data.stand.standId()), equipped);
        }

        return StandReinforced.resolve(data, equipped);
    }

    public static ResourceLocation supplanting(StandType type, ResourceLocation id) {
        return supplanting(movesetFor(type), id);
    }

    private static ResourceLocation supplanting(List<StandSkill> moveset, ResourceLocation id) {
        for (StandSkill skill : moveset) {
            if (skill.supplants() && id.equals(skill.replaces())) {
                return skill.id();
            }
        }
        return id;
    }

    public static boolean conflict(StandSkill a, StandSkill b) {
        if (a == null || b == null || a == b) {
            return false;
        }
        return a.id().equals(b.replaces()) || b.id().equals(a.replaces());
    }

    public static boolean utilityTool(JojohaPlayerData data, StandSkill skill) {
        return skill != null && data.stand.isPresent()
                && utilityToolsFor(StandTypes.byIdOrDefault(data.stand.standId())).contains(skill);
    }

    public static final int UTILITY_SLOT = SLOT_COUNT;

    public static boolean toolOnly(StandSkill skill) {
        if (skill == null) {
            return false;
        }
        return skill instanceof BuildModeSkill || skill == RecallStandSkill.INSTANCE
                || skill instanceof ChoreSkill
                || SIGNATURE_TOOLS.containsValue(skill);
    }

    public static boolean fitsUtility(JojohaPlayerData data, ResourceLocation id) {
        StandSkill skill = byId(id);
        if (skill == null) {
            return false;
        }
        if (MovementCharges.kindOf(StandReinforced.baseOf(id)) != null) {
            return true;
        }
        if (!data.stand.isPresent()) {
            return false;
        }
        StandType type = StandTypes.byIdOrDefault(data.stand.standId());
        return utilityToolsFor(type).contains(byId(supplanting(type, id))) || utilityTool(data, skill);
    }

    public static boolean fitsCombat(JojohaPlayerData data, ResourceLocation id) {
        StandSkill skill = byId(id);
        return skill == null || !utilityOnly(data, skill);
    }

    public static boolean utilityOnly(JojohaPlayerData data, StandSkill skill) {
        if (toolOnly(skill)) {
            return true;
        }
        return utilityTool(data, skill)
                && !movesetFor(StandTypes.byIdOrDefault(data.stand.standId())).contains(skill);
    }

    public static boolean inMoveset(JojohaPlayerData data, StandSkill skill) {
        if (skill == null || !data.stand.isPresent()) {
            return false;
        }
        StandType type = StandTypes.byIdOrDefault(data.stand.standId());
        return utilityToolsFor(type).contains(skill) || movesetFor(type).contains(skill);
    }

    public static boolean canEquip(JojohaPlayerData data, StandSkill skill) {
        if (skill == null || StandReinforced.rides(skill.id()) || !skill.isUnlocked(data)) {
            return false;
        }

        if (skill.passiveOnly()) {
            return false;
        }

        if (!skill.requiresStand()) {
            return true;
        }

        if (!data.stand.isPresent()) {
            return false;
        }

        return inMoveset(data, skill);
    }

    public static final int NO_SLOT = -1;

    public static void usePassive(ServerPlayer player, StandSkill skill) {
        if (player.isSpectator() || skill == null || !skill.passiveOnly()) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!skill.isUnlocked(data)) {
            return;
        }
        cast(player, data, skill, StandSummonHandler.findStand(player, data), 0, NO_SLOT);
    }

    public static void handleUseRequest(ServerPlayer player, int slot, int chargeTicks) {
        if (player.isSpectator()) {
            return;
        }
        if (chargeTicks >= 0 && PillarMen.petrified(player)) {
            notify(player, "message.jojoha.pillar.petrified");
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(player);
        ResourceLocation onBar = slot >= 0 && slot < SLOT_COUNT ? data.equippedSkill(slot) : null;
        if (onBar != null && !Moves.canEquip(data, onBar)) {
            SkillLoadout.tidy(player);
            return;
        }
        StandSkill skill = skillInSlot(data, slot);

        if (chargeTicks <= CHARGE_BEGIN) {
            if (skill != null && skill.chargesTime()) {
                TimeStopCharging.heard(player, data, skill, chargeTicks);
            }
            return;
        }

        if (chargeTicks < 0
                && !Moves.isSustained(idInSlot(data, slot))) {
            return;
        }

        String benched = Moves.benched(data, idInSlot(data, slot));
        if (benched != null && chargeTicks >= 0) {
            notify(player, "message.jojoha.skill.utility_stance".equals(benched)
                    && StandUtilityWork.isBusy(player)
                    ? "message.jojoha.skill.utility_working" : benched);
            return;
        }

        if (skill == null) {
            Moves.dispatch(player, idInSlot(data, slot), chargeTicks);
            return;
        }

        if (skill.passiveOnly()) {
            return;
        }

        if (Headless.is(player)) {
            notify(player, "message.jojoha.vampire.headless");
            return;
        }

        int charge = Math.max(0, Math.min(chargeTicks, skill.chargeMaxTicks(data)));
        if (skill.chargesTime()) {
            charge = TimeStopCharging.settle(player, charge);
        }

        if (skill instanceof BuildModeSkill tool) {
            data.buildMode = tool.mode();
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            notify(player, tool.translationKey());
            return;
        }

        if (skill == RecallStandSkill.INSTANCE) {
            StandEntity recalled = StandSummonHandler.findStand(player, data);
            StandUtilityWork.release(player, recalled);
            if (recalled != null) {
                recalled.clearWork();
            }
            notify(player, skill.translationKey());
            return;
        }

        StandEntity stand = StandSummonHandler.findStand(player, data);

        if (skill.isSustained() && (chargeTicks == RELEASE || skill.isSustainActive(player))) {
            if (skill.isSustainActive(player)) {
                skill.activate(player, data, stand, charge);
                PlayerDataAccess.set(player, data);
                PlayerDataAccess.sync(player);
            }
            return;
        }

        if (skill.canBeCalledOff(data)) {
            skill.callOff(player, data, stand);
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            return;
        }

        cast(player, data, skill, stand, charge, slot);
    }

    private static void cast(ServerPlayer player, JojohaPlayerData data, StandSkill skill,
                             StandEntity stand, int charge, int slot) {
        if (slot != NO_SLOT && !data.standOut()) {
            stand = null;
        }

        if (data.standPiloting && skill.movesTheUsersBody()) {
            notify(player, "message.jojoha.skill.piloting");
            return;
        }

        if (Possession.possessing(player)
                && skill != BodyPossessionSkill.INSTANCE) {
            notify(player, "message.jojoha.possession.busy");
            return;
        }

        if (stand == null && skill.requiresStand()) {
            notify(player, "message.jojoha.skill.no_stand");
            return;
        }

        if (!skill.isUnlocked(data)) {
            notify(player, "message.jojoha.skill.not_learned");
            return;
        }

        if (TimeStopSettle.settling(player)) {
            return;
        }

        if (Stun.blocks(player, skill.isAnswerToBeingHit())) {
            notify(player, Stun.freezesMovement(player)
                    ? "message.jojoha.skill.stunned"
                    : "message.jojoha.skill.staggered");
            return;
        }

        MobEffectInstance tangled =
                player.getEffect(ModEffects.tangled());
        if (slot != NO_SLOT && tangled != null && TangledPassive.blocks(
                tangled.getAmplifier(), slot)) {
            notify(player, "message.jojoha.skill.tangled");
            return;
        }

        TrustTier trust = data.stand.trust();
        if (stand != null && trust.level() < requiredTrust(skill).level()) {
            if (skill.requiresStand()) {
                notify(player, "message.jojoha.skill.trust_too_low");
                return;
            }
            stand = null;
        }

        long now = player.level().getGameTime();
        if (data.isMoveOnCooldown(skill.id(), now)) {
            return;
        }
        MovementCharges.Kind charged =
                MovementCharges.kindOf(skill.id());
        if (charged != null && MovementCharges.ready(data, charged, now) == 0) {
            return;
        }

        String unavailable = Heaven.unavailable(player, skill);
        if (unavailable == null) {
            unavailable = skill.unavailable(player, data);
        }
        if (unavailable != null) {
            notify(player, unavailable);
            return;
        }

        float cost = Charms.freeUtility(player, data)
                ? 0F : skill.energyCost(charge) * enduranceCostFactor(data)
                        * DividedAttention.skillCost(data)
                        * Heaven.costScale(data);
        if (data.standEnergy < cost) {
            notify(player, "message.jojoha.skill.no_energy");
            return;
        }

        StandEntity linking = stand;
        boolean fired = linking != null && !skill.movesTheUsersBody() && !skill.isAnswerToBeingHit()
                && ComboLink.linked(player)
                ? ComboLink.link(player, linking, () -> skill.activate(player, data, linking, charge))
                : skill.activate(player, data, stand, charge);
        if (!fired) {
            notify(player, "message.jojoha.skill.no_target");
            return;
        }

        data.standEnergy -= cost;

        StandGrowth.usedSkill(player, skill.id(), cost, skill.cooldownTicks(charge));
        Heaven.cast(player, data, skill);

        if (charged != null) {
            BeatdownSkill.escape(player);
        }

        int lockout = skill.lockoutTicks(data);
        int cooldown = lockout > 0 ? lockout : scaledCooldown(skill, data, charge);
        if (StatPerkEffects.freeRecovery(player, data, skill.cooldownTicks(charge),
                lockout > 0, skill.requiresStand())) {
            cooldown = Math.max(1, Math.round(cooldown * StatPerkEffects.TIME_IS_SHORT_SCALE));
        }
        if (charged != null) {
            MovementCharges.spend(data, charged, now,
                    MovementCharges.RECHARGE_TICKS);
        } else {
            data.setMoveCooldown(skill.id(), now, cooldown);
        }
        CastDing.play(player);
        NextLineMove.cast(player, skill.id());
        Trickster.cast(player, skill.id());

        if (lockout > 0) {
            notify(player, "message.jojoha.skill.spent");
        }

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    public static TrustTier requiredTrust(StandSkill skill) {
        TrustTier own = skill.minimumTrust();
        if ((SkillTrees.firstOnStandTree(skill.id())
                || StandReinforced.readyWhenPartial(skill.id()))
                && own.level() > TrustTier.PARTIAL.level()) {
            return TrustTier.PARTIAL;
        }
        return own;
    }

    public static int scaledCooldown(StandSkill skill, JojohaPlayerData data) {
        return scaledCooldown(skill, data, 0);
    }

    public static int scaledCooldown(StandSkill skill, JojohaPlayerData data, int chargeTicks) {
        return Math.max(1, Math.round(skill.cooldownTicks(chargeTicks)
                * StatEffects.standCooldownScale(data.stand.speed())
                * StatPerkEffects.cooldownFactor(skill.id(), data)
                * Heaven.cooldownScale(data)));
    }

    private static float enduranceCostFactor(JojohaPlayerData data) {
        float factor = StatEffects.standCostScale(data.stand.endurance());
        return StatPerk.EFFICIENT.has(data)
                ? factor * StatPerkEffects.EFFICIENT_COST_SCALE
                : factor;
    }

    public static void notify(ServerPlayer player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey), true);
    }
}
