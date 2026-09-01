package org.gumel.jojoha.data;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.Locale;
import java.util.Map;

/**
 * A whole balance pass, named, so that changing one's mind about the feel of the mod is one line in
 * a datapack rather than thirty.
 *
 * <p>The numbers were tunable individually already, which is the right foundation and the wrong
 * interface. Nobody wants to discover that a Stand feels heavy and then go looking for the eleven
 * separate damage keys that add up to that impression - and a set of numbers only balances against
 * each other, so tuning them one at a time is how a kit ends up internally inconsistent.
 *
 * <p>So a preset is a table, and a datapack picks one:
 *
 * <pre>{@code { "preset": "tuned" }}</pre>
 *
 * <p>Individual keys still win over whatever the preset says - see {@link StandTuning} - so the
 * preset is a starting point rather than a cage.
 */
public enum TuningPreset {
    /**
     * The numbers as written in the code, and the default.
     *
     * <p>Deliberately empty. BASE is not a table of values that happen to match the constants; it is
     * the absence of a table, so the fallback each call site passes is what gets used. That means
     * BASE cannot drift out of step with the code the way a duplicated table would.
     */
    BASE(Map.of()),

    /**
     * A pass over the whole kit for people who want a fight to last longer than one exchange.
     *
     * <p>Two things move, and the second is the point. Damage comes down across the board - not
     * evenly, but hardest on the moves that were ending fights on their own. And time stop is
     * shortened, which no amount of damage tuning could have substituted for: a stop is not a source
     * of damage, it is a window in which none of the usual rules apply, and the length of that window
     * is the single biggest lever on how a fight against one plays out.
     *
     * <p>Uses per fight come down with it. Three ten-second stops and two seven-second ones are
     * different games, and it is the total held time that decides which.
     */
    TUNED(Map.ofEntries(
            // The ordinary exchange: softened, so a trade is a trade rather than a decision.
            Map.entry(id("punch"), 3.0F),
            Map.entry(id("pursuit"), 3.0F),
            Map.entry(id("grab"), 1.5F),

            // Flurries land many times, so a small cut per blow is a large cut per barrage - which
            // is where most of the damage in a fight was actually coming from.
            Map.entry(id("barrage_hit"), 1.3F),
            Map.entry(id("barrage_body_hit"), 1.3F),

            // The finishers. Still finishers, but they have to be set up rather than opened with.
            Map.entry(id("star_finger"), 5.5F),
            Map.entry(id("combo_launch"), 4.0F),
            Map.entry(id("combo_slam"), 4.5F),
            Map.entry(id("uppercut_stand"), 4.5F),
            Map.entry(id("uppercut_body"), 3.5F),

            // The window itself. Seven seconds rather than ten, and twelve rather than twenty for a
            // vampire - long enough to still be the strongest thing in the game, short enough that
            // surviving one is a thing that happens.
            Map.entry(id("time_stop_max_ticks"), 140F),
            Map.entry(id("time_stop_vampire_max_ticks"), 240F),
            Map.entry(id("time_stop_uses_per_fight"), 2F),

            // And longer to recover from having spent them.
            Map.entry(id("time_stop_lockout_ticks"), 1600F)));

    private final Map<ResourceLocation, Float> values;

    TuningPreset(Map<ResourceLocation, Float> values) {
        this.values = values;
    }

    /** What this preset says a key is worth, or null to leave it to the code. */
    public Float get(ResourceLocation key) {
        return values.get(key);
    }

    /** How many values this preset moves - reported at load so a pack author can see it took. */
    public int size() {
        return values.size();
    }

    /** Parsed leniently: an unknown name is worth a warning, not a crash. */
    public static TuningPreset byName(String name) {
        for (TuningPreset preset : values()) {
            if (preset.name().equalsIgnoreCase(name)) {
                return preset;
            }
        }
        return null;
    }

    public String lowerName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }
}
