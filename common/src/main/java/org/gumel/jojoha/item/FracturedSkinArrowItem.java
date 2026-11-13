package org.gumel.jojoha.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.StandTypes;

public final class FracturedSkinArrowItem extends Item {
    public FracturedSkinArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (hand == InteractionHand.OFF_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        if (!(level instanceof ServerLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        JojohaPlayerData data = PlayerDataAccess.get(serverPlayer);

        if (!data.stand.isPresent()) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.jojoha.skin_arrow.no_stand"), true);
            return InteractionResultHolder.fail(stack);
        }

        if (data.standArrowRitualTicks > 0 || data.standSkinSwapTicks > 0) {
            return InteractionResultHolder.fail(stack);
        }

        if (StandTypes.byIdOrDefault(data.stand.standId()).rollableSkinCount() < 2) {
            serverPlayer.displayClientMessage(
                    Component.translatable("message.jojoha.skin_arrow.no_skins"), true);
            return InteractionResultHolder.fail(stack);
        }

        data.standArrowRitualSkin = true;
        StandArrowRitual.begin(serverPlayer, data);
        return InteractionResultHolder.success(stack);
    }
}
