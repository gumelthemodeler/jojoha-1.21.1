package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.stand.StandMode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Per-player mutable state for everything the mod tracks: core stats, active spec,
 * unlocked Hamon paths, trait, and Stand progress. One instance per player, held behind
 * a platform attachment (see {@link PlayerDataAccess}) and mirrored to the client via
 * {@code SyncPlayerDataPacket}.
 */
public final class JojohaPlayerData {
    public int strength;
    public int vitality;
    public int agility;
    public int endurance;
    /** Per the design doc: cannot be spent from a menu, only earned through dungeons, bosses, etc. */
    public int worthiness;
    /** Spendable pool for strength/vitality/agility/endurance. */
    public int availableStatPoints;

    /**
     * Points that can only be spent on the Stand.
     *
     * <p>Separate from the player's own pool, and earned separately. Before a Stand exists it simply
     * has nothing to be spent on; raising yourself and raising your Stand are no longer the same
     * budget, so neither competes with the other.
     */
    public int availableStandPoints;

    /** The pool a given page spends from: the Stand's for the Stand, the player's for the player. */
    public int pointsFor(boolean stand) {
        return stand ? availableStandPoints : availableStatPoints;
    }

    public PlayerSpec spec;
    public VampireStage vampireStage;

    private final Set<ResourceLocation> unlockedHamonPaths;

    /**
     * Every skill-tree node this player has taken.
     *
     * <p>Nodes rather than moves, because a node can grant something other than a move - the one at
     * the centre of each tree grants nothing at all and only opens the path. What a node hands over
     * is the tree's business; this is only the record of having taken it.
     */
    private final Set<ResourceLocation> unlockedNodes;
    public ResourceLocation trait;
    public StandData stand;

    /** Combat bar resources - spec energy fuels spec moves (e.g. Ripple Pulse), stand energy tracks how long the Stand can stay active. Both regenerate passively; see EnergySystem. */
    public float specEnergy;
    public float standEnergy;
    /**
     * The two energy pools, bundled into one codec field.
     *
     * <p>Not a tidying-up: {@code RecordCodecBuilder.group} holds sixteen fields and the group was
     * full, so something had to be folded up before the skill tree could be saved at all. These two
     * were the obvious pair - both floats, both about the same thing, and both already optional.
     *
     * <p>A save written before this bundling has {@code spec_energy} and {@code stand_energy} at the
     * top level, which this no longer reads, so both come back at their default of full. That is
     * the cheapest possible thing to lose in a migration: the pools regenerate on their own and the
     * default is the maximum, so the player is not out of pocket by so much as one point.
     */
    public record Energy(float spec, float stand) {
        public static final Energy DEFAULT = new Energy(MAX_SPEC_ENERGY, MAX_STAND_ENERGY);

        public static final com.mojang.serialization.Codec<Energy> CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.create(instance -> instance.group(
                        Codec.FLOAT.optionalFieldOf("spec", MAX_SPEC_ENERGY).forGetter(Energy::spec),
                        Codec.FLOAT.optionalFieldOf("stand", MAX_STAND_ENERGY).forGetter(Energy::stand)
                ).apply(instance, Energy::new));
    }

    public static final float MAX_SPEC_ENERGY = 100F;
    public static final float MAX_STAND_ENERGY = 100F;

    /**
     * The energy ceiling this player currently has, which rises with Stand trust.
     *
     * <p>{@link #MAX_STAND_ENERGY} is only the starting value now - anything that fills, clamps or
     * draws a proportion of the bar has to ask for this instead, or a Bonded Stand would show a
     * full bar at 100 out of 250.
     */
    public float maxStandEnergy() {
        if (!stand.isPresent()) {
            return MAX_STAND_ENERGY;
        }

        // Trust decides the ceiling and POTENTIAL raises it. The two are different things: trust is
        // what the Stand allows its user, potential is what the Stand has in it to give.
        return stand.trust().maxStandEnergy() + StatEffects.energyBonus(stand.potential());
    }

    /**
     * Move id -> the game time its cooldown expires at.
     *
     * <p>Saved, and as a direct consequence also synced: the sync packet is built from this class's
     * codec, so anything the codec carries reaches the client and anything it does not is invisible
     * there. This used to be session-only, which cost two separate things. A player who logged out
     * and back in returned with every cooldown cleared - including the time stop lockout, which is
     * written here like any other - so the whole of a move's cost could be skipped by rejoining. And
     * the interface could never draw a cooldown at all, because the client's copy of this map was
     * always empty.
     *
     * <p>The values are absolute game times rather than remaining ticks, which is right for a sync
     * (both sides share a clock) and only nearly right for a save - the clock is the world's, so an
     * expiry carried into a different world is meaningless. That is pruned on join rather than
     * solved here; see Jojoha's join handler.
     */
    public final Map<ResourceLocation, Long> moveCooldowns = new HashMap<>();

    /**
     * How much killing is banked toward the next stat point - see {@link StatProgression}.
     *
     * <p>Saved and synced, the second because the interface wants to draw how close the next one is
     * and the client cannot know that otherwise.
     */
    public int killProgress;

    /**
     * What sits in each bar slot, by id, or null for an empty one.
     *
     * <p>Saved and synced. The bar used to fill itself from the Stand's moveset, which meant every
     * Stand of a kind played identically and there was nothing to decide - see
     * {@code StandSkills.skillInSlot}.
     */
    public final ResourceLocation[] equippedSkills =
            new ResourceLocation[org.gumel.jojoha.stand.skill.StandSkills.SLOT_COUNT];

    /** What is in a slot, or null. Bounds-checked, because slots arrive from packets. */
    /**
     * The bar the Utility stance shows, which is a different bar and not a different page.
     *
     * <p>Utility used to hand out a fixed set of tools that could not be arranged. This is where an
     * arrangement lives once one is made; while it is entirely empty the stance still falls back to
     * the stock tools, so a player who never opens it sees exactly what they always did.
     */
    public final ResourceLocation[] utilityEquippedSkills =
            new ResourceLocation[org.gumel.jojoha.stand.skill.StandSkills.SLOT_COUNT];

    public ResourceLocation equippedSkill(int slot) {
        return slot >= 0 && slot < equippedSkills.length ? equippedSkills[slot] : null;
    }

    public ResourceLocation utilityEquippedSkill(int slot) {
        return slot >= 0 && slot < utilityEquippedSkills.length
                ? utilityEquippedSkills[slot] : null;
    }

    /** Either bar, chosen by a flag, so callers do not have to branch around two arrays. */
    public ResourceLocation[] loadout(boolean utility) {
        return utility ? utilityEquippedSkills : equippedSkills;
    }

    /** True while the Utility bar has never been arranged, and the stock tools still apply. */
    public boolean utilityLoadoutUntouched() {
        for (ResourceLocation id : utilityEquippedSkills) {
            if (id != null) {
                return false;
            }
        }
        return true;
    }

    /** Which slot holds a move, or -1. Used to keep one move from occupying two slots. */
    /**
     * Where a move sits on one of the two bars, or -1.
     *
     * <p>Takes the bar, because "which slot is this on" has no answer until you say which bar you
     * are asking about - the same move can sit in different places on each.
     */
    public int slotOf(ResourceLocation skillId, boolean utility) {
        ResourceLocation[] bar = loadout(utility);
        for (int slot = 0; slot < bar.length; slot++) {
            if (skillId.equals(bar[slot])) {
                return slot;
            }
        }
        return -1;
    }

    public int slotOf(ResourceLocation skillId) {
        for (int slot = 0; slot < equippedSkills.length; slot++) {
            if (skillId.equals(equippedSkills[slot])) {
                return slot;
            }
        }
        return -1;
    }

    /** Whether the player currently has their Stand summoned - synced, drives the combat bar's Stand-state icon. */
    public boolean standSummoned;
    /** UUID of the summoned StandEntity, for server-side lookup/removal. Session-only, not persisted or synced. */
    public java.util.UUID summonedStandEntityUuid;

    // --- Combat state below: all session-only (not persisted, not synced) - see StandCombatHandler/EnergySystem. ---

    /** Ticks remaining in "active combat" - while > 0, Stand energy drains at the faster combat rate instead of the passive one. Refreshed whenever the player deals or receives damage. */
    public int combatTicks;
    /**
     * Time stops thrown in the fight currently under way.
     *
     * <p>Session-only, and counted against {@link #combatTicks} rather than against a clock of its
     * own - "a fight" is already a thing this data knows about, and inventing a second definition
     * of it would give the player two overlapping notions of when they are in one. Cleared when the
     * fight ends, or when the lockout the third stop earns has been served. See TimeStopSkill.
     */
    public int timeStopUsesThisFight;
    /** Whether the player is currently holding the guard key with a Stand summoned. */
    public boolean standGuarding;
    /** Hits absorbed by Stand blocking since the last guard break - resets to 0 on break. */
    public int blockedHitsSinceBreak;
    /** Ticks remaining until blocking is available again after a guard break; 0 = not broken. */
    public int guardBreakCooldownTicks;
    /** Set right before a Stand punch deals damage, checked (and cleared) by the LIVING_HURT handler so it can tell a Stand hit apart from a "non-stand form" hit for energy restoration. */
    public boolean lastDamageWasStandAttack;
    /**
     * The direction the player was last travelling, and how recently.
     *
     * <p>Session-only, and sampled once per player tick rather than read on demand. A move handler
     * runs from a packet, and by then the level has already copied the player's position into its
     * "old position" field for the frame - so measuring travel at that point always reads zero. The
     * only place the two positions genuinely differ is across a tick, so that is where it is taken.
     *
     * <p>The freshness counter is what lets a dash thrown a moment after letting go of a key still
     * follow it: raw per-tick movement flickers to zero constantly, and keying off it directly
     * would make the direction depend on which tick the press happened to land in.
     */
    public double recentMoveX;
    public double recentMoveZ;
    public int recentMoveTicks;
    /** Where the player was at the end of the previous tick - the baseline for the above. */
    public double lastTickX;
    public double lastTickZ;

    /**
     * Ticks left in which an M1 becomes a grab-and-pull instead of a punch, opened by Stand Leap.
     *
     * <p>Session-only. It is a follow-up window rather than a mode: the point is that leaping and
     * then striking is one continuous action, so it has to expire on its own if the player does
     * something else.
     */
    public int standLeapGrabTicks;
    /**
     * Ticks left in which the breath's user cannot be touched by anything with a body behind it.
     *
     * <p>Session-only and server-side: the hurt handler is the only thing that reads it. The move
     * hauls a crowd onto the user by design, so without a moment's cover the reward for landing it
     * is being surrounded - the window is what makes the follow-up worth reaching for rather than
     * a gamble. Restricted to direct hits at the point it is checked, so it is a moment of
     * untouchability in the vortex and not a way to walk through lava.
     */
    public int inhaleIFrameTicks;

    /** Whether the next punch in the M1 chain should be "punch2" rather than "punch" - alternates each swing. */
    public boolean nextPunchIsPunch2;
    /** Game time (ticks) the punch cooldown expires at - gates how fast the M1 chain can advance. */
    public long punchCooldownExpiry;
    /**
     * Whether a Stone Mask is sitting on this player's face.
     *
     * <p>Persisted, unlike the ritual counter beside it. The mask is put on once and stays on, so
     * it has to survive a relog - and it is the local player's own authority for whether to draw
     * it, which is why it is synced rather than remembered client-side like everyone else's is.
     */
    public boolean stoneMaskWorn;
    /** Ticks left in the mask's equip-and-turn sequence; 0 = not running. See StoneMaskRitual. */
    public int stoneMaskRitualTicks;

    /** Ticks left in the Stand Arrow's stab before the Stand awakens; 0 = no ritual running. See StandArrowRitual. */
    public int standArrowRitualTicks;
    /**
     * Whether the running ritual was started by a shard rather than a whole arrow.
     *
     * <p>Session-only, and recorded at the stab rather than worked out at the end. The ritual runs
     * for several seconds and the odds belong to the thing that was actually used - a player who
     * stabs themselves with a shard and then puts a whole arrow in their hand should not be
     * rewarded for the sleight of hand, and one who used their last shard should not be punished
     * for having none left to find.
     */
    public boolean standArrowRitualShard;

    /**
     * Whether the ritual currently running is a skin arrow rather than a Stand arrow.
     *
     * <p>The stab, the spiral and the timing are the same for both - what differs is only what
     * happens when the arrow finally lands. Sharing the timeline and branching at the payoff keeps
     * the two rituals from drifting apart in a way a player would notice as one of them feeling
     * subtly wrong.
     */
    public boolean standArrowRitualSkin;

    /**
     * Ticks left in the swap itself: the white burn, the burst, and the pause before the Stand
     * comes back wearing something else.
     *
     * <p>Counted on the player rather than on the Stand because the Stand does not survive the
     * middle of it. Something has to still be there between the explosion and the re-summon, and
     * the only thing that is, is the person it came out of.
     */
    public int standSkinSwapTicks;
    /**
     * Ticks left to keep watching for the player to touch down after the awakening's levitation
     * releases them; 0 = not watching. See StandArrowRitual#tickLandWatch.
     */
    public int standArrowLandWatchTicks;
    /**
     * How many separate time stops this player has been caught inside.
     *
     * <p>Star Platinum's own time stop is learned by surviving other people's, so this is progress
     * rather than statistics - persisted, and never reset.
     */
    public int timeStopExposures;
    /** Ticks left in the wind-up before a time stop lands. See TimeStopCast. */
    public int timeStopCastTicks;

    /** Stops thrown, ever. Practice - see TimeStopSkill.ceiling. */
    public int timeStopCasts;
    /**
     * Ticks left in a stop this player is the one holding. Synced so their client can draw it.
     */
    public int timeStopHeldTicks;
    /**
     * Ticks this player is frozen by someone else's stopped time. Synced, because the client is
     * what has to stop answering their keyboard - see KeyboardInputMixin.
     */
    public int timeStopFrozenTicks;
    /**
     * Whether the user is currently flying their Stand directly. Synced, because the client owns
     * the camera swap and the input suppression that go with it. See PilotSystem.
     */
    public boolean standPiloting;
    /** Stance the Stand is being held in. Synced, since the client positions the model from it. */
    public StandMode standMode = StandMode.ANALOG;

    /**
     * What shape the Stand lays blocks in - see {@link org.gumel.jojoha.stand.BuildMode}.
     *
     * <p>Only meaningful in the Utility stance, but kept regardless of stance so that leaving and
     * coming back does not silently reset a player's tool to something else.
     */
    public org.gumel.jojoha.stand.BuildMode buildMode = org.gumel.jojoha.stand.BuildMode.SINGLE;

    // Takes trait as Optional (rather than a bare nullable ResourceLocation) specifically so
    // the codec below never hands a raw null into RecordCodecBuilder's group combinator - see
    // the comment on the codec field for why that matters.
    public JojohaPlayerData(int strength, int vitality, int agility, int endurance, int worthiness,
                             int availableStatPoints, PlayerSpec spec, VampireStage vampireStage,
                             Set<ResourceLocation> unlockedHamonPaths, Optional<ResourceLocation> trait, StandData stand,
                             Energy energy, StandSession standSession,
                             boolean stoneMaskWorn, Map<ResourceLocation, Long> moveCooldowns,
                             Set<ResourceLocation> unlockedNodes) {
        this.strength = strength;
        this.vitality = vitality;
        this.agility = agility;
        this.endurance = endurance;
        this.worthiness = worthiness;
        this.availableStatPoints = availableStatPoints;
        this.spec = spec;
        this.vampireStage = vampireStage;
        this.unlockedHamonPaths = new HashSet<>(unlockedHamonPaths);
        this.unlockedNodes = new HashSet<>(unlockedNodes);
        this.trait = trait.orElse(null);
        this.stand = stand;
        this.specEnergy = energy.spec();
        this.standEnergy = energy.stand();
        this.standSummoned = standSession.summoned();
        this.standMode = StandMode.fromOrdinal(standSession.modeOrdinal());
        this.buildMode = org.gumel.jojoha.stand.BuildMode.fromOrdinal(standSession.buildModeOrdinal());
        this.standPiloting = standSession.piloting();
        this.timeStopExposures = standSession.timeStopExposures();
        this.timeStopFrozenTicks = standSession.timeStopFrozenTicks();
        this.availableStandPoints = standSession.standPoints();
        this.timeStopHeldTicks = standSession.timeStopHeldTicks();
        this.timeStopCastTicks = standSession.timeStopCastTicks();
        this.timeStopCasts = standSession.timeStopCasts();
        this.timeStopUsesThisFight = standSession.timeStopUsesThisFight();
        this.killProgress = standSession.killProgress();

        // Blank entries are empty slots, and a short or absent list simply leaves the rest empty -
        // which is what makes this readable from a save written before the field existed.
        readLoadout(standSession.equipped(), equippedSkills);
        readLoadout(standSession.utilityEquipped(), utilityEquippedSkills);
        this.stoneMaskWorn = stoneMaskWorn;
        this.moveCooldowns.putAll(moveCooldowns);
    }

    private static void readLoadout(java.util.List<String> saved, ResourceLocation[] into) {
        for (int slot = 0; slot < into.length && slot < saved.size(); slot++) {
            String id = saved.get(slot);
            into[slot] = id == null || id.isEmpty() ? null : ResourceLocation.tryParse(id);
        }
    }

    /** The slots as the codec wants them: one entry per slot, empty string for nothing. */
    public java.util.List<String> equippedList() {
        return writeLoadout(equippedSkills);
    }

    public java.util.List<String> utilityEquippedList() {
        return writeLoadout(utilityEquippedSkills);
    }

    private static java.util.List<String> writeLoadout(ResourceLocation[] from) {
        java.util.List<String> out = new java.util.ArrayList<>(from.length);
        for (ResourceLocation id : from) {
            out.add(id == null ? "" : id.toString());
        }
        return out;
    }

    public static JojohaPlayerData createDefault() {
        return new JojohaPlayerData(5, 5, 5, 5, 0, 0, PlayerSpec.NONE, VampireStage.NONE,
                Set.of(), Optional.empty(), StandData.NONE, Energy.DEFAULT,
                StandSession.DEFAULT, false, Map.of(), Set.of());
    }

    public Set<ResourceLocation> getUnlockedHamonPaths() {
        return unlockedHamonPaths;
    }

    public boolean hasHamonPath(ResourceLocation pathId) {
        return unlockedHamonPaths.contains(pathId);
    }

    /** Forgets every path, for when the spec that taught them is taken away. */
    public void clearHamonPaths() {
        unlockedHamonPaths.clear();
    }

    public void grantHamonPath(ResourceLocation pathId) {
        unlockedHamonPaths.add(pathId);
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

    /** Forgets the whole tree, for the commands that reset a player. */
    public void clearNodes() {
        unlockedNodes.clear();
    }

    /** Whether someone else's stopped time currently has this player held in place. */
    public boolean isTimeStopFrozen() {
        return timeStopFrozenTicks > 0;
    }

    /** Whether this player is inside a stop at all, holding it or caught by it. */
    public boolean isInStoppedTime() {
        return timeStopFrozenTicks > 0 || timeStopHeldTicks > 0;
    }

    /** Whether a stop is being wound up. Nothing folds during this - see TimeStopCast. */
    public boolean isCastingTimeStop() {
        return timeStopCastTicks > 0;
    }

    /**
     * How far through the wind-up, 0 to 1.
     *
     * <p>Derived from the remaining ticks rather than tracked separately, so it cannot drift out of
     * step with the counter the server is actually decrementing.
     */
    public float timeStopCastProgress() {
        return timeStopCastTicks <= 0
                ? 0F
                : 1F - (timeStopCastTicks / (float) org.gumel.jojoha.stand.skill.TimeStopCast.CAST_TICKS);
    }

    public boolean isMoveOnCooldown(ResourceLocation moveId, long currentGameTime) {
        Long expiry = moveCooldowns.get(moveId);
        return expiry != null && expiry > currentGameTime;
    }

    public void setMoveCooldown(ResourceLocation moveId, long currentGameTime, int cooldownTicks) {
        moveCooldowns.put(moveId, currentGameTime + cooldownTicks);
    }

    public static final Codec<JojohaPlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("strength").forGetter(d -> d.strength),
            Codec.INT.fieldOf("vitality").forGetter(d -> d.vitality),
            Codec.INT.fieldOf("agility").forGetter(d -> d.agility),
            Codec.INT.fieldOf("endurance").forGetter(d -> d.endurance),
            Codec.INT.fieldOf("worthiness").forGetter(d -> d.worthiness),
            Codec.INT.fieldOf("stat_points").forGetter(d -> d.availableStatPoints),
            PlayerSpec.CODEC.fieldOf("spec").forGetter(d -> d.spec),
            VampireStage.CODEC.fieldOf("vampire_stage").forGetter(d -> d.vampireStage),
            ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("hamon_paths").forGetter(d -> d.unlockedHamonPaths),
            // Codec.optionalFieldOf(key, null) is broken for reference-typed defaults (NPEs
            // internally via Optional.of(null)) - but it turns out that's not actually the whole
            // story: RecordCodecBuilder's own group-combination internals choke on a bare Java
            // null flowing through *any* field slot in a large (>8 field) group, regardless of
            // which codec produced it. So this has to stay Optional<ResourceLocation> all the
            // way through the constructor - unwrapping to a nullable field has to happen
            // strictly outside the codec's value flow, in the constructor body below.
            ResourceLocation.CODEC.optionalFieldOf("trait").forGetter(d -> Optional.ofNullable(d.trait)),
            StandData.CODEC.fieldOf("stand").forGetter(d -> d.stand),
            // optionalFieldOf(key, default) here (not fieldOf) so save data from before these
            // fields existed still decodes - a *required* field missing from older saved NBT
            // fails the whole decode, and the attachment API silently falls back to
            // createDefault(), wiping the entire player (spec, stand, stats, everything), not
            // just the new field. The null-default codec bug documented above doesn't apply
            // here since none of these defaults are null.
            Energy.CODEC.optionalFieldOf("energy", Energy.DEFAULT)
                    .forGetter(d -> new Energy(d.specEnergy, d.standEnergy)),
            StandSession.CODEC.optionalFieldOf("stand_session", StandSession.DEFAULT).forGetter(
                    // Bundled rather than listed flat because RecordCodecBuilder.group tops out at
                    // sixteen fields. Mode travels inside it as an ordinal so an unrecognised value
                    // from a future build clamps rather than failing the decode and wiping the
                    // player - see the note above.
                    d -> new StandSession(d.standSummoned, d.standMode.ordinal(), d.standPiloting,
                            d.timeStopExposures, d.timeStopFrozenTicks, d.timeStopHeldTicks,
                            d.timeStopCastTicks, d.timeStopCasts, d.buildMode.ordinal(),
                            d.timeStopUsesThisFight, d.killProgress, d.equippedList(),
                            d.utilityEquippedList(), d.availableStandPoints)),
            // Last, so adding it did not renumber every parameter the constructor already takes.
            // Optional for the reason spelled out above: a required field absent from older saved
            // NBT fails the whole decode and silently wipes the player.
            Codec.BOOL.optionalFieldOf("stone_mask_worn", false).forGetter(d -> d.stoneMaskWorn),
            Codec.unboundedMap(ResourceLocation.CODEC, Codec.LONG)
                    .optionalFieldOf("move_cooldowns", Map.of())
                    .forGetter(d -> d.moveCooldowns),
            // The sixteenth and last field this group can hold. Anything further has to be bundled
            // into a record of its own, the way StandSession and Energy above were.
            //
            // Optional with an empty default for the reason spelled out above: a required field
            // absent from older saved NBT fails the whole decode, and the attachment API answers a
            // failed decode by handing back a fresh default player - so a save written before the
            // tree existed would lose everything, not merely its nodes.
            ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf)
                    .optionalFieldOf("unlocked_nodes", Set.of())
                    .forGetter(d -> d.unlockedNodes)
    ).apply(instance, JojohaPlayerData::new));
}
