package org.gumel.jojoha.data;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

import java.util.Locale;
import java.util.Map;

public enum TuningPreset {
    BASE(Map.of()),

    TUNED(Map.ofEntries(
            Map.entry(id("punch"), 3.0F),
            Map.entry(id("pursuit"), 3.0F),
            Map.entry(id("grab"), 1.5F),

            Map.entry(id("barrage_hit"), 1.3F),
            Map.entry(id("barrage_body_hit"), 1.3F),

            Map.entry(id("star_finger"), 5.5F),
            Map.entry(id("combo_launch"), 4.0F),
            Map.entry(id("combo_slam"), 4.5F),
            Map.entry(id("uppercut_stand"), 4.5F),
            Map.entry(id("uppercut_body"), 3.5F),

            Map.entry(id("time_stop_max_ticks"), 140F),
            Map.entry(id("time_stop_vampire_max_ticks"), 240F),
            Map.entry(id("time_stop_uses_per_fight"), 2F),

            Map.entry(id("time_stop_lockout_ticks"), 1600F)));

    private final Map<ResourceLocation, Float> values;

    TuningPreset(Map<ResourceLocation, Float> values) {
        this.values = values;
    }

    public Float get(ResourceLocation key) {
        return values.get(key);
    }

    public int size() {
        return values.size();
    }

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
