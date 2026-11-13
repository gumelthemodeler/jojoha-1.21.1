package org.gumel.jojoha.loot;

import dev.architectury.event.events.common.LootEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.gumel.jojoha.accessory.AccessoryItem;
import org.gumel.jojoha.registry.ModItems;

import java.util.Map;

public final class ArrowLoot {
    private static final Map<ResourceKey<LootTable>, Float> CHESTS = Map.of(
            BuiltInLootTables.WOODLAND_MANSION, 0.22F,
            BuiltInLootTables.ANCIENT_CITY, 0.30F,
            BuiltInLootTables.BASTION_TREASURE, 0.28F,
            BuiltInLootTables.BASTION_OTHER, 0.14F,
            BuiltInLootTables.END_CITY_TREASURE, 0.24F,
            BuiltInLootTables.STRONGHOLD_CORRIDOR, 0.10F,
            BuiltInLootTables.NETHER_BRIDGE, 0.12F,
            BuiltInLootTables.DESERT_PYRAMID, 0.10F,
            BuiltInLootTables.JUNGLE_TEMPLE, 0.12F,
            BuiltInLootTables.SIMPLE_DUNGEON, 0.06F);

    private static final int WEIGHT_ARROW = 1;
    private static final int WEIGHT_SHARD = 5;

    private static final int WEIGHT_SKIN_ARROW = 1;

    private static final Map<ResourceKey<LootTable>, Float> REQUIEM = Map.of(
            BuiltInLootTables.ANCIENT_CITY, 0.006F,
            BuiltInLootTables.WOODLAND_MANSION, 0.004F,
            BuiltInLootTables.BASTION_TREASURE, 0.004F,
            BuiltInLootTables.END_CITY_TREASURE, 0.003F);

    private static final Map<ResourceKey<LootTable>, Float> PATH_SCROLLS = Map.of(
            BuiltInLootTables.STRONGHOLD_LIBRARY, 0.35F,
            BuiltInLootTables.WOODLAND_MANSION, 0.22F,
            BuiltInLootTables.PILLAGER_OUTPOST, 0.20F,
            BuiltInLootTables.DESERT_PYRAMID, 0.18F,
            BuiltInLootTables.JUNGLE_TEMPLE, 0.18F,
            BuiltInLootTables.SIMPLE_DUNGEON, 0.10F);

    private static final Map<ResourceKey<LootTable>, Float> STONE_MASKS = Map.of(
            BuiltInLootTables.DESERT_PYRAMID, 0.16F,
            BuiltInLootTables.JUNGLE_TEMPLE, 0.16F,
            BuiltInLootTables.ANCIENT_CITY, 0.14F,
            BuiltInLootTables.STRONGHOLD_CORRIDOR, 0.10F,
            BuiltInLootTables.WOODLAND_MANSION, 0.10F);

    private static final Map<ResourceKey<LootTable>, Float> ARROWHEADS = Map.ofEntries(
            Map.entry(BuiltInLootTables.ANCIENT_CITY, 0.40F),
            Map.entry(BuiltInLootTables.WOODLAND_MANSION, 0.35F),
            Map.entry(BuiltInLootTables.BASTION_TREASURE, 0.35F),
            Map.entry(BuiltInLootTables.END_CITY_TREASURE, 0.30F),
            Map.entry(BuiltInLootTables.PILLAGER_OUTPOST, 0.25F),
            Map.entry(BuiltInLootTables.DESERT_PYRAMID, 0.22F),
            Map.entry(BuiltInLootTables.JUNGLE_TEMPLE, 0.22F),
            Map.entry(BuiltInLootTables.BASTION_OTHER, 0.20F),
            Map.entry(BuiltInLootTables.NETHER_BRIDGE, 0.20F),
            Map.entry(BuiltInLootTables.STRONGHOLD_CORRIDOR, 0.18F),
            Map.entry(BuiltInLootTables.RUINED_PORTAL, 0.15F),
            Map.entry(BuiltInLootTables.SIMPLE_DUNGEON, 0.12F));

    private static final Map<ResourceKey<LootTable>, Float> ANUBIS = Map.of(
            BuiltInLootTables.DESERT_PYRAMID, 0.03F);

    private static final Map<ResourceKey<LootTable>, Float> STAT_CRYSTALS = Map.of(
            BuiltInLootTables.ANCIENT_CITY, 0.06F,
            BuiltInLootTables.END_CITY_TREASURE, 0.05F,
            BuiltInLootTables.BASTION_TREASURE, 0.05F,
            BuiltInLootTables.WOODLAND_MANSION, 0.04F,
            BuiltInLootTables.STRONGHOLD_LIBRARY, 0.04F,
            BuiltInLootTables.DESERT_PYRAMID, 0.03F,
            BuiltInLootTables.JUNGLE_TEMPLE, 0.03F,
            BuiltInLootTables.SIMPLE_DUNGEON, 0.02F);

    private static final Map<ResourceKey<LootTable>, Float> ACCESSORIES = Map.ofEntries(
            Map.entry(BuiltInLootTables.ANCIENT_CITY, 0.12F),
            Map.entry(BuiltInLootTables.BASTION_TREASURE, 0.12F),
            Map.entry(BuiltInLootTables.WOODLAND_MANSION, 0.10F),
            Map.entry(BuiltInLootTables.END_CITY_TREASURE, 0.10F),
            Map.entry(BuiltInLootTables.SIMPLE_DUNGEON, 0.08F),
            Map.entry(BuiltInLootTables.DESERT_PYRAMID, 0.08F),
            Map.entry(BuiltInLootTables.JUNGLE_TEMPLE, 0.08F),
            Map.entry(BuiltInLootTables.BURIED_TREASURE, 0.08F),
            Map.entry(BuiltInLootTables.STRONGHOLD_CORRIDOR, 0.06F),
            Map.entry(BuiltInLootTables.PILLAGER_OUTPOST, 0.06F),
            Map.entry(BuiltInLootTables.SHIPWRECK_TREASURE, 0.06F),
            Map.entry(BuiltInLootTables.ABANDONED_MINESHAFT, 0.05F));

    private static final Map<ResourceKey<LootTable>, Float> VAULT_KEYS = Map.ofEntries(
            Map.entry(BuiltInLootTables.WOODLAND_MANSION, 0.05F),
            Map.entry(BuiltInLootTables.ANCIENT_CITY, 0.05F),
            Map.entry(BuiltInLootTables.BURIED_TREASURE, 0.05F),
            Map.entry(BuiltInLootTables.DESERT_PYRAMID, 0.04F),
            Map.entry(BuiltInLootTables.JUNGLE_TEMPLE, 0.04F),
            Map.entry(BuiltInLootTables.STRONGHOLD_CORRIDOR, 0.03F),
            Map.entry(BuiltInLootTables.SIMPLE_DUNGEON, 0.03F),
            Map.entry(BuiltInLootTables.PILLAGER_OUTPOST, 0.03F),
            Map.entry(BuiltInLootTables.ABANDONED_MINESHAFT, 0.02F),
            Map.entry(BuiltInLootTables.SHIPWRECK_TREASURE, 0.02F));

    private static final float FISHING_ARROW = 0.005F;

    public static final float BIZARRE_FIND = 0.05F;

    public static final float CURIO_FIND = 0.12F;

    private static final float ARROWHEADS_MIN = 1F;
    private static final float ARROWHEADS_MAX = 3F;

    private ArrowLoot() {
    }

    public static void init() {
        LootEvent.MODIFY_LOOT_TABLE.register((key, context, built) -> {
            if (!built) {
                return;
            }
            Float arrows = CHESTS.get(key);
            if (arrows != null) {
                context.addPool(pool(arrows));
            }

            Float arrowheads = ARROWHEADS.get(key);
            if (arrowheads != null) {
                context.addPool(arrowheadPool(arrowheads));
            }

            Float requiem = REQUIEM.get(key);
            if (requiem != null) {
                context.addPool(single("requiem_arrow", requiem, ModItems.REQUIEM_ARROW.get()));
            }

            Float mask = STONE_MASKS.get(key);
            if (mask != null) {
                context.addPool(single("stone_masks", mask, ModItems.STONE_MASK.get()));
            }

            if (key.equals(BuiltInLootTables.FISHING)) {
                context.addPool(single("fishing_arrows", FISHING_ARROW, ModItems.STAND_ARROW.get()));
            }

            Float anubis = ANUBIS.get(key);
            if (anubis != null) {
                context.addPool(single("anubis", anubis, ModItems.ANUBIS.get()));
            }

            Float crystal = STAT_CRYSTALS.get(key);
            if (crystal != null) {
                context.addPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1F))
                        .when(ConfigChance.of("stat_crystals", crystal))
                        .add(LootItem.lootTableItem(ModItems.STAT_CRYSTAL.get()).setWeight(10))
                        .add(LootItem.lootTableItem(ModItems.WILD_GEM.get()).setWeight(4))
                        .add(LootItem.lootTableItem(ModItems.POWER_GEM.get()).setWeight(2))
                        .add(LootItem.lootTableItem(ModItems.AGILITY_GEM.get()).setWeight(2))
                        .add(LootItem.lootTableItem(ModItems.ENDURANCE_GEM.get()).setWeight(2))
                        .add(LootItem.lootTableItem(ModItems.VITALITY_GEM.get()).setWeight(2))
                        .add(LootItem.lootTableItem(ModItems.WORTHINESS_GEM.get()).setWeight(2)));
            }

            if (key.location().getPath().startsWith("chests/")) {
                context.addPool(bizarrePool(BIZARRE_FIND));
                context.addPool(curioPool(CURIO_FIND));
            }

            Float accessory = ACCESSORIES.get(key);
            if (accessory != null) {
                context.addPool(accessoryPool(accessory));
            }

            Float vaultKey = VAULT_KEYS.get(key);
            if (vaultKey != null) {
                context.addPool(single("labyrinth_keys", vaultKey, ModItems.LABYRINTH_KEY.get()));
            }

            Float scroll = PATH_SCROLLS.get(key);
            if (scroll != null) {
                context.addPool(scrollPool(scroll));
            }
        });
    }

    private static LootPool.Builder bizarrePool(float chance) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1F))
                .when(ConfigChance.of("bizarre_finds", chance))
                .when(BizarreFind::new)
                .add(LootItem.lootTableItem(ModItems.JOJO_COIN.get()).setWeight(30)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1F, 3F))))
                .add(LootItem.lootTableItem(ModItems.ANCIENT_ARROWHEAD.get()).setWeight(20))
                .add(LootItem.lootTableItem(Items.EMERALD).setWeight(20)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1F, 3F))))
                .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(10))
                .add(LootItem.lootTableItem(ModItems.METEOR_MAP.get()).setWeight(10))
                .add(LootItem.lootTableItem(ModItems.STAND_ARROW_SHARD.get()).setWeight(5))
                .add(LootItem.lootTableItem(ModItems.STAT_CRYSTAL.get()).setWeight(2));
    }

    private static LootPool.Builder curioPool(float chance) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1F))
                .when(ConfigChance.of("curios", chance))
                .add(LootItem.lootTableItem(ModItems.CHERRIES.get()).setWeight(14)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1F, 3F))))
                .add(LootItem.lootTableItem(ModItems.COFFEE_FLAVORED_GUM.get()).setWeight(12)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1F, 2F))))
                .add(LootItem.lootTableItem(ModItems.TAROT_CARD.get()).setWeight(12))
                .add(LootItem.lootTableItem(ModItems.SPEEDWAGON_TONIC.get()).setWeight(10))
                .add(LootItem.lootTableItem(ModItems.LUCKY_COIN.get()).setWeight(10))
                .add(LootItem.lootTableItem(ModItems.ZEPPELI_WINE.get()).setWeight(8))
                .add(LootItem.lootTableItem(ModItems.BLOOD_VIAL.get()).setWeight(8)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1F, 2F))))
                .add(LootItem.lootTableItem(ModItems.TORN_MANGA_PAGE.get()).setWeight(8))
                .add(LootItem.lootTableItem(ModItems.ROAD_ROLLER_KEY.get()).setWeight(4))
                .add(LootItem.lootTableItem(ModItems.LUCID_LILY.get()).setWeight(6))
                .add(LootItem.lootTableItem(ModItems.GARLIC.get()).setWeight(8)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1F, 3F))))
                .add(LootItem.lootTableItem(ModItems.COCA_COLA.get()).setWeight(6))
                .add(LootItem.lootTableItem(ModItems.GYROS_COFFEE.get()).setWeight(6))
                .add(LootItem.lootTableItem(ModItems.BRANDYWINE.get()).setWeight(4))
                .add(LootItem.lootTableItem(ModItems.TEQUILA.get()).setWeight(4))
                .add(LootItem.lootTableItem(ModItems.CIGARETTE.get()).setWeight(4)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1F, 2F))))
                .add(LootItem.lootTableItem(ModItems.ZOMBIE_HORSE.get()).setWeight(3))
                .add(LootItem.lootTableItem(ModItems.VALENTINES_NAPKIN.get()).setWeight(1));
    }

    private static LootPool.Builder accessoryPool(float chance) {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1F))
                .when(ConfigChance.of("accessories", chance));
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof AccessoryItem accessory) {
                pool.add(LootItem.lootTableItem(item).setWeight(accessory.rarity().lootWeight()));
            }
        }
        return pool;
    }

    private static LootPool.Builder arrowheadPool(float chance) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1F))
                .when(ConfigChance.of("arrowheads", chance))
                .add(LootItem.lootTableItem(ModItems.ANCIENT_ARROWHEAD.get())
                        .apply(SetItemCountFunction.setCount(
                                UniformGenerator.between(ARROWHEADS_MIN, ARROWHEADS_MAX))));
    }

    private static LootPool.Builder scrollPool(float chance) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1F))
                .when(ConfigChance.of("path_scrolls", chance))
                .add(LootItem.lootTableItem(ModItems.PATH_SCROLL.get())
                        .apply(() -> new StampPathFunction()));
    }

    private static LootPool.Builder single(String loot, float chance, Item item) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1F))
                .when(ConfigChance.of(loot, chance))
                .add(LootItem.lootTableItem(item));
    }

    private static LootPool.Builder pool(float chance) {
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1F))
                .when(ConfigChance.of("stand_arrows", chance))
                .add(LootItem.lootTableItem(ModItems.STAND_ARROW.get())
                        .setWeight(WEIGHT_ARROW))
                .add(LootItem.lootTableItem(ModItems.STAND_ARROW_SHARD.get())
                        .setWeight(WEIGHT_SHARD)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1F, 2F))))
                .add(LootItem.lootTableItem(ModItems.FRACTURED_SKIN_ARROW.get())
                        .setWeight(WEIGHT_SKIN_ARROW));
    }
}
