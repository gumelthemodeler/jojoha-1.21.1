package org.gumel.jojoha.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.stand.anubis.Anubis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AnubisLook {
    private static final int REMEMBERED = 30;

    private static final Map<Integer, Long> NAMED = new HashMap<>();

    private static ItemStack blade;

    private AnubisLook() {
    }

    public static void heard(List<Integer> ids) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        long now = minecraft.level.getGameTime();
        NAMED.values().removeIf(until -> until < now || until - now > REMEMBERED);
        for (int id : ids) {
            NAMED.put(id, now + REMEMBERED);
        }
    }

    public static boolean wields(LivingEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entity == null || minecraft.level == null) {
            return false;
        }
        if (entity == minecraft.player) {
            return Anubis.is(ClientPlayerDataCache.data);
        }
        Long until = NAMED.get(entity.getId());
        long now = minecraft.level.getGameTime();
        return until != null && now <= until && until - now <= REMEMBERED;
    }

    public static ItemStack drawnAs(ItemStack stack, LivingEntity holder) {
        if (!Anubis.isBlade(stack) || !wields(holder) || !inMainHand(holder, stack)) {
            return stack;
        }
        if (blade == null) {
            blade = new ItemStack(ModItems.ANUBIS.get());
        }
        return blade;
    }

    private static boolean inMainHand(LivingEntity holder, ItemStack stack) {
        ItemStack held = holder.getMainHandItem();
        if (stack == held) {
            return true;
        }
        return !InventoryIconContext.inSlot() && stack != holder.getOffhandItem()
                && ItemStack.isSameItemSameComponents(stack, held);
    }

    public static boolean named(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        return player != null && stack == player.getMainHandItem() && Anubis.is(ClientPlayerDataCache.data);
    }
}
