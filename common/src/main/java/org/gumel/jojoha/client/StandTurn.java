package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.PlacementTurn;

public final class StandTurn {
    private static int turns;

    private StandTurn() {
    }

    public static int turns() {
        return turns;
    }

    public static void tick() {
        if (!inStance(Minecraft.getInstance().player)) {
            turns = 0;
        }
    }

    public static boolean scrolled(double amount) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (amount == 0 || minecraft.screen != null || !RawKey.modifierDown() || !inStance(player)) {
            return false;
        }
        turns = Math.floorMod(turns + (amount > 0 ? 1 : -1), PlacementTurn.TURNS);

        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                SoundEvents.UI_BUTTON_CLICK.value(), 1.6F, 0.35F));

        if (!answersTheKey(player)) {
            minecraft.gui.setOverlayMessage(
                    Component.translatable("message.jojoha.build.no_turn"), false);
        }
        return true;
    }

    private static boolean inStance(LocalPlayer player) {
        return player != null && ClientPlayerDataCache.data.standSummoned
                && ClientPlayerDataCache.data.standMode.handlesItems();
    }

    private static boolean answersTheKey(LocalPlayer player) {
        InteractionHand hand = StandHandsInput.delegatedHand(player);
        if (hand == null) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        return stack.getItem() instanceof BlockItem item
                && PlacementTurn.turnable(item.getBlock().defaultBlockState());
    }
}
