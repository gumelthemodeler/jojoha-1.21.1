package org.gumel.jojoha.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.Mth;

public final class NumberSetting extends Setting<Float> {
    public enum Unit {
        TIMES,
        PERCENT,
        ONE_IN,
        WHOLE,
        HEALTH,
        HARDNESS,
        PIXELS,
        BLOCKS
    }

    private final float min;
    private final float max;
    private final float step;
    private final Unit unit;

    NumberSetting(ConfigFile file, String group, String key, String langKey, float defaultValue,
                  float min, float max, float step, Unit unit) {
        super(file, group, key, langKey, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit;
    }

    public float value() {
        return get();
    }

    public int whole() {
        return Math.round(get());
    }

    public float chance() {
        return get() / 100F;
    }

    public float min() {
        return min;
    }

    public float max() {
        return max;
    }

    public float step() {
        return step;
    }

    public Unit unit() {
        return unit;
    }

    public float snap(float raw) {
        float steps = Math.round((Mth.clamp(raw, min, max) - min) / step);
        return clean(Math.round((min + steps * step) * 10000F) / 10000F);
    }

    @Override
    protected Float clean(Float value) {
        if (value == null || value.isNaN()) {
            return defaultValue();
        }
        return Mth.clamp(value, min, max);
    }

    @Override
    protected Float read(JsonElement element) {
        return element.getAsFloat();
    }

    @Override
    protected JsonElement write(Float value) {
        return new JsonPrimitive(value);
    }
}
