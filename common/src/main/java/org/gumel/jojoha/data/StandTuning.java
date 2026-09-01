package org.gumel.jojoha.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.gumel.jojoha.Jojoha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Every number in the mod that anyone might reasonably disagree about, in one place a datapack can
 * reach.
 *
 * <p>Reads {@code data/<namespace>/jojoha_tuning/*.json}. A file may name a preset, override
 * individual keys, or both:
 *
 * <pre>{@code
 * {
 *   "preset": "tuned",
 *   "damage": { "jojoha:punch": 2.5 },
 *   "values": { "jojoha:time_stop_max_ticks": 100 }
 * }
 * }</pre>
 *
 * <h2>How a number is decided</h2>
 *
 * <p>Three sources, in this order, and the first one with an answer wins:
 *
 * <ol>
 *   <li>an explicit key in a datapack file;</li>
 *   <li>the active {@link TuningPreset};</li>
 *   <li>the constant the calling code passes as its fallback.</li>
 * </ol>
 *
 * <p>That ordering is what makes a preset a starting point rather than a cage: picking TUNED and
 * then disagreeing about exactly one move costs one line, not a fork of the whole table.
 *
 * <h2>Why damage and values are the same map</h2>
 *
 * <p>They were separate ideas for about ten minutes. A damage figure and a duration are both a
 * number a designer wants to move without recompiling, and the only thing distinguishing them is
 * which call site reads them - which is precisely what the key already says. Two maps would have
 * meant two lookup paths, two merge orders and two places to forget the preset.
 *
 * <p>The {@code damage} and {@code values} blocks in the file stay separate because they read better
 * to a human writing one, not because anything downstream cares.
 */
public final class StandTuning extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tuning");

    private static final Logger LOGGER = LoggerFactory.getLogger("jojoha-tuning");
    private static final com.google.gson.Gson GSON = new com.google.gson.GsonBuilder().create();

    /** Explicit per-key overrides from datapack files. Beats the preset. */
    private static final Map<ResourceLocation, Float> OVERRIDES = new HashMap<>();

    private static TuningPreset preset = TuningPreset.BASE;

    public StandTuning() {
        super(GSON, "jojoha_tuning");
    }

    /** Which pass is currently in force. */
    public static TuningPreset preset() {
        return preset;
    }

    /**
     * A tunable number, or the caller's own constant if nobody has an opinion about it.
     *
     * <p>The fallback is not a default in a table somewhere - it is the value written at the call
     * site, which is what BASE means. Nothing has to be kept in step with anything.
     */
    public static float value(ResourceLocation key, float fallback) {
        Float override = OVERRIDES.get(key);
        if (override != null) {
            return override;
        }

        Float fromPreset = preset.get(key);
        return fromPreset != null ? fromPreset : fallback;
    }

    public static float value(String path, float fallback) {
        return value(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path), fallback);
    }

    /**
     * The same lookup, named for what most callers are asking about.
     *
     * <p>Kept as its own method because every existing call site reads better for it, and because a
     * damage figure genuinely is the common case.
     */
    public static float damage(ResourceLocation move, float fallback) {
        return value(move, fallback);
    }

    public static float damage(String path, float fallback) {
        return value(path, fallback);
    }

    /** A tunable whole number - durations and counts, which are meaningless as fractions. */
    public static int ticks(String path, int fallback) {
        return Math.max(0, Math.round(value(path, fallback)));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager,
                         ProfilerFiller profiler) {
        OVERRIDES.clear();
        preset = TuningPreset.BASE;

        files.forEach((file, element) -> {
            try {
                JsonObject root = GsonHelper.convertToJsonObject(element, "tuning");

                if (root.has("preset")) {
                    String name = GsonHelper.getAsString(root, "preset");
                    TuningPreset named = TuningPreset.byName(name);
                    if (named == null) {
                        LOGGER.warn("[jojoha] {} asks for a tuning preset that does not exist: {}",
                                file, name);
                    } else {
                        preset = named;
                    }
                }

                // Both blocks land in the same map. The split is for whoever is writing the file.
                readInto(file, root, "damage");
                readInto(file, root, "values");
            } catch (Exception failure) {
                LOGGER.error("[jojoha] Could not read tuning file {}", file, failure);
            }
        });

        LOGGER.info("[jojoha] Tuning preset {} ({} values), plus {} explicit overrides",
                preset.lowerName(), preset.size(), OVERRIDES.size());
    }

    private static void readInto(ResourceLocation file, JsonObject root, String block) {
        if (!root.has(block)) {
            return;
        }

        for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(root, block).entrySet()) {
            ResourceLocation key = ResourceLocation.tryParse(entry.getKey());
            if (key == null) {
                LOGGER.warn("[jojoha] {} names a key that is not a valid identifier: {}",
                        file, entry.getKey());
                continue;
            }
            OVERRIDES.put(key, GsonHelper.convertToFloat(entry.getValue(), entry.getKey()));
        }
    }
}
