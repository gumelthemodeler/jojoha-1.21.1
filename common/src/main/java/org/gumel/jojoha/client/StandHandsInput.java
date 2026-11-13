package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.data.ClientPlayerDataCache;

public final class StandHandsInput {
    public static final int REPEAT_DELAY_TICKS = 4;

    private StandHandsInput() {
    }

    public static boolean shouldDelegate() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null
                && ClientPlayerDataCache.data.standSummoned
                && ClientPlayerDataCache.data.standMode.handlesItems()
                && delegatedHand(player) != null;
    }

    public static InteractionHand delegatedHand(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && stack.getUseDuration(player) <= 0) {
                return hand;
            }
        }
        return null;
    }
}
