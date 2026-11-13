package org.gumel.jojoha.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.gumel.jojoha.Jojoha;

import java.util.HashMap;
import java.util.Map;

public final class StandTuning extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tuning");
    private static final Gson GSON = new GsonBuilder().create();

    private static final Map<ResourceLocation, Float> OVERRIDES = new HashMap<>();

    private static TuningPreset preset = TuningPreset.BASE;

    public StandTuning() {
        super(GSON, "jojoha_tuning");
    }

    public static TuningPreset preset() {
        return preset;
    }

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

    public static float damage(ResourceLocation move, float fallback) {
        return value(move, fallback);
    }

    public static float damage(String path, float fallback) {
        return value(path, fallback);
    }

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
                        Jojoha.LOGGER.warn("{} asks for a tuning preset that does not exist: {}",
                                file, name);
                    } else {
                        preset = named;
                    }
                }

                readInto(file, root, "damage");
                readInto(file, root, "values");
            } catch (Exception failure) {
                Jojoha.LOGGER.error("Could not read tuning file {}", file, failure);
            }
        });

        Jojoha.LOGGER.info("Tuning preset {} ({} values), plus {} explicit overrides",
                preset.lowerName(), preset.size(), OVERRIDES.size());
    }

    private static void readInto(ResourceLocation file, JsonObject root, String block) {
        if (!root.has(block)) {
            return;
        }

        for (Map.Entry<String, JsonElement> entry : GsonHelper.getAsJsonObject(root, block).entrySet()) {
            ResourceLocation key = ResourceLocation.tryParse(entry.getKey());
            if (key == null) {
                Jojoha.LOGGER.warn("{} names a key that is not a valid identifier: {}",
                        file, entry.getKey());
                continue;
            }
            OVERRIDES.put(key, GsonHelper.convertToFloat(entry.getValue(), entry.getKey()));
        }
    }
}
