package org.gumel.jojoha.data;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.gumel.jojoha.Jojoha;

/**
 * What a stat is actually worth, and the only place that decides.
 *
 * <p>Before this, three of the player's five did nothing at all - vitality, agility and endurance
 * were numbers you could raise and then not notice - and the two that did anything were priced
 * inline at their call sites. Every curve now lives here, so "what does agility do" has one answer
 * and retuning it is one line rather than a search.
 *
 * <h2>The balance the numbers are anchored to</h2>
 *
 * <p>Stats run 5 to {@link StatPoints#MAX_STAT}, so maxing one is thirty-five points - most of a
 * playthrough's earnings spent on a single line. The rule used to price that: <b>a maxed stat is
 * worth roughly one top-tier enchantment or one potion effect.</b> Not two, and not nothing.
 *
 * <ul>
 *   <li>Strength at 40 adds four damage. Sharpness V adds three, so a maxed stat is a little over
 *       one enchantment - and it applies to fists as well as swords, which is what earns the extra.
 *   <li>Vitality at 40 adds ten hearts, doubling what a player starts with. The one deliberate
 *       exception to the rule - see {@link #MAX_HEALTH_BONUS}.
 *   <li>Agility at 40 adds a fifth to movement. Speed I adds a fifth.
 *   <li>Endurance at 40 adds four armour, half a diamond chestplate, and a fifth of immovability.
 *   <li>Worthiness at 40 adds three luck and half again the energy regeneration.
 * </ul>
 *
 * <p>That anchoring is the whole of the balance argument. A player who has maxed everything is
 * carrying about five enchantments' worth of advantage, spread across five separate investments
 * they could not all have made - which is a strong late character rather than one that has left
 * vanilla behind. Nothing here scales multiplicatively with gear, so a diamond sword does not become
 * a different weapon; it stays a diamond sword swung by someone stronger.
 *
 * <h2>Why the Stand's five are not here</h2>
 *
 * <p>They are, but as numbers rather than modifiers - a Stand has no attribute map of its own that
 * anything reads, so its stats are consulted where they are spent: {@code powerScale} on damage,
 * {@link #standCooldownScale} on cooldowns, and so on. The curves still live here.
 */
public final class StatEffects {
    /** Where every stat starts, and the point at which all of these bonuses are zero. */
    public static final int BASE_STAT = 5;

    /** How far there is to climb, for turning a stat into a fraction of the way up. */
    private static final float CLIMB = StatPoints.MAX_STAT - BASE_STAT;

    // ---- what a maxed player stat is worth -------------------------------------------------------
    private static final double MAX_ATTACK_DAMAGE = 4.0;
    /**
     * Enough to take a maxed player to two full rows of hearts.
     *
     * <p>Twenty, on top of vanillas twenty, which is forty health - twice what anyone starts with.
     * That is deliberately outside the "one enchantment" rule the rest of these follow, and it is
     * the one place the rule is broken on purpose: health is the stat a player can actually see, and
     * a progression system whose most visible number moves by four hearts over an entire playthrough
     * does not read as progression at all.
     *
     * <p>Worth knowing what it costs. Vitality is now comfortably the strongest of the five and the
     * obvious first investment, and a fully invested player takes twice as long to kill.
     */
    private static final double MAX_HEALTH_BONUS = 20.0;
    private static final double MAX_MOVEMENT = 0.20;
    private static final double MAX_ARMOUR = 4.0;
    private static final double MAX_KNOCKBACK = 0.20;
    private static final double MAX_LUCK = 3.0;

    private static final float BODY_DAMAGE_AT_BASE = 1.15F;
    private static final float BODY_DAMAGE_AT_MAX = 1.6F;

    /** And how much faster energy comes back at maxed worthiness. */
    private static final float MAX_REGEN_BONUS = 0.5F;

    // ---- what a maxed Stand stat is worth --------------------------------------------------------
    /**
     * The damage multiplier at each end.
     *
     * <p>The low end is not one. It is what {@code powerScale} already returned at a starting Stand,
     * and it stays that way on purpose: every damage number in the mod was tuned against it, so
     * moving the floor would quietly weaken every move at once while looking like a stat change.
     */
    private static final float POWER_AT_BASE = 1.2F;
    private static final float POWER_AT_MAX = 2.0F;

    private static final float COOLDOWN_CUT_AT_BASE = 0.10F;
    private static final float COOLDOWN_CUT_AT_MAX = 0.40F;

    private static final float COST_CUT_AT_BASE = 0.075F;
    private static final float COST_CUT_AT_MAX = 0.40F;

    /** Blows a guard can hold before it breaks, at each end. */
    private static final int GUARD_HITS_AT_BASE = 5;
    private static final int GUARD_HITS_AT_MAX = 12;

    /** Extra Stand energy at maxed potential, on top of whatever the Trust Tier allows. */
    private static final float MAX_ENERGY_BONUS = 35F;

    private static final ResourceLocation STRENGTH_ID = id("stat_strength");
    private static final ResourceLocation VITALITY_ID = id("stat_vitality");
    private static final ResourceLocation AGILITY_ID = id("stat_agility");
    private static final ResourceLocation ENDURANCE_ARMOUR_ID = id("stat_endurance_armour");
    private static final ResourceLocation ENDURANCE_KNOCKBACK_ID = id("stat_endurance_knockback");
    private static final ResourceLocation WORTHINESS_ID = id("stat_worthiness");

    private StatEffects() {
    }

    /**
     * How far up a stat is, 0 at the starting five and 1 at the cap.
     *
     * <p>Everything below is this times a ceiling, which is why no curve here needs its own clamp:
     * a stat outside the range - from an older save, or a command - lands at one end or the other
     * rather than off the scale.
     */
    public static float climb(int stat) {
        return Math.min(1F, Math.max(0F, (stat - BASE_STAT) / CLIMB));
    }

    // ---- the player -------------------------------------------------------------------------------

    /**
     * Puts the player's five onto their attributes.
     *
     * <p>Called every tick, and cheap because it does nothing when nothing has changed: each
     * modifier is compared against what is already there and only replaced when the number differs.
     * Modifiers are transient, so nothing is written to the player's save and a build that removes a
     * stat cannot leave a permanent bonus behind on every character that ever had it.
     */
    public static void apply(ServerPlayer player, JojohaPlayerData data) {
        set(player, Attributes.ATTACK_DAMAGE, STRENGTH_ID,
                MAX_ATTACK_DAMAGE * climb(data.strength), AttributeModifier.Operation.ADD_VALUE);

        set(player, Attributes.MAX_HEALTH, VITALITY_ID,
                MAX_HEALTH_BONUS * climb(data.vitality), AttributeModifier.Operation.ADD_VALUE);

        // Multiplied rather than added, so it stacks with a speed potion the way a potion stacks
        // with a potion instead of dwarfing it.
        set(player, Attributes.MOVEMENT_SPEED, AGILITY_ID,
                MAX_MOVEMENT * climb(data.agility),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        set(player, Attributes.ARMOR, ENDURANCE_ARMOUR_ID,
                MAX_ARMOUR * climb(data.endurance), AttributeModifier.Operation.ADD_VALUE);
        set(player, Attributes.KNOCKBACK_RESISTANCE, ENDURANCE_KNOCKBACK_ID,
                MAX_KNOCKBACK * climb(data.endurance), AttributeModifier.Operation.ADD_VALUE);

        set(player, Attributes.LUCK, WORTHINESS_ID,
                MAX_LUCK * climb(data.worthiness), AttributeModifier.Operation.ADD_VALUE);
    }

    /**
     * What the player's own STRENGTH multiplies a bare-handed blow by.
     *
     * <p>Separate from the attack damage modifier and not a duplicate of it. That modifier is what
     * vanilla consults when a swing is resolved normally; these moves name their damage outright
     * and never go near it, so without this they would be the only attacks in the mod that a
     * strength stat did nothing for.
     *
     * <p>Deliberately the gentler of the two, because it multiplies rather than adds - 1.15 at the
     * starting five and 1.6 at the cap.
     */
    public static float bodyDamageScale(int strength) {
        return BODY_DAMAGE_AT_BASE + (BODY_DAMAGE_AT_MAX - BODY_DAMAGE_AT_BASE) * climb(strength);
    }

    /** How much faster both energy bars refill. 1 at the starting worthiness. */
    public static float regenScale(JojohaPlayerData data) {
        return 1F + MAX_REGEN_BONUS * climb(data.worthiness);
    }

    // ---- the Stand ---------------------------------------------------------------------------------

    /** What the Stand's POWER multiplies a blow by. */
    public static float powerScale(int power) {
        return POWER_AT_BASE + (POWER_AT_MAX - POWER_AT_BASE) * climb(power);
    }

    /** What a cooldown is multiplied by, given the Stand's SPEED. */
    public static float standCooldownScale(int speed) {
        return 1F - (COOLDOWN_CUT_AT_BASE + (COOLDOWN_CUT_AT_MAX - COOLDOWN_CUT_AT_BASE) * climb(speed));
    }

    /** What an energy cost is multiplied by, given the Stand's ENDURANCE. */
    public static float standCostScale(int endurance) {
        return 1F - (COST_CUT_AT_BASE + (COST_CUT_AT_MAX - COST_CUT_AT_BASE) * climb(endurance));
    }

    /** How many blows the guard holds, given the Stand's PROTECTION. */
    public static int guardHits(int protection) {
        return Math.round(GUARD_HITS_AT_BASE
                + (GUARD_HITS_AT_MAX - GUARD_HITS_AT_BASE) * climb(protection));
    }

    /** Extra Stand energy the Stand's POTENTIAL allows, above what its Trust Tier does. */
    public static float energyBonus(int potential) {
        return MAX_ENERGY_BONUS * climb(potential);
    }

    // ---- plumbing ----------------------------------------------------------------------------------

    private static void set(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id,
                            double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && existing.amount() == amount) {
            return;
        }

        // Removed first. Adding a modifier under an id that is already present is rejected outright,
        // so without this the value would be written once and then never change again.
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    /** Takes every one of them off, for a player who no longer has this mod's say over them. */
    public static void clear(ServerPlayer player) {
        remove(player, Attributes.ATTACK_DAMAGE, STRENGTH_ID);
        remove(player, Attributes.MAX_HEALTH, VITALITY_ID);
        remove(player, Attributes.MOVEMENT_SPEED, AGILITY_ID);
        remove(player, Attributes.ARMOR, ENDURANCE_ARMOUR_ID);
        remove(player, Attributes.KNOCKBACK_RESISTANCE, ENDURANCE_KNOCKBACK_ID);
        remove(player, Attributes.LUCK, WORTHINESS_ID);
    }

    private static void remove(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.removeModifier(id);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }
}
