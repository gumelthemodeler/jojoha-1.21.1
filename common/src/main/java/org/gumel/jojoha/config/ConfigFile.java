package org.gumel.jojoha.config;

import org.gumel.jojoha.Jojoha;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.architectury.platform.Platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ConfigFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final String name;
    private final boolean grouped;
    private final List<Setting<?>> settings = new ArrayList<>();
    private List<String> order = List.of();
    private boolean loaded;

    public ConfigFile(String name, boolean grouped) {
        this.name = name;
        this.grouped = grouped;
    }

    public String name() {
        return name;
    }

    public ConfigFile ordered(String... groups) {
        this.order = List.of(groups);
        return this;
    }

    public List<Setting<?>> settings() {
        List<Setting<?>> sorted = new ArrayList<>(settings);
        sorted.sort(Comparator.comparingInt(setting -> rank(setting.group())));
        return Collections.unmodifiableList(sorted);
    }

    private int rank(String group) {
        int at = order.indexOf(group);
        return at < 0 ? order.size() : at;
    }

    public ToggleSetting toggle(String group, String key, String langKey, boolean defaultValue) {
        return add(new ToggleSetting(this, group, key, langKey, defaultValue));
    }

    public NumberSetting number(String group, String key, String langKey, float defaultValue,
                                float min, float max, float step, NumberSetting.Unit unit) {
        return add(new NumberSetting(this, group, key, langKey, defaultValue, min, max, step, unit));
    }

    public <E extends Enum<E>> ChoiceSetting<E> choice(String group, String key, String langKey,
                                                       Class<E> type, E defaultValue) {
        return add(new ChoiceSetting<>(this, group, key, langKey, type, defaultValue));
    }

    private <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        loaded = false;
        return setting;
    }

    void ensureLoaded() {
        if (!loaded) {
            reload();
        }
    }

    public void reload() {
        loaded = true;
        for (Setting<?> setting : settings) {
            setting.reset();
        }
        Path path = path();
        if (!Files.exists(path)) {
            save();
            return;
        }
        boolean complete = true;
        try {
            JsonObject root = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) {
                root = new JsonObject();
            }
            for (Setting<?> setting : settings) {
                JsonObject holder = holderFor(root, setting);
                JsonElement value = holder == null ? null : holder.get(setting.key());
                if (value == null) {
                    complete = false;
                    value = moved(root, setting);
                    if (value == null) {
                        continue;
                    }
                }
                if (!setting.load(value)) {
                    Jojoha.LOGGER.warn("{} has a bad value for {}, using {}", name, setting.key(),
                            setting.defaultValue());
                }
            }
        } catch (IOException | RuntimeException e) {
            Jojoha.LOGGER.warn("{} could not be read, using the defaults: {}", name, e.toString());
            return;
        }
        if (!complete) {
            save();
        }
    }

    public void resetAll() {
        ensureLoaded();
        for (Setting<?> setting : settings) {
            setting.reset();
        }
    }

    public void save() {
        JsonObject root = new JsonObject();
        for (Setting<?> setting : settings()) {
            JsonObject holder = root;
            if (grouped) {
                if (!root.has(setting.group())) {
                    root.add(setting.group(), new JsonObject());
                }
                holder = root.getAsJsonObject(setting.group());
            }
            holder.add(setting.key(), setting.save());
        }
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Jojoha.LOGGER.warn("{} could not be written: {}", name, e.toString());
        }
    }

    private JsonElement moved(JsonObject root, Setting<?> setting) {
        JsonElement found = lookup(root, setting.key(), setting.formerKey());
        if (found != null || !grouped) {
            return found;
        }
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                found = lookup(entry.getValue().getAsJsonObject(), setting.key(), setting.formerKey());
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JsonElement lookup(JsonObject holder, String key, String formerKey) {
        if (holder.has(key) && !holder.get(key).isJsonObject()) {
            return holder.get(key);
        }
        return formerKey != null && holder.has(formerKey) && !holder.get(formerKey).isJsonObject()
                ? holder.get(formerKey) : null;
    }

    private JsonObject holderFor(JsonObject root, Setting<?> setting) {
        if (!grouped) {
            return root;
        }
        JsonElement group = root.get(setting.group());
        return group != null && group.isJsonObject() ? group.getAsJsonObject() : null;
    }

    private Path path() {
        return Platform.getConfigFolder().resolve(name);
    }
}
