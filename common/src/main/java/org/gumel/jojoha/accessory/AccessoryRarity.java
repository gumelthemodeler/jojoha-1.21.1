package org.gumel.jojoha.accessory;

import net.minecraft.ChatFormatting;

import java.util.Locale;

public enum AccessoryRarity {
    COMMON(ChatFormatting.WHITE, 24),
    UNCOMMON(ChatFormatting.YELLOW, 12),
    RARE(ChatFormatting.AQUA, 6),
    LEGENDARY(ChatFormatting.LIGHT_PURPLE, 2),
    ANCIENT(ChatFormatting.GOLD, 1);

    private final ChatFormatting colour;
    private final int lootWeight;

    AccessoryRarity(ChatFormatting colour, int lootWeight) {
        this.colour = colour;
        this.lootWeight = lootWeight;
    }

    public int lootWeight() {
        return lootWeight;
    }

    public ChatFormatting colour() {
        return colour;
    }

    public String translationKey() {
        return "accessory.jojoha.rarity." + name().toLowerCase(Locale.ROOT);
    }
}
