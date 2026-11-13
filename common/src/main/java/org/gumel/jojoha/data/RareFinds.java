package org.gumel.jojoha.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.Jojoha;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RareFinds {
    private static final Map<String, Integer> WORTH = Map.of(
            "requiem_arrow", 60,
            "heavens_arrow", 60,
            "stand_arrow", 40,
            "fractured_skin_arrow", 40,
            "stone_mask", 25,
            "ancient_arrowhead", 20,
            "path_scroll", 18,
            "meteor_map", 15,
            "stand_arrow_shard", 12,
            "meteorite_ingot", 8);

    private static final int MOST_COUNTED = 16;

    private static final int MOST_PAID_PER_DAY = 6;

    private static final double DROP_RANGE = 8.0;

    private static final Map<UUID, Map<String, Integer>> CARRIED = new HashMap<>();

    private static final Map<UUID, Map<String, Integer>> PUT_AWAY = new HashMap<>();

    private static final Set<UUID> OPENED_CONTAINER = new HashSet<>();

    private static final Map<String, int[]> PAID_TODAY = new HashMap<>();
    private static long paidDay = -1;

    private RareFinds() {
    }

    public static void forget(UUID player) {
        CARRIED.remove(player);
        OPENED_CONTAINER.remove(player);
    }

    public static void watch(ServerPlayer player) {
        if (player.containerMenu != player.inventoryMenu) {
            OPENED_CONTAINER.add(player.getUUID());
        }
    }

    public static void check(ServerPlayer player) {
        boolean stashed = OPENED_CONTAINER.remove(player.getUUID()) || player.containerMenu != player.inventoryMenu;
        Map<String, Integer> seen = CARRIED.computeIfAbsent(player.getUUID(), key -> new HashMap<>());
        boolean firstLook = seen.isEmpty();

        for (Map.Entry<String, Integer> entry : WORTH.entrySet()) {
            String item = entry.getKey();
            int now = Math.min(MOST_COUNTED, count(player, item));
            Integer before = seen.put(item, now);

            if (firstLook || before == null || now == before) {
                continue;
            }

            Map<String, Integer> away = PUT_AWAY.computeIfAbsent(player.getUUID(), key -> new HashMap<>());
            if (now < before) {
                if (stashed || droppedNearby(player, item)) {
                    away.merge(item, before - now, (had, more) -> Math.min(MOST_COUNTED, had + more));
                }
                continue;
            }

            int back = Math.min(now - before, away.getOrDefault(item, 0));
            if (back > 0) {
                away.put(item, away.get(item) - back);
            }
            int found = payable(player, item, now - before - back);
            if (found <= 0) {
                continue;
            }

            StatGrowth.award(player, StatPoints.WORTHINESS,
                    StatGrowth.dailyLimited(player, "finds", found * entry.getValue()));
        }
    }

    private static boolean droppedNearby(ServerPlayer player, String item) {
        return !player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(DROP_RANGE),
                dropped -> dropped.getOwner() == player && matches(dropped.getItem(), item)).isEmpty();
    }

    private static boolean matches(ItemStack stack, String item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals(Jojoha.MOD_ID) && id.getPath().equals(item);
    }

    private static int payable(ServerPlayer player, String item, int found) {
        long day = player.level().getGameTime() / 24000L;
        if (day != paidDay) {
            paidDay = day;
            PAID_TODAY.clear();
        }

        int[] paid = PAID_TODAY.computeIfAbsent(player.getUUID() + "/" + item, key -> new int[1]);
        int room = Math.max(0, MOST_PAID_PER_DAY - paid[0]);
        int allowed = Math.min(found, room);
        paid[0] += allowed;
        return allowed;
    }

    private static int count(ServerPlayer player, String item) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (matches(stack, item)) {
                total += stack.getCount();
            }
        }
        return total;
    }
}
