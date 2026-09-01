package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.gumel.jojoha.registry.ModComponents;

import java.util.List;

/**
 * Armour that says what it is wearing.
 *
 * <p>A dressed piece is otherwise indistinguishable from an undressed one in a slot - same sprite,
 * same name, same numbers - so without a line in the tooltip there is no way to tell two chestplates
 * apart until you put one on. That is the whole reason this subclass exists; nothing else about it
 * differs from {@link ArmorItem}.
 *
 * <p>The outfit's name is a translation key built from its id, so an outfit added later needs a lang
 * entry and nothing here. An outfit with no entry falls back to showing its id, which is ugly but
 * honest - better than a blank line that tells you nothing.
 */
public class OutfitArmorItem extends ArmorItem {

    public OutfitArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        ResourceLocation outfit = stack.get(ModComponents.OUTFIT.get());
        if (outfit == null) return;

        String key = "outfit." + outfit.getNamespace() + "." + outfit.getPath();
        Component name = Component.translatable(key);
        lines.add(Component.translatable("tooltip.jojoha.outfit", name)
                .withStyle(ChatFormatting.GRAY));
    }
}
