package org.gumel.jojoha.accessory;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

public final class Accessories {
    public static final int COUNT = AccessorySlot.values().length;
    public static final String SAVE_KEY = "JojohaAccessories";

    public static final int PANEL_X = -39, PANEL_Y = 2, PANEL_W = 41, PANEL_H = 68;

    private static final int[][] SLOTS = {
            {3, 6}, {22, 6},
            {3, 26}, {22, 26},
            {3, 46}, {22, 46},
    };

    public static BooleanSupplier shown = () -> true;

    private Accessories() {
    }

    public interface Holder {
        SimpleContainer jojoha$accessories();

        ItemStack jojoha$shown(int index);
    }

    public static SimpleContainer of(Player player) {
        return ((Holder) player).jojoha$accessories();
    }

    public static ItemStack worn(Player player, AccessorySlot slot) {
        return player.level().isClientSide
                ? ((Holder) player).jojoha$shown(slot.ordinal())
                : of(player).getItem(slot.ordinal());
    }

    public static boolean wearing(Player player, Item item) {
        return item instanceof AccessoryItem accessory && worn(player, accessory.slot()).is(item) && accessory.worksFor(player);
    }

    public static int slotX(AccessorySlot slot) {
        return PANEL_X + SLOTS[slot.ordinal()][0];
    }

    public static int slotY(AccessorySlot slot) {
        return PANEL_Y + SLOTS[slot.ordinal()][1];
    }

    public static void copy(Player from, Player to) {
        SimpleContainer source = of(from);
        SimpleContainer target = of(to);
        for (int i = 0; i < COUNT; i++) {
            target.setItem(i, source.getItem(i).copy());
        }
    }

    public static void showWorn(Player player) {
        SimpleContainer worn = of(player);
        for (int i = 0; i < COUNT; i++) {
            worn.setItem(i, ((Holder) player).jojoha$shown(i).copy());
        }
    }
}
