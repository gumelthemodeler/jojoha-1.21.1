package org.gumel.jojoha.stand;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record StandSkin(ResourceLocation texture, String translationKey, float weight, int auraColor, boolean supporter,
                        @Nullable String unlock) {
    public StandSkin(ResourceLocation texture, String translationKey) {
        this(texture, translationKey, DEFAULT_WEIGHT, INHERIT);
    }

    public StandSkin(ResourceLocation texture, String translationKey, int auraColor) {
        this(texture, translationKey, DEFAULT_WEIGHT, auraColor);
    }

    public StandSkin(ResourceLocation texture, String translationKey, float weight, int auraColor) {
        this(texture, translationKey, weight, auraColor, false, null);
    }

    public StandSkin supporterOnly() {
        return new StandSkin(texture, translationKey, 0F, auraColor, true, null);
    }

    public StandSkin earnedBy(String hintKey) {
        return new StandSkin(texture, translationKey, 0F, auraColor, false, hintKey);
    }

    public boolean rollable() {
        return !supporter && unlock == null;
    }

    public static final float DEFAULT_WEIGHT = 1.0F;

    public static final int INHERIT = -1;
}
