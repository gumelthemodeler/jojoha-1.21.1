package org.gumel.jojoha.dream;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.gumel.jojoha.registry.ModItems;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Worth {
    private static final int GEAR_PIECES = 3;

    private static final int PER_ENCHANT_LEVEL = 2;
    private static final int ENCHANT_CAP = 24;

    private static final int DISC = 6;
    private static final int TEMPLATE = 8;

    private static Map<Item, Integer> table;
    private static List<Item> repairMaterials;

    private Worth() {
    }

    public static int of(ItemStack stack) {
        return stack.isEmpty() ? 0 : perItem(stack) * stack.getCount();
    }

    public static int perItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int named = table().getOrDefault(stack.getItem(), 0);
        int base = named > 0 ? named : kindWorth(stack);
        return base + enchantWorth(stack);
    }

    private static int kindWorth(ItemStack stack) {
        if (stack.has(DataComponents.JUKEBOX_PLAYABLE)) {
            return DISC;
        }
        if (stack.getItem() instanceof SmithingTemplateItem) {
            return TEMPLATE;
        }
        return gearWorth(stack);
    }

    private static int gearWorth(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return 0;
        }
        for (Item material : repairMaterials()) {
            if (stack.getItem().isValidRepairItem(stack, new ItemStack(material))) {
                return table().getOrDefault(material, 0) * GEAR_PIECES;
            }
        }
        return 0;
    }

    private static int enchantWorth(ItemStack stack) {
        int levels = levels(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY))
                + levels(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY));
        return Math.min(ENCHANT_CAP, levels * PER_ENCHANT_LEVEL);
    }

    private static int levels(ItemEnchantments enchantments) {
        int levels = 0;
        for (Holder<Enchantment> held : enchantments.keySet()) {
            levels += enchantments.getLevel(held);
        }
        return levels;
    }

    private static List<Item> repairMaterials() {
        if (repairMaterials == null) {
            repairMaterials = List.of(
                    ModItems.METEORITE_INGOT.get(), Items.NETHERITE_INGOT, Items.DIAMOND,
                    Items.GOLD_INGOT, Items.EMERALD, Items.IRON_INGOT, Items.COPPER_INGOT,
                    Items.AMETHYST_SHARD);
        }
        return repairMaterials;
    }

    private static Map<Item, Integer> table() {
        if (table == null) {
            table = build();
        }
        return table;
    }

    private static Map<Item, Integer> build() {
        Map<Item, Integer> worth = new HashMap<>();

        worth.put(Items.COPPER_INGOT, 1);
        worth.put(Items.RAW_COPPER, 1);
        worth.put(Items.LAPIS_LAZULI, 1);
        worth.put(Items.QUARTZ, 1);
        worth.put(Items.AMETHYST_SHARD, 2);
        worth.put(Items.IRON_INGOT, 2);
        worth.put(Items.RAW_IRON, 1);
        worth.put(Items.GOLD_INGOT, 3);
        worth.put(Items.RAW_GOLD, 2);
        worth.put(Items.EMERALD, 6);
        worth.put(Items.DIAMOND, 8);
        worth.put(Items.ANCIENT_DEBRIS, 10);
        worth.put(Items.NETHERITE_SCRAP, 10);
        worth.put(Items.NETHERITE_INGOT, 45);

        worth.put(Items.COPPER_BLOCK, 9);
        worth.put(Items.LAPIS_BLOCK, 9);
        worth.put(Items.IRON_BLOCK, 18);
        worth.put(Items.GOLD_BLOCK, 27);
        worth.put(Items.EMERALD_BLOCK, 54);
        worth.put(Items.DIAMOND_BLOCK, 72);
        worth.put(Items.NETHERITE_BLOCK, 405);

        worth.put(Items.EXPERIENCE_BOTTLE, 2);
        worth.put(Items.ENDER_PEARL, 3);
        worth.put(Items.BLAZE_ROD, 3);
        worth.put(Items.SADDLE, 4);
        worth.put(Items.GHAST_TEAR, 6);
        worth.put(Items.ENDER_EYE, 6);
        worth.put(Items.DRAGON_BREATH, 6);
        worth.put(Items.NAME_TAG, 6);
        worth.put(Items.NAUTILUS_SHELL, 8);
        worth.put(Items.ECHO_SHARD, 8);
        worth.put(Items.GOLDEN_APPLE, 8);
        worth.put(Items.SHULKER_SHELL, 10);
        worth.put(Items.END_CRYSTAL, 20);
        worth.put(Items.WITHER_SKELETON_SKULL, 30);
        worth.put(Items.HEART_OF_THE_SEA, 40);
        worth.put(Items.TOTEM_OF_UNDYING, 40);
        worth.put(Items.CONDUIT, 45);
        worth.put(Items.ENCHANTED_GOLDEN_APPLE, 45);
        worth.put(Items.TRIDENT, 45);
        worth.put(Items.ELYTRA, 50);
        worth.put(Items.NETHER_STAR, 60);
        worth.put(Items.BEACON, 80);
        worth.put(Items.DRAGON_EGG, 120);

        worth.put(Items.PHANTOM_MEMBRANE, 2);
        worth.put(Items.RABBIT_FOOT, 2);
        worth.put(Items.TURTLE_SCUTE, 3);
        worth.put(Items.BREEZE_ROD, 3);
        worth.put(Items.TRIAL_KEY, 4);
        worth.put(Items.OMINOUS_TRIAL_KEY, 10);
        worth.put(Items.GOAT_HORN, 6);
        worth.put(Items.IRON_HORSE_ARMOR, 4);
        worth.put(Items.GOLDEN_HORSE_ARMOR, 6);
        worth.put(Items.DIAMOND_HORSE_ARMOR, 12);
        worth.put(Items.SNIFFER_EGG, 20);
        worth.put(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 30);
        worth.put(Items.HEAVY_CORE, 40);
        worth.put(Items.WITHER_ROSE, 4);

        worth.put(ModItems.LUCID_LILY.get(), 10);
        worth.put(ModItems.TOTEM_KEY.get(), 15);
        worth.put(ModItems.LABYRINTH_KEY.get(), 10);
        worth.put(ModItems.STAT_CRYSTAL.get(), 40);
        worth.put(ModItems.RED_STONE_OF_AJA.get(), 60);
        worth.put(ModItems.BUBBLE_GLOVES.get(), 15);
        worth.put(ModItems.LISA_SCARF.get(), 15);
        for (RegistrySupplier<Item> curio : List.of(
                ModItems.SPEEDWAGON_TONIC, ModItems.COFFEE_FLAVORED_GUM, ModItems.ZEPPELI_WINE,
                ModItems.BLOOD_VIAL, ModItems.CHERRIES, ModItems.TAROT_CARD, ModItems.LUCKY_COIN,
                ModItems.TORN_MANGA_PAGE, ModItems.ROAD_ROLLER_KEY)) {
            worth.put(curio.get(), 6);
        }
        for (RegistrySupplier<Item> charm : List.of(
                ModItems.STAR_MIRROR, ModItems.CREATIVE_CUBE, ModItems.SKULL_OF_FORTUNE,
                ModItems.WARDING_CHARM, ModItems.FRACTURED_SHOULDER_PLATE)) {
            worth.put(charm.get(), 30);
        }
        for (RegistrySupplier<Item> relic : List.of(
                ModItems.JOSEPHS_FINGERLESS_GLOVES, ModItems.JOTAROS_CHAIN, ModItems.SPEEDWAGONS_BOWLER_HAT,
                ModItems.CAESARS_HEADBAND, ModItems.KAKYOIN_EARRING, ModItems.POLNAREFF_EARRING,
                ModItems.JOTAROS_CAP, ModItems.SHERRYS_LOCKET, ModItems.HAMON_CONTROL_MASK,
                ModItems.RING_OF_AJA, ModItems.DIOS_HEADBAND, ModItems.WAMUUS_HEADDRESS,
                ModItems.WEDDING_RING_OF_DEATH)) {
            worth.put(relic.get(), 35);
        }

        worth.put(ModItems.JOJO_COIN.get(), 5);
        worth.put(ModItems.RAW_METEORITE_SCRAP.get(), 4);
        worth.put(ModItems.RAW_METEORITE.get(), 8);
        worth.put(ModItems.PATH_SCROLL.get(), 15);
        worth.put(ModItems.STAND_ARROW_SHARD.get(), 15);
        worth.put(ModItems.METEORITE_INGOT.get(), 20);
        worth.put(ModItems.METEORITE_TABLET.get(), 30);
        worth.put(ModItems.MEMORY_DISC.get(), 20);
        worth.put(ModItems.METEOR_MAP.get(), 25);
        worth.put(ModItems.ANCIENT_ARROWHEAD.get(), 25);
        worth.put(ModItems.STONE_MASK.get(), 35);
        worth.put(ModItems.STAND_DISC.get(), 20);
        worth.put(ModItems.STAND_ARROW.get(), 100);
        worth.put(ModItems.FRACTURED_SKIN_ARROW.get(), 120);
        worth.put(ModItems.HEAVENS_ARROW.get(), 140);
        worth.put(ModItems.REQUIEM_ARROW.get(), 140);

        for (RegistrySupplier<Item> card : List.of(
                ModItems.SOUL_CARD_JOTARO, ModItems.SOUL_CARD_KAKYOIN, ModItems.SOUL_CARD_POLNAREFF,
                ModItems.SOUL_CARD_AVDOL, ModItems.SOUL_CARD_JOSEPH, ModItems.SOUL_CARD_DIO,
                ModItems.SOUL_CARD_ZEPPELI, ModItems.SOUL_CARD_JONATHAN, ModItems.SOUL_CARD_DIO_P1,
                ModItems.SOUL_CARD_JOSEPH_P2, ModItems.SOUL_CARD_CAESAR)) {
            worth.put(card.get(), 12);
        }
        return worth;
    }
}
