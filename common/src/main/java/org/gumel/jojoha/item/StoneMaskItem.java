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
import org.gumel.jojoha.data.VampireStage;

public class StoneMaskItem extends Item {
    private final boolean aja;

    public StoneMaskItem(Properties properties, boolean aja) {
        super(properties);
        this.aja = aja;
    }

    public boolean aja() {
        return aja;
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

        if (data.stoneMaskWorn || data.stoneMaskRitualTicks > 0) {
            return InteractionResultHolder.fail(stack);
        }

        if (data.vampireStage != (aja ? VampireStage.PILLAR_MAN : VampireStage.NONE)) {
            if (aja) {
                player.displayClientMessage(
                        Component.translatable("message.jojoha.mask.aja_unworthy"), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (!MaskBlood.isBloodied(stack)) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.mask.unfed"), true);
            return InteractionResultHolder.fail(stack);
        }

        if (level.isDay()) {
            player.displayClientMessage(
                    Component.translatable("message.jojoha.mask.daylight"), true);
            return InteractionResultHolder.fail(stack);
        }

        StoneMaskRitual.begin(serverPlayer, data);
        return InteractionResultHolder.success(stack);
    }
}
