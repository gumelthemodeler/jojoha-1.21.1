package org.gumel.jojoha.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.family.FamilyLine;

import java.util.List;

public final class FamilyRecordItem extends Item {
    public FamilyRecordItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Player)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer from && target instanceof ServerPlayer to) {
            if (from.isShiftKeyDown() && FamilyLine.isHeir(from, to)) {
                FamilyLine.releaseHeir(from, to.getUUID());
            } else {
                FamilyLine.invite(from, to);
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            if (server.isShiftKeyDown()) {
                FamilyLine.releaseHeir(server, null);
            } else {
                FamilyLine.show(server);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.jojoha.family_record.tooltip.invite").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.jojoha.family_record.tooltip.read").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.jojoha.family_record.tooltip.release").withStyle(ChatFormatting.GRAY));
    }
}
