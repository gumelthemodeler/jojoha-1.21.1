package org.gumel.jojoha.stand.anubis;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.skill.Indicator;
import org.gumel.jojoha.skill.Inflicts;

import java.util.List;

public final class AnubisItem extends SwordItem {
    public AnubisItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return Passing.bond(server, stack) ? InteractionResultHolder.success(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("item.jojoha.anubis.sealed").withStyle(ChatFormatting.DARK_PURPLE));
        lines.add(Indicator.BLEED.line(Component.translatable("item.jojoha.anubis.draw",
                Inflicts.BLEEDING.name()).withStyle(ChatFormatting.GRAY)));
        lines.add(Indicator.ABILITY.line(
                Component.translatable("item.jojoha.anubis.take").withStyle(ChatFormatting.GRAY)));
    }
}
