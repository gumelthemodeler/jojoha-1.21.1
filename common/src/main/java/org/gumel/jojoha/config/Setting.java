package org.gumel.jojoha.config;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    private final ConfigFile file;
    private final String group;
    private final String key;
    private final String langKey;
    private final T defaultValue;
    private T value;
    private String labelArgument;
    private String formerKey;
    private BooleanSupplier available = () -> true;

    protected Setting(ConfigFile file, String group, String key, String langKey, T defaultValue) {
        this.file = file;
        this.group = group;
        this.key = key;
        this.langKey = langKey;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public T get() {
        file.ensureLoaded();
        return value;
    }

    public void set(T value) {
        file.ensureLoaded();
        this.value = clean(value);
    }

    public void reset() {
        this.value = defaultValue;
    }

    public T defaultValue() {
        return defaultValue;
    }

    public ConfigFile file() {
        return file;
    }

    public String group() {
        return group;
    }

    public String key() {
        return key;
    }

    public String formerKey() {
        return formerKey;
    }

    public Setting<T> formerly(String key) {
        this.formerKey = key;
        return this;
    }

    public String langKey() {
        return langKey;
    }

    public String labelArgument() {
        return labelArgument;
    }

    public Setting<T> withLabelArgument(String translationKey) {
        this.labelArgument = translationKey;
        return this;
    }

    public boolean available() {
        return available.getAsBoolean();
    }

    public Setting<T> onlyWhen(BooleanSupplier condition) {
        this.available = condition;
        return this;
    }

    protected T clean(T value) {
        return value;
    }

    protected abstract T read(JsonElement element);

    protected abstract JsonElement write(T value);

    boolean load(JsonElement element) {
        try {
            this.value = clean(read(element));
            return true;
        } catch (RuntimeException badValue) {
            this.value = defaultValue;
            return false;
        }
    }

    JsonElement save() {
        return write(value);
    }
}
