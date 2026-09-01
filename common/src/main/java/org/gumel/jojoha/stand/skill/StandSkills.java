package org.gumel.jojoha.stand.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandRange;
import org.gumel.jojoha.stand.StandSummonHandler;
import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.skill.moves.BarrageSkill;
import org.gumel.jojoha.stand.skill.moves.GrabSkill;
import org.gumel.jojoha.stand.skill.moves.BuildModeSkill;
import org.gumel.jojoha.stand.skill.moves.RecallStandSkill;
import org.gumel.jojoha.stand.skill.moves.InhaleSkill;
import org.gumel.jojoha.stand.skill.moves.MoveTickers;
import org.gumel.jojoha.stand.skill.moves.PilotSkill;
import org.gumel.jojoha.stand.skill.moves.StandDashSkill;
import org.gumel.jojoha.stand.skill.moves.StandLeapSkill;
import org.gumel.jojoha.stand.skill.moves.StarFingerSkill;
import org.gumel.jojoha.stand.skill.moves.SkullCrusherSkill;
import org.gumel.jojoha.stand.skill.moves.TimeSkipSkill;
import org.gumel.jojoha.stand.skill.moves.TimeStopExtendedSkill;
import org.gumel.jojoha.stand.skill.moves.TimeStopSkill;
import org.gumel.jojoha.stand.skill.moves.UppercutSkill;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The registry of Stand moves, and the server-side gate everything goes through to use one.
 *
 * <p>A Stand's moveset is assembled rather than listed: the generic moves its {@link StandRange}
 * grants, followed by the signature moves named on its {@link StandType}. Composing it this way is
 * what keeps adding a Stand to a one-line asset registration - a new Stand inherits a working
 * moveset from its range class and only has to declare what makes it itself.
 */
public final class StandSkills {
    /**
     * The combat bar has five move slots and two pages, so ten moves are reachable in total.
     *
     * <p>Both numbers come from the bar art rather than the other way round - the five black slots
     * are painted into the sprite, and the page toggle already existed for the bar.
     */
    public static final int SLOTS_PER_PAGE = 8;
    public static final int PAGE_COUNT = 2;
    public static final int SLOT_COUNT = SLOTS_PER_PAGE * PAGE_COUNT;

    private static final Map<ResourceLocation, StandSkill> SKILLS = new LinkedHashMap<>();

    /**
     * Moves every Stand of a given range gets for free.
     *
     * <p>Ordered before signature moves deliberately: the generic pair is the same on every Stand,
     * so keeping them in fixed slots means muscle memory survives switching Stands.
     */
    private static final List<StandSkill> CLOSE_RANGE_GENERICS = List.of(
            BarrageSkill.INSTANCE, UppercutSkill.INSTANCE, GrabSkill.INSTANCE,
            StandLeapSkill.INSTANCE, StandDashSkill.INSTANCE, TimeSkipSkill.INSTANCE);
    /** Long-range Stands trade the leap - they have no need to close distance - for piloting. */
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
        register(org.gumel.jojoha.stand.skill.moves.HermitGrappleSkill.INSTANCE);
        register(org.gumel.jojoha.stand.skill.moves.ThornZipSkill.INSTANCE);
        register(org.gumel.jojoha.stand.skill.moves.LassoOfThornsSkill.INSTANCE);
        register(org.gumel.jojoha.stand.skill.moves.CameraCrushSkill.INSTANCE);
        register(org.gumel.jojoha.stand.skill.moves.ThornWhipSkill.INSTANCE);
        register(org.gumel.jojoha.stand.skill.moves.TwistingGutPunchSkill.INSTANCE);
        register(InhaleSkill.INSTANCE);
        register(StarFingerSkill.INSTANCE);
        register(TimeStopSkill.INSTANCE);
        register(TimeSkipSkill.INSTANCE);
        register(TimeStopExtendedSkill.INSTANCE);
        register(SkullCrusherSkill.INSTANCE);

        // The Utility stance's tools. Never registered before, because nothing ever looked them up
        // by id - the stance handed out the instances directly. Everything does now: the tree names
        // them, the bar stores them by id, and byId() has to be able to find them again. Without
        // this, Return showed as a raw translation key on the tree and resolved to nothing at all
        // once it was put in a slot.
        register(BuildModeSkill.SINGLE);
        register(BuildModeSkill.ROW);
        register(BuildModeSkill.COLUMN);
        register(BuildModeSkill.FREE);
        register(RecallStandSkill.INSTANCE);
    }

    private StandSkills() {
    }

    /** Starts the server-side tickers the moves rely on. Called from the mod's own init. */
    public static void init() {
        MoveTickers.init();
    }

    private static void register(StandSkill skill) {
        SKILLS.put(skill.id(), skill);
    }

    /** Every registered skill, in registration order. For anything listing what exists. */
    public static java.util.Collection<StandSkill> all() {
        return SKILLS.values();
    }

    public static StandSkill byId(ResourceLocation id) {
        return SKILLS.get(id);
    }

    /**
     * Testing hook: forces every Stand to be treated as this range class.
     *
     * <p>Exists because the long-range generic - piloting - has nothing to attach to until a
     * long-range Stand is authored, and an unreachable move is an untested one. Deliberately a
     * plain global rather than per-player state: it is reached only from a permission-gated debug
     * command, and giving it a real home in player data would imply it is a feature.
     */
    private static StandRange rangeOverride;

    public static void setRangeOverride(StandRange range) {
        rangeOverride = range;
    }

    /** The generic moves for a range class, then the Stand's own, capped at the slot count. */
    /**
     * The tools a Utility Stand carries instead of its moves.
     *
     * <p>Fixed, and the same for every Stand. What shape a wall is does not depend on whose Stand is
     * stacking it, and a player who learns the bar on one Stand should not have to relearn it on the
     * next.
     */
    /** Public so the loadout can seed the Utility bar with them the first time it is edited. */
    public static final List<StandSkill> UTILITY_TOOLS = List.of(
            BuildModeSkill.SINGLE, BuildModeSkill.ROW, BuildModeSkill.COLUMN, BuildModeSkill.FREE,
            RecallStandSkill.INSTANCE);

    /** The moveset as it should appear right now, which in Utility is not a moveset at all. */
    public static List<StandSkill> movesetFor(JojohaPlayerData data) {
        return data.standMode.handlesItems()
                ? UTILITY_TOOLS
                : movesetFor(StandTypes.byIdOrDefault(data.stand.standId()));
    }

    /**
     * Every move this Stand can have on a bar.
     *
     * <p>The generics are for Stands with a body. Barrage, uppercut, leap and dash all describe a
     * figure throwing itself about, and handing them to something that is a pair of vines growing
     * out of your arms produced a Hermit Purple that could uppercut - which is not a Hermit Purple.
     *
     * <p>It also read as the previous Stand's moves following you across a swap, because those
     * five are the ones anybody testing has already unlocked: switch from Star Platinum and its
     * whole opening row is sitting on the new Stand's page. They were never Star Platinum's, but
     * they were only ever meant for something shaped like it.
     *
     * <p>A bound Stand therefore starts from nothing and is exactly its own signature moves. Its
     * tree is the whole of what it can do, which is the point of giving it one.
     */
    public static List<StandSkill> movesetFor(StandType type) {
        StandRange range = rangeOverride != null ? rangeOverride : type.range();
        List<StandSkill> moveset = new ArrayList<>(type.form().isFreeStanding()
                ? (range == StandRange.LONG ? LONG_RANGE_GENERICS : CLOSE_RANGE_GENERICS)
                : List.<StandSkill>of());

        for (ResourceLocation id : type.signatureSkills()) {
            StandSkill skill = byId(id);
            if (skill != null && !moveset.contains(skill)) {
                moveset.add(skill);
            }
        }

        return moveset.size() > SLOT_COUNT ? moveset.subList(0, SLOT_COUNT) : moveset;
    }

    /** The move in a given slot for this player's Stand, or null if that slot is empty. */
    public static StandSkill skillInSlot(JojohaPlayerData data, int slot) {
        if (!data.stand.isPresent() || slot < 0 || slot >= SLOT_COUNT) {
            return null;
        }

        // Utility still swaps the whole bar rather than reading the ordinary one - the stance is
        // a different bar, not a different page of the same one, and a player gets their own moves
        // back the moment they leave it.
        //
        // What it swaps to is now arrangeable. Until somebody arranges it, it is the stock tools it
        // always was; the moment a single slot is set, the stored bar is the whole answer - which is
        // what makes it possible to take a tool off it and not have it come straight back.
        if (data.standMode.handlesItems()) {
            if (data.utilityLoadoutUntouched()) {
                return slot < UTILITY_TOOLS.size() ? UTILITY_TOOLS.get(slot) : null;
            }
            ResourceLocation held = data.utilityEquippedSkill(slot);
            return held == null ? null : byId(held);
        }

        // Everything else is what the player put there. The bar used to fill itself from the
        // Stand's moveset, which meant there was nothing to choose and no reason for a skill page
        // to exist.
        ResourceLocation equipped = data.equippedSkill(slot);
        return equipped == null ? null : byId(equipped);
    }

    /**
     * Whether two moves are alternatives to one another.
     *
     * <p>Asked in both directions, so only the newer of the pair has to declare the relationship.
     */
    public static boolean conflict(StandSkill a, StandSkill b) {
        if (a == null || b == null || a == b) {
            return false;
        }
        return a.id().equals(b.replaces()) || b.id().equals(a.replaces());
    }

    /**
     * Whether this player is allowed to put this move on their bar.
     *
     * <p>Asked on the server, of a request that arrived from a client, so it checks the two things a
     * client could lie about: that the move exists at all, and that they have it.
     */
    public static boolean canEquip(JojohaPlayerData data, StandSkill skill) {
        if (skill == null || !skill.isUnlocked(data)) {
            return false;
        }

        if (!skill.requiresStand()) {
            return true;
        }

        if (!data.stand.isPresent()) {
            return false;
        }

        // And it has to be a move this Stand has. The skill page lists every move in the game, so
        // without this a close-range Stand could be handed Pilot - which belongs to the long-range
        // moveset and has no meaning for something that fights at arm's length.
        //
        // The Utility stance's tools are not in any moveset - they are not moves a Stand knows, they
        // are what the stance turns the bar into - so they are allowed through on their own account.
        // Without this the Utility bar could not be given the very tools it exists to arrange.
        return UTILITY_TOOLS.contains(skill)
                || movesetFor(StandTypes.byIdOrDefault(data.stand.standId())).contains(skill);
    }

    /**
     * Server-side handling of a slot press.
     *
     * <p>Everything is re-derived here rather than taken from the client: the packet carries only
     * which slot was pressed, so a tampered client can at worst press a slot it does not have,
     * which resolves to nothing. Trust, energy and cooldown are all checked again on this side.
     */
    public static void handleUseRequest(ServerPlayer player, int slot, int chargeTicks) {
        // Checked on this side rather than trusted to the hidden bar, because the bar being hidden
        // is a client's opinion and a move arriving anyway is a packet. A spectator cannot touch the
        // world by any other means and should not get one through a Stand.
        if (player.isSpectator()) {
            return;
        }

        JojohaPlayerData data = PlayerDataAccess.get(player);
        StandSkill skill = skillInSlot(data, slot);
        if (skill == null) {
            return;
        }

        // Never trusted as sent: the client decides when to let go, the server decides what that is
        // worth.
        int charge = Math.max(0, Math.min(chargeTicks, skill.chargeMaxTicks()));

        // Picking a shape is not a cast. Handled ahead of every gate below - trust, energy, cooldown,
        // the manifested Stand itself - because none of them are about a setting, and a player who
        // ran their pool dry laying a floor should still be able to change what the next click does.
        if (skill instanceof BuildModeSkill tool) {
            data.buildMode = tool.mode();
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            notify(player, tool.translationKey());
            return;
        }

        // Likewise free, and for the same reason - see RecallStandSkill. Calling your own Stand
        // back is not something it does to the world.
        if (skill == RecallStandSkill.INSTANCE) {
            org.gumel.jojoha.stand.StandUtilityWork.release(player,
                    StandSummonHandler.findStand(player, data));
            notify(player, skill.translationKey());
            return;
        }

        // Moves are combat, and Utility is not. The Stand is off placing blocks; a barrage thrown
        // from a stance whose Stand is thirty blocks away laying a floor would either teleport it
        // back or swing at nothing.
        if (data.standMode.handlesItems()) {
            notify(player, "message.jojoha.skill.utility_stance");
            return;
        }

        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand == null && skill.requiresStand()) {
            notify(player, "message.jojoha.skill.no_stand");
            return;
        }

        if (!skill.isUnlocked(data)) {
            notify(player, "message.jojoha.skill.not_learned");
            return;
        }

        // Wrapped up. Three of the eight slots are unusable until it wears off, and which three is
        // carried in the effect's amplifier - see TangledPassive.
        net.minecraft.world.effect.MobEffectInstance tangled =
                player.getEffect(org.gumel.jojoha.registry.ModEffects.tangled());
        if (tangled != null && org.gumel.jojoha.stand.passive.TangledPassive.blocks(
                tangled.getAmplifier(), slot)) {
            notify(player, "message.jojoha.skill.tangled");
            return;
        }

        TrustTier trust = data.stand.trust();
        if (trust.level() < skill.minimumTrust().level()) {
            notify(player, "message.jojoha.skill.trust_too_low");
            return;
        }

        // Letting go of a sustained move is not a cast, and must not be gated like one.
        //
        // A sustained move sends the same packet to start and to stop, so the stop arrives at these
        // gates while the move it is stopping is still running - and a cooldown set by the start
        // blocks it. The result is a move that can be begun and then never ended, which is why the
        // one sustained skill that already existed carries a cooldown of zero. That was a workaround
        // for this, not a design decision, and it meant no sustained move could ever have a cooldown.
        //
        // Handled ahead of cooldown, energy and the charge, because none of them is about stopping
        // something: the cost was paid when it started.
        if (skill.isSustained() && skill.isSustainActive(player)) {
            skill.activate(player, data, stand, charge);
            PlayerDataAccess.set(player, data);
            PlayerDataAccess.sync(player);
            return;
        }

        long now = player.level().getGameTime();
        if (data.isMoveOnCooldown(skill.id(), now)) {
            return;
        }

        // Cost is scaled by the Stand's Endurance, which the design doc defines as how long you can
        // keep the Stand active - so a tougher Stand naturally gets more moves out of one summon.
        float cost = skill.energyCost(charge) * enduranceCostFactor(data);
        if (data.standEnergy < cost) {
            notify(player, "message.jojoha.skill.no_energy");
            return;
        }

        // Charged only on success, so a move that found nothing to hit is free to retry - but it
        // says so. A move that silently does nothing is indistinguishable from a broken keybind,
        // which is exactly how a targeting bug went unnoticed here before.
        if (!skill.activate(player, data, stand, charge)) {
            notify(player, "message.jojoha.skill.no_target");
            return;
        }

        data.standEnergy -= cost;

        // A lockout wins over the ordinary cooldown, and is used as given - see StandSkill.
        int lockout = skill.lockoutTicks(data);
        data.setMoveCooldown(skill.id(), now,
                lockout > 0 ? lockout : scaledCooldown(skill, data, charge));

        if (lockout > 0) {
            notify(player, "message.jojoha.skill.spent");
        }

        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
    }

    /**
     * Cooldown after the Stand's Speed stat, which the doc defines as scaling "the speed of your
     * stands moves and summoning, as well as cooldowns".
     *
     * <p>Capped at a 40% reduction so a maxed stat sharpens the rhythm of a fight without
     * collapsing it into an unbroken stream of moves.
     */
    public static int scaledCooldown(StandSkill skill, JojohaPlayerData data) {
        return scaledCooldown(skill, data, 0);
    }

    public static int scaledCooldown(StandSkill skill, JojohaPlayerData data, int chargeTicks) {
        return Math.max(1, Math.round(skill.cooldownTicks(chargeTicks)
                * org.gumel.jojoha.data.StatEffects.standCooldownScale(data.stand.speed())));
    }

    private static float enduranceCostFactor(JojohaPlayerData data) {
        return org.gumel.jojoha.data.StatEffects.standCostScale(data.stand.endurance());
    }

    private static void notify(ServerPlayer player, String translationKey) {
        player.displayClientMessage(Component.translatable(translationKey), true);
    }
}
