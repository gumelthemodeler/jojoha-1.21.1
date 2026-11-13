package org.gumel.jojoha.combat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.Jojoha;

public final class StandLuck {
    public static final TagKey<Item> LUCKY_ARMOUR = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand_luck"));

    public static final float PER_PIECE = 0.025F;

    private StandLuck() {
    }

    public static boolean helps(ItemStack stack) {
        return !stack.isEmpty() && stack.is(LUCKY_ARMOUR);
    }

    public static float bonusFor(Player player) {
        int worn = 0;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                continue;
            }
            if (helps(player.getItemBySlot(slot))) {
                worn++;
            }
        }

        return worn * PER_PIECE;
    }
}
