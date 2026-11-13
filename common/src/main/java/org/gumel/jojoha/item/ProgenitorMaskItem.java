package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.pillar.PillarBirth;

import java.util.List;

public class ProgenitorMaskItem extends Item {
    public ProgenitorMaskItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer served)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (!PillarBirth.mark(served)) {
            return InteractionResultHolder.fail(stack);
        }
        stack.consume(1, player);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.jojoha.progenitor_mask.desc").withStyle(ChatFormatting.GRAY));
    }
}
