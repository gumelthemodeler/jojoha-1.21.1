package org.gumel.jojoha.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.Level;

public final class MemoryDiscItem extends Item {
    public MemoryDiscItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        ItemStack disc = StandDiscs.copySkin(serverPlayer, stack);
        if (disc.isEmpty()) {
            return InteractionResultHolder.fail(stack);
        }
        return InteractionResultHolder.success(
                ItemUtils.createFilledResult(stack, player, disc, false));
    }
}
