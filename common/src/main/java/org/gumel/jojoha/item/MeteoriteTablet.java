package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SmithingTemplateItem;

import java.util.List;

public final class MeteoriteTablet {

    private MeteoriteTablet() {
    }

    public static SmithingTemplateItem create() {
        return new SmithingTemplateItem(
                Component.translatable("item.jojoha.meteorite_tablet.applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.jojoha.meteorite_tablet.ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.jojoha.meteorite_tablet.upgrade").withStyle(ChatFormatting.GRAY),
                Component.translatable("item.jojoha.meteorite_tablet.base_slot"),
                Component.translatable("item.jojoha.meteorite_tablet.additions_slot"),
                List.of(armor("helmet"), tool("sword"), armor("chestplate"), tool("pickaxe"),
                        armor("leggings"), tool("axe"), armor("boots"), tool("hoe"), tool("shovel")),
                List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_ingot")));
    }

    private static ResourceLocation armor(String piece) {
        return ResourceLocation.withDefaultNamespace("item/empty_armor_slot_" + piece);
    }

    private static ResourceLocation tool(String kind) {
        return ResourceLocation.withDefaultNamespace("item/empty_slot_" + kind);
    }
}
