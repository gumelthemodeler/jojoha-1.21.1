package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.skill.Indicator;

import java.util.List;
import java.util.function.Predicate;

public final class AncientBowItem extends BowItem {
    public static final TagKey<Item> STAND_ARROWS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_arrows"));

    private static final Predicate<ItemStack> FIRES = stack -> stack.is(STAND_ARROWS);

    public AncientBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return FIRES;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Indicator.ABILITY.line(
                Component.translatable("item.jojoha.ancient_bow.fires").withStyle(ChatFormatting.GRAY)));
    }
}
