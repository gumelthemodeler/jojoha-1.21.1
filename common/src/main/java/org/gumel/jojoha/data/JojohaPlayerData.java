package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.skill.Moves;
import org.gumel.jojoha.stand.BondRank;
import org.gumel.jojoha.stand.BuildMode;
import org.gumel.jojoha.stand.StandMode;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.skill.StandReinforced;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.stand.skill.TimeStopCast;
import org.gumel.jojoha.stand.skill.moves.TimeSkipSkill;
import org.gumel.jojoha.stand.workshop.Unlocks;
import org.gumel.jojoha.ultimate.AdaptationLibrary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public final class JojohaPlayerData {
    public int strength;
    public int vitality;
    public int agility;
    public int endurance;
    public int worthiness;
    public int availableStatPoints;

    public int availableStandPoints;

    public int pointsFor(boolean stand) {
        return stand ? availableStandPoints : availableStatPoints;
    }

    public Discipline discipline;
    public VampireStage vampireStage;

    private final Set<ResourceLocation> unlockedHamonPaths;

    private final Set<ResourceLocation> equippedPassives;

    private final Set<ResourceLocation> unlockedNodes;

    private final Map<ResourceLocation, Set<Integer>> committedSkins = new HashMap<>();
    public ResourceLocation trait;
    public StandData stand;

    public float specEnergy;
    public float standEnergy;
    public record Energy(float spec, float stand) {
        public static final Energy DEFAULT = new Energy(MAX_SPEC_ENERGY, MAX_STAND_ENERGY);

        public static final Codec<Energy> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.FLOAT.optionalFieldOf("spec", MAX_SPEC_ENERGY).forGetter(Energy::spec),
                        Codec.FLOAT.optionalFieldOf("stand", MAX_STAND_ENERGY).forGetter(Energy::stand)
                ).apply(instance, Energy::new));
    }

    public static final float MAX_SPEC_ENERGY = 100F;
    public static final float MAX_STAND_ENERGY = 100F;

    public float maxStandEnergy() {
        if (!stand.isPresent()) {
            return MAX_STAND_ENERGY;
        }

        float base = stand.trust().maxStandEnergy() + StatEffects.energyBonus(stand.potential())
                + BondRank.of(stand).energyBonus();
        return base * Heaven.staminaScale(this);
    }

    public final Map<ResourceLocation, Long> moveCooldowns = new HashMap<>();

    public int killProgress;

    public int[] playerStatProgress = new int[StatPoints.COUNT];
    public int[] standStatProgress = new int[StatPoints.COUNT];

    public Set<String> deeds = new HashSet<>();

    public final ResourceLocation[] equippedSkills =
            new ResourceLocation[StandSkills.SLOT_COUNT];

    public final ResourceLocation[] utilitySkills =
            new ResourceLocation[StandSkills.SLOTS_PER_PAGE];

    public ResourceLocation equippedSkill(int slot) {
        return barSkill(inUtilityStance(), slot);
    }

    public ResourceLocation barSkill(boolean utility, int slot) {
        if (slot < 0) {
            return null;
        }
        if (utility) {
            return utilitySkills[slot % utilitySkills.length];
        }
        return slot < equippedSkills.length ? equippedSkills[slot] : null;
    }

    public int slotOf(boolean utility, ResourceLocation skillId) {
        ResourceLocation[] bar = utility ? utilitySkills : equippedSkills;
        for (int slot = 0; slot < bar.length; slot++) {
            if (Objects.equals(skillId, bar[slot])) {
                return slot;
            }
        }
        return -1;
    }

    public boolean halfSummon;

    public boolean standOut() {
        return standSummoned && !halfSummon;
    }

    public boolean inUtilityStance() {
        return standOut() && standMode.handlesItems();
    }

    public void placeOnBar(ResourceLocation skillId, int preferred) {
        skillId = StandReinforced.baseOf(skillId);
        if (skillId == null) {
            return;
        }
        if (StandSkills.toolOnly(
                StandSkills.byId(skillId))) {
            placeOnUtility(skillId, preferred);
            return;
        }
        if (slotOf(skillId) >= 0) {
            return;
        }
        if (preferred >= 0 && preferred < equippedSkills.length && equippedSkills[preferred] == null) {
            equippedSkills[preferred] = skillId;
            return;
        }
        for (int slot = 0; slot < equippedSkills.length; slot++) {
            if (equippedSkills[slot] == null) {
                equippedSkills[slot] = skillId;
                return;
            }
        }
    }

    private void placeOnUtility(ResourceLocation skillId, int preferred) {
        if (skillId == null || slotOf(true, skillId) >= 0) {
            return;
        }
        if (preferred >= 0 && preferred < utilitySkills.length && utilitySkills[preferred] == null) {
            utilitySkills[preferred] = skillId;
            return;
        }
        for (int slot = 0; slot < utilitySkills.length; slot++) {
            if (utilitySkills[slot] == null) {
                utilitySkills[slot] = skillId;
                return;
            }
        }
    }

    private void splitTools() {
        for (int slot = 0; slot < equippedSkills.length; slot++) {
            ResourceLocation id = equippedSkills[slot];
            if (id != null && StandSkills.toolOnly(
                    StandSkills.byId(id))) {
                equippedSkills[slot] = null;
                placeOnUtility(id, slot);
            }
        }
    }

    public boolean dropUnequippable() {
        boolean dropped = false;
        for (ResourceLocation[] bar : List.of(equippedSkills, utilitySkills)) {
            for (int slot = 0; slot < bar.length; slot++) {
                if (bar[slot] != null && !Moves.canEquip(this, bar[slot])) {
                    bar[slot] = null;
                    dropped = true;
                }
            }
        }
        return dropped;
    }

    public TrustTier manifestTier() {
        TrustTier trust = stand.trust();
        if (!halfSummon) {
            return trust;
        }
        return trust.ordinal() > TrustTier.PARTIAL.ordinal()
                ? TrustTier.PARTIAL
                : trust;
    }

    public int slotOf(ResourceLocation skillId) {
        for (int slot = 0; slot < equippedSkills.length; slot++) {
            if (skillId.equals(equippedSkills[slot])) {
                return slot;
            }
        }
        return -1;
    }

    public boolean standSummoned;
    public UUID summonedStandEntityUuid;

    public int combatTicks;
    public int timeStopUsesThisFight;
    public boolean standGuarding;
    public int blockedHitsSinceBreak;
    public int guardRecoverTicks;
    public int guardBreakCooldownTicks;
    public boolean lastDamageWasStandAttack;
    public double recentMoveX;
    public double recentMoveZ;
    public int recentMoveTicks;
    public double lastTickX;
    public double lastTickZ;

    public int standLeapGrabTicks;
    public int inhaleIFrameTicks;

    public boolean nextPunchIsPunch2;
    public long punchCooldownExpiry;
    public boolean stoneMaskWorn;
    public DreamMemory dreamMemory = DreamMemory.NONE;
    public TotemBinding totem = TotemBinding.NONE;
    public boolean anubis;

    public int anubisUse;

    public int anubisPossessed;
    public ResourceLocation alignedStand;
    public float alignOdds;
    public float hamonSpent;
    public String fortune = "";
    public int phobia;
    public float honor = 50F;

    public int curses;

    public int ghostTicks;

    public long lastConflict;

    public int limbs;

    public int limbRegrow;

    public int fortuneTally;

    public long fortuneUntil;
    public float ascension;
    public int heavenFightTicks;
    public int greatestHighTicks;
    public StandPlacement standPlacement = StandPlacement.NONE;
    public boolean timeSkipDash;
    public int summonStyle;
    public int barrageShout;
    public int restPose;
    public final WorkshopLook[] looks = emptyLooks();
    public final Set<String> workshopUnlocks = new LinkedHashSet<>();
    public String standIdle = "";
    public final StandPose[] poses = emptyPoses();

    public final Map<String, Integer> adaptMastery = new LinkedHashMap<>();
    public final Map<String, String> adaptSources = new LinkedHashMap<>();
    public final List<String> adaptActives = new ArrayList<>();
    public final List<String> adaptPassives = new ArrayList<>();

    public boolean ultimateWings;
    public boolean headless;
    public boolean progenitor;
    public int reform;
    public boolean bornRolled;
    public int stoneMaskRitualTicks;

    public int standArrowRitualTicks;
    public boolean standArrowRitualShard;

    public boolean standArrowRitualSkin;

    public int standSkinSwapTicks;
    public int standArrowLandWatchTicks;
    public int timeStopExposures;
    public int timeStopCastTicks;

    public int timeStopCasts;
    public int timeStopHeldTicks;
    public int timeStopFrozenTicks;
    public boolean standPiloting;
    public StandMode standMode = StandMode.ANALOG;

    public BuildMode buildMode = BuildMode.SINGLE;

    public JojohaPlayerData(int strength, int vitality, int agility, int endurance, int worthiness,
                             int availableStatPoints, Discipline discipline, VampireStage vampireStage,
                             Set<ResourceLocation> unlockedHamonPaths,
                             Optional<ResourceLocation> trait, StandData stand,
                             Energy energy, StandSession standSession,
                             boolean stoneMaskWorn, Map<ResourceLocation, Long> moveCooldowns,
                             Set<ResourceLocation> unlockedNodes) {
        this.strength = strength;
        this.vitality = vitality;
        this.agility = agility;
        this.endurance = endurance;
        this.worthiness = worthiness;
        this.availableStatPoints = availableStatPoints;
        this.discipline = discipline;
        this.vampireStage = vampireStage;
        this.unlockedHamonPaths = new HashSet<>(unlockedHamonPaths);
        this.equippedPassives = new LinkedHashSet<>(stand.equippedPassives());
        this.unlockedNodes = new HashSet<>(unlockedNodes);
        this.trait = trait.orElse(null);
        this.stand = stand;
        this.specEnergy = energy.spec();
        this.standEnergy = energy.stand();
        this.standSummoned = standSession.summoned();
        this.standMode = StandMode.fromOrdinal(standSession.modeOrdinal());
        this.buildMode = BuildMode.fromOrdinal(standSession.buildModeOrdinal());
        this.standPiloting = standSession.piloting();
        this.timeStopExposures = standSession.timeStopExposures();
        this.timeStopFrozenTicks = standSession.timeStopFrozenTicks();
        this.availableStandPoints = standSession.standPoints();
        this.timeStopHeldTicks = standSession.timeStopHeldTicks();
        this.timeStopCastTicks = standSession.timeStopCastTicks();
        this.timeStopCasts = standSession.timeStopCasts();
        this.timeStopUsesThisFight = standSession.timeStopUsesThisFight();
        this.killProgress = standSession.killProgress();
        StatProgress progress = standSession.statProgress();
        this.playerStatProgress = StatProgress.toArray(progress.player());
        this.standStatProgress = StatProgress.toArray(progress.stand());
        this.deeds = new HashSet<>(progress.deeds());

        readLoadout(standSession.equipped(), equippedSkills);
        mergeLoadout(standSession.barState().playerEquipped());
        List<String> tools = standSession.utilityEquipped();
        for (int slot = 0; slot < tools.size(); slot++) {
            String id = tools.get(slot);
            if (id != null && !id.isEmpty()) {
                placeOnUtility(ResourceLocation.tryParse(id), slot);
            }
        }
        splitTools();
        this.halfSummon = standSession.barState().halfSummon();
        this.dreamMemory = standSession.barState().dream();
        this.totem = standSession.barState().totem();
        this.anubis = standSession.barState().anubis();
        this.anubisUse = standSession.barState().anubisHold().use();
        this.anubisPossessed = standSession.barState().anubisHold().possessed();
        SpiritData spirit = standSession.barState().spirit();
        this.alignedStand = spirit.aligned().orElse(null);
        this.alignOdds = spirit.alignOdds();
        this.hamonSpent = spirit.hamonSpent();
        FortuneData fortune = standSession.barState().fortune();
        this.fortune = fortune.fortune();
        this.phobia = fortune.phobia();
        this.honor = fortune.honor();
        this.curses = fortune.curses();
        this.ghostTicks = fortune.ghostTicks();
        this.lastConflict = fortune.lastConflict();
        this.limbs = fortune.limbs();
        this.limbRegrow = fortune.limbRegrow();
        this.fortuneTally = fortune.tally();
        this.fortuneUntil = fortune.until();
        HeavenData heaven = standSession.barState().heaven();
        this.ascension = heaven.ascension();
        this.heavenFightTicks = heaven.fightTicks();
        this.greatestHighTicks = heaven.greatestHighTicks();
        this.standPlacement = standSession.barState().placement();
        this.timeSkipDash = standSession.barState().timeSkipDash();
        WorkshopData workshop = standSession.barState().workshop();
        this.summonStyle = workshop.summon();
        this.barrageShout = workshop.shout();
        this.restPose = workshop.rest();
        for (int slot = 0; slot < this.looks.length && slot < workshop.looks().size(); slot++) {
            this.looks[slot] = workshop.looks().get(slot);
        }
        for (String key : workshop.log()) {
            if (Unlocks.kept(key)) {
                this.workshopUnlocks.add(key);
            }
        }
        this.standIdle = workshop.idle();
        for (int slot = 0; slot < this.poses.length && slot < workshop.poses().size(); slot++) {
            this.poses[slot] = workshop.poses().get(slot);
        }
        UltimateData ultimate = standSession.barState().ultimate();
        this.adaptMastery.putAll(ultimate.mastery());
        this.adaptSources.putAll(ultimate.sources());
        this.adaptActives.addAll(ultimate.actives());
        this.adaptPassives.addAll(ultimate.passives());
        ultimate.adaptation().ifPresent(AdaptationLibrary.migrate(this));
        this.ultimateWings = ultimate.wings();
        this.headless = ultimate.headless();
        this.progenitor = ultimate.progenitor();
        this.reform = ultimate.reform();
        this.bornRolled = ultimate.bornRolled();
        foldStandForms();
        standSession.barState().committedSkins().forEach(
                (standId, skins) -> committedSkins.put(standId, new TreeSet<>(skins)));
        this.stoneMaskWorn = stoneMaskWorn;
        this.moveCooldowns.putAll(moveCooldowns);
    }

    private static void readLoadout(List<String> saved, ResourceLocation[] into) {
        for (int slot = 0; slot < into.length && slot < saved.size(); slot++) {
            String id = saved.get(slot);
            into[slot] = id == null || id.isEmpty() ? null : ResourceLocation.tryParse(id);
        }
    }

    private void foldStandForms() {
        for (int slot = 0; slot < equippedSkills.length; slot++) {
            ResourceLocation id = equippedSkills[slot];
            ResourceLocation base = StandReinforced.baseOf(id);
            if (base == null || base.equals(id)) {
                continue;
            }
            if (TimeSkipSkill.ID.equals(id)) {
                timeSkipDash = true;
            }
            equippedSkills[slot] = null;
            if (slotOf(base) < 0) {
                equippedSkills[slot] = base;
            }
        }
    }

    private void mergeLoadout(List<String> saved) {
        for (int slot = 0; slot < saved.size(); slot++) {
            String id = saved.get(slot);
            if (id != null && !id.isEmpty()) {
                placeOnBar(ResourceLocation.tryParse(id), slot);
            }
        }
    }

    public Set<ResourceLocation> getEquippedPassives() {
        return equippedPassives;
    }

    public StandData standWithLoadout() {
        return stand.withPassives(Collections.unmodifiableSet(
                new LinkedHashSet<>(equippedPassives)));
    }

    public void setChosenPassives(List<ResourceLocation> families) {
        equippedPassives.clear();
        equippedPassives.addAll(families);
    }

    public List<String> equippedList() {
        return writeLoadout(equippedSkills);
    }

    public List<String> utilityList() {
        return writeLoadout(utilitySkills);
    }

    private static List<String> writeLoadout(ResourceLocation[] from) {
        List<String> out = new ArrayList<>(from.length);
        for (ResourceLocation id : from) {
            out.add(id == null ? "" : id.toString());
        }
        return out;
    }

    public static JojohaPlayerData createDefault() {
        return new JojohaPlayerData(5, 5, 5, 5, 0, 0, Discipline.NONE, VampireStage.NONE,
                Set.of(), Optional.empty(), StandData.NONE, Energy.DEFAULT,
                StandSession.DEFAULT, false, Map.of(), Set.of());
    }

    public Set<ResourceLocation> getUnlockedHamonPaths() {
        return unlockedHamonPaths;
    }

    public boolean hasHamonPath(ResourceLocation pathId) {
        return unlockedHamonPaths.contains(pathId);
    }

    public void clearHamonPaths() {
        unlockedHamonPaths.removeIf(id -> !HamonPaths.isMode(id));
    }

    public void grantHamonPath(ResourceLocation pathId) {
        unlockedHamonPaths.add(pathId);
    }

    private static StandPose[] emptyPoses() {
        StandPose[] poses = new StandPose[StandPose.SLOTS];
        Arrays.fill(poses, StandPose.EMPTY);
        return poses;
    }

    private static WorkshopLook[] emptyLooks() {
        WorkshopLook[] looks = new WorkshopLook[WorkshopData.LOOK_SLOTS];
        Arrays.fill(looks, WorkshopLook.EMPTY);
        return looks;
    }

    public boolean canWearSkin(ResourceLocation standId, int skin) {
        return skin == 0 || hasCommittedSkin(standId, skin) || Unlocks.all(this);
    }

    public boolean hasCommittedSkin(ResourceLocation standId, int skin) {
        Set<Integer> skins = standId == null ? null : committedSkins.get(standId);
        return skins != null && skins.contains(skin);
    }

    public boolean commitSkin(ResourceLocation standId, int skin) {
        return committedSkins.computeIfAbsent(standId, id -> new TreeSet<>()).add(skin);
    }

    private Map<ResourceLocation, List<Integer>> committedSkinsList() {
        Map<ResourceLocation, List<Integer>> out = new HashMap<>();
        committedSkins.forEach((standId, skins) -> out.put(standId, List.copyOf(skins)));
        return out;
    }

    public Set<ResourceLocation> getUnlockedNodes() {
        return unlockedNodes;
    }

    public boolean hasNode(ResourceLocation nodeId) {
        return nodeId != null && unlockedNodes.contains(nodeId);
    }

    public void grantNode(ResourceLocation nodeId) {
        unlockedNodes.add(nodeId);
    }

    public void clearNodes() {
        unlockedNodes.clear();
    }

    public boolean isTimeStopFrozen() {
        return timeStopFrozenTicks > 0;
    }

    public boolean isInStoppedTime() {
        return timeStopFrozenTicks > 0 || timeStopHeldTicks > 0;
    }

    public boolean isCastingTimeStop() {
        return timeStopCastTicks > 0;
    }

    public float timeStopCastProgress() {
        return timeStopCastTicks <= 0
                ? 0F
                : Math.max(0F, 1F - (timeStopCastTicks / (float) TimeStopCast.CAST_TICKS));
    }

    public boolean isMoveOnCooldown(ResourceLocation moveId, long currentGameTime) {
        Long expiry = moveCooldowns.get(moveId);
        return expiry != null && expiry > currentGameTime;
    }

    public long moveCooldownLeft(ResourceLocation moveId, long currentGameTime) {
        Long expiry = moveCooldowns.get(moveId);
        return expiry == null ? 0L : Math.max(0L, expiry - currentGameTime);
    }

    public void setMoveCooldown(ResourceLocation moveId, long currentGameTime, int cooldownTicks) {
        if (DebugCooldowns.none() && !DebugCooldowns.kept(moveId)) {
            moveCooldowns.remove(moveId);
            return;
        }
        moveCooldowns.put(moveId, currentGameTime + cooldownTicks);
    }

    public static final Codec<JojohaPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("strength").forGetter(d -> d.strength),
            Codec.INT.fieldOf("vitality").forGetter(d -> d.vitality),
            Codec.INT.fieldOf("agility").forGetter(d -> d.agility),
            Codec.INT.fieldOf("endurance").forGetter(d -> d.endurance),
            Codec.INT.fieldOf("worthiness").forGetter(d -> d.worthiness),
            Codec.INT.fieldOf("stat_points").forGetter(d -> d.availableStatPoints),
            Discipline.CODEC.fieldOf("spec").forGetter(d -> d.discipline),
            VampireStage.CODEC.fieldOf("vampire_stage").forGetter(d -> d.vampireStage),
            ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("hamon_paths").forGetter(d -> d.unlockedHamonPaths),
            ResourceLocation.CODEC.optionalFieldOf("trait").forGetter(d -> Optional.ofNullable(d.trait)),
            StandData.CODEC.fieldOf("stand").forGetter(JojohaPlayerData::standWithLoadout),
            Energy.CODEC.optionalFieldOf("energy", Energy.DEFAULT)
                    .forGetter(d -> new Energy(d.specEnergy, d.standEnergy)),
            StandSession.CODEC.optionalFieldOf("stand_session", StandSession.DEFAULT).forGetter(
                    d -> new StandSession(d.standSummoned, d.standMode.ordinal(), d.standPiloting,
                            d.timeStopExposures, d.timeStopFrozenTicks, d.timeStopHeldTicks,
                            d.timeStopCastTicks, d.timeStopCasts, d.buildMode.ordinal(),
                            d.timeStopUsesThisFight, d.killProgress, d.equippedList(),
                            d.utilityList(), d.availableStandPoints,
                            new StatProgress(StatProgress.fromArray(d.playerStatProgress),
                                    StatProgress.fromArray(d.standStatProgress),
                                    List.copyOf(d.deeds)),
                            new BarState(List.of(), d.halfSummon, d.committedSkinsList(),
                                    d.dreamMemory, d.totem, d.anubis,
                                    new SpiritData(Optional.ofNullable(d.alignedStand), d.alignOdds,
                                            d.hamonSpent),
                                    new FortuneData(d.fortune, d.phobia, d.honor, d.curses,
                                            d.ghostTicks, d.lastConflict, d.limbs, d.limbRegrow,
                                            d.fortuneTally, d.fortuneUntil),
                                    new HeavenData(d.ascension, d.heavenFightTicks, d.greatestHighTicks),
                                    d.standPlacement, d.timeSkipDash,
                                    new UltimateData(Optional.empty(), d.ultimateWings, d.headless,
                                            Map.copyOf(d.adaptMastery), List.copyOf(d.adaptActives),
                                            List.copyOf(d.adaptPassives), Map.copyOf(d.adaptSources),
                                            d.progenitor, d.reform, d.bornRolled),
                                    new AnubisHold(d.anubisUse, d.anubisPossessed),
                                    new WorkshopData(d.summonStyle, d.barrageShout, d.restPose, List.of(d.looks),
                                            List.copyOf(d.workshopUnlocks), d.standIdle,
                                            List.of(d.poses))))),
            Codec.BOOL.optionalFieldOf("stone_mask_worn", false).forGetter(d -> d.stoneMaskWorn),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG)
                    .optionalFieldOf("move_cooldowns", Map.of())
                    .forGetter(d -> d.moveCooldowns),
            ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf)
                    .optionalFieldOf("unlocked_nodes", Set.of())
                    .forGetter(d -> d.unlockedNodes)
    ).apply(instance, JojohaPlayerData::new));

    public void shareEndurance(boolean merge) {
        if (stand == null || !stand.isPresent()) {
            return;
        }
        if (merge && stand.endurance() > endurance) {
            endurance = Math.min(stand.endurance(), StatPoints.MAX_STAT);
        }
        if (stand.endurance() != endurance) {
            stand = stand.withEndurance(endurance);
        }
    }
}
