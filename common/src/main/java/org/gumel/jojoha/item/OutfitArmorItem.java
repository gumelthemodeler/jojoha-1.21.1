package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.gumel.jojoha.combat.StandLuck;
import org.gumel.jojoha.registry.ModComponents;

import java.util.List;

public class OutfitArmorItem extends ArmorItem {

    public OutfitArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        if (StandLuck.helps(stack)) {
            lines.add(Component.translatable("tooltip.jojoha.stand_luck.1")
                    .withStyle(ChatFormatting.DARK_AQUA));
            lines.add(Component.translatable("tooltip.jojoha.stand_luck.2")
                    .withStyle(ChatFormatting.DARK_AQUA));
        }

        ResourceLocation outfit = stack.get(ModComponents.OUTFIT.get());
        if (outfit == null) return;

        String key = "outfit." + outfit.getNamespace() + "." + outfit.getPath();
        Component name = Component.translatable(key);
        lines.add(Component.translatable("tooltip.jojoha.outfit", name)
                .withStyle(ChatFormatting.GRAY));
    }
}
