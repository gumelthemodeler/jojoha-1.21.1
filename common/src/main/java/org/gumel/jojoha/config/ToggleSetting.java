package org.gumel.jojoha.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class ToggleSetting extends Setting<Boolean> {
    ToggleSetting(ConfigFile file, String group, String key, String langKey, boolean defaultValue) {
        super(file, group, key, langKey, defaultValue);
    }

    public boolean on() {
        return get();
    }

    @Override
    protected Boolean clean(Boolean value) {
        return value != null && value;
    }

    @Override
    protected Boolean read(JsonElement element) {
        return element.getAsBoolean();
    }

    @Override
    protected JsonElement write(Boolean value) {
        return new JsonPrimitive(value);
    }
}
