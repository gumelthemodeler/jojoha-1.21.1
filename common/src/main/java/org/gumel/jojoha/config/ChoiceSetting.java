package org.gumel.jojoha.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public final class ChoiceSetting<E extends Enum<E>> extends Setting<E> {
    private final Class<E> type;

    ChoiceSetting(ConfigFile file, String group, String key, String langKey, Class<E> type, E defaultValue) {
        super(file, group, key, langKey, defaultValue);
        this.type = type;
    }

    public E[] options() {
        return type.getEnumConstants();
    }

    public static String name(Enum<?> option) {
        return option.name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected E clean(E value) {
        return value == null ? defaultValue() : value;
    }

    @Override
    protected E read(JsonElement element) {
        return Enum.valueOf(type, element.getAsString().trim().toUpperCase(Locale.ROOT));
    }

    @Override
    protected JsonElement write(E value) {
        return new JsonPrimitive(name(value));
    }
}
