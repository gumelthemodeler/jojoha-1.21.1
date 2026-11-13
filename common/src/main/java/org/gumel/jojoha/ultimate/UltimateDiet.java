package org.gumel.jojoha.ultimate;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.combat.VampireTraits;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.VampireStage;

public final class UltimateDiet {
    private static final ThreadLocal<Player> EATER = new ThreadLocal<>();

    private UltimateDiet() {
    }

    public static void init() {
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) -> {
            ItemStack held = player.getItemInHand(hand);
            if (!held.has(DataComponents.FOOD) || !ultimate(player)) {
                return CompoundEventResult.pass();
            }
            player.displayClientMessage(Component.translatable("message.jojoha.ultimate.no_food"), true);
            return CompoundEventResult.interruptFalse(held);
        });
    }

    public static boolean ultimate(Player player) {
        if (player instanceof ServerPlayer served) {
            return PlayerDataAccess.get(served).vampireStage == VampireStage.ULTIMATE_LIFEFORM;
        }
        return player.isLocalPlayer() && ClientPlayerDataCache.data != null
                && ClientPlayerDataCache.data.vampireStage == VampireStage.ULTIMATE_LIFEFORM;
    }

    public static void eating(Player player) {
        EATER.set(player);
    }

    public static void doneEating() {
        EATER.remove();
    }

    public static float exhaustion(float amount) {
        Player player = EATER.get();
        return player instanceof ServerPlayer && ultimate(player)
                ? amount * VampireTraits.ULTIMATE_THIRST_SCALE : amount;
    }
}
