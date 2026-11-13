package org.gumel.jojoha.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.gumel.jojoha.hamon.HamonPath;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.item.PathScrollItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public enum ItemShelves {
    STANDS("stands", () -> ModItems.STAND_ARROW.get(), out -> {
        add(out, ModItems.STAND_ARROW, ModItems.STAND_ARROW_SHARD, ModItems.ANCIENT_ARROWHEAD,
                ModItems.ANCIENT_BOW, ModItems.FRACTURED_SKIN_ARROW, ModItems.HEAVENS_ARROW,
                ModItems.REQUIEM_ARROW, ModItems.MEMORY_DISC, ModItems.STAND_DISC);
    }),
    HAMON("hamon", () -> ModItems.CLACKERS.get(0).get(), out -> {
        for (RegistrySupplier<Item> clackers : ModItems.CLACKERS) {
            add(out, clackers);
        }
        add(out, ModItems.BUBBLE_GLOVES, ModItems.LISA_SCARF);
        for (HamonPath path : HamonPaths.all()) {
            out.accept(PathScrollItem.of(new ItemStack(ModItems.PATH_SCROLL.get()), path));
        }
        add(out, ModItems.STONE_MASK, ModItems.RED_STONE_OF_AJA, ModItems.RED_STONE_MASK,
                ModItems.PROGENITOR_MASK, ModItems.FAMILY_RECORD, ModItems.HEAD_JAR);
        for (HamonPath mode : HamonPaths.modes()) {
            out.accept(PathScrollItem.of(new ItemStack(ModItems.PATH_SCROLL.get()), mode));
        }
    }),
    WEAPONS("weapons", () -> ModItems.RAPIER_METEORITE.get(), out -> {
        add(out, ModItems.DAGGER_WOODEN, ModItems.DAGGER_STONE, ModItems.DAGGER_IRON, ModItems.DAGGER_GOLD,
                ModItems.DAGGER_DIAMOND, ModItems.DAGGER_NETHERITE, ModItems.DAGGER_METEORITE, ModItems.PLUCK,
                ModItems.RAPIER_WOODEN, ModItems.RAPIER_STONE, ModItems.RAPIER_IRON, ModItems.RAPIER_GOLD,
                ModItems.RAPIER_DIAMOND, ModItems.RAPIER_NETHERITE, ModItems.RAPIER_METEORITE,
                ModItems.SABRE_WOODEN, ModItems.SABRE_STONE, ModItems.SABRE_IRON, ModItems.SABRE_GOLD,
                ModItems.SABRE_DIAMOND, ModItems.SABRE_NETHERITE, ModItems.SABRE_METEORITE,
                ModItems.METEORITE_SCYTHE);
    }),
    GEAR("gear", () -> ModItems.METEORITE_CHESTPLATE.get(), out -> {
        add(out, ModItems.METEORITE_PICKAXE, ModItems.METEORITE_AXE, ModItems.METEORITE_SHOVEL,
                ModItems.METEORITE_HOE, ModItems.METEORITE_HELMET, ModItems.METEORITE_CHESTPLATE,
                ModItems.METEORITE_LEGGINGS, ModItems.METEORITE_BOOTS);
    }),
    ACCESSORIES("accessories", () -> ModItems.JOTAROS_CAP.get(), out -> {
        add(out, ModItems.STAR_MIRROR, ModItems.CREATIVE_CUBE, ModItems.SKULL_OF_FORTUNE, ModItems.WARDING_CHARM,
                ModItems.FRACTURED_SHOULDER_PLATE, ModItems.JOSEPHS_FINGERLESS_GLOVES, ModItems.JOTAROS_CHAIN,
                ModItems.SPEEDWAGONS_BOWLER_HAT, ModItems.CAESARS_HEADBAND, ModItems.KAKYOIN_EARRING,
                ModItems.POLNAREFF_EARRING, ModItems.JOTAROS_CAP, ModItems.SHERRYS_LOCKET,
                ModItems.HAMON_CONTROL_MASK, ModItems.RING_OF_AJA, ModItems.DIOS_HEADBAND,
                ModItems.WAMUUS_HEADDRESS, ModItems.WEDDING_RING_OF_DEATH, ModItems.ROYAL_CROWN, ModItems.HEAVY_RING);
    }),
    MATERIALS("materials", () -> ModItems.METEORITE_INGOT.get(), out -> {
        add(out, ModItems.RAW_METEORITE, ModItems.RAW_METEORITE_SCRAP, ModItems.METEORITE_INGOT,
                ModItems.METEORITE_TABLET, ModItems.METEOR_MAP, ModItems.STAT_CRYSTAL,
                ModItems.SOUL_CARD_JOTARO, ModItems.SOUL_CARD_KAKYOIN, ModItems.SOUL_CARD_POLNAREFF,
                ModItems.SOUL_CARD_AVDOL, ModItems.SOUL_CARD_JOSEPH, ModItems.SOUL_CARD_DIO,
                ModItems.SOUL_CARD_ZEPPELI, ModItems.SOUL_CARD_JONATHAN, ModItems.SOUL_CARD_DIO_P1,
                ModItems.SOUL_CARD_JOSEPH_P2, ModItems.SOUL_CARD_CAESAR,
                ModItems.TOTEM_KEY, ModItems.LABYRINTH_KEY, ModItems.CAMERA, ModItems.JOJO_COIN,
                ModItems.SPEEDWAGON_TONIC, ModItems.COFFEE_FLAVORED_GUM, ModItems.ZEPPELI_WINE, ModItems.BLOOD_VIAL,
                ModItems.CHERRIES, ModItems.TAROT_CARD, ModItems.LUCKY_COIN, ModItems.TORN_MANGA_PAGE,
                ModItems.ROAD_ROLLER_KEY, ModItems.LUCID_LILY, ModItems.POWER_GEM, ModItems.AGILITY_GEM,
                ModItems.ENDURANCE_GEM, ModItems.VITALITY_GEM, ModItems.WORTHINESS_GEM, ModItems.WILD_GEM,
                ModItems.AJA_SHARD, ModItems.DEFECTED_AJA, ModItems.STEEL_INGOT,
                ModItems.TOBACCO_LEAVES, ModItems.CIGARETTE, ModItems.CIGARETTE_LIT, ModItems.GARLIC, ModItems.STAKE,
                ModItems.DARIOS_LETTER, ModItems.BRANDYWINE, ModItems.COCA_COLA, ModItems.TEQUILA, ModItems.EMPTY_MUG,
                ModItems.GYROS_COFFEE, ModItems.HERBAL_TEA, ModItems.ZOMBIE_HORSE, ModItems.VALENTINES_NAPKIN);
    }),
    SPAWN_EGGS("spawn_eggs", () -> ModItems.VAMPIRE_SPAWN_EGG.get(), out -> {
        add(out, ModItems.WILLOW_SPAWN_EGG, ModItems.RISEN_ZOMBIE_SPAWN_EGG, ModItems.AMALGAMATION_SPAWN_EGG, ModItems.VAMPIRE_SPAWN_EGG,
                ModItems.GHOST_SPAWN_EGG, ModItems.VAMPIRE_RAT_SPAWN_EGG, ModItems.HAMON_STUDENT_SPAWN_EGG,
                ModItems.STAND_USER_SPAWN_EGG, ModItems.JOTARO_SPAWN_EGG, ModItems.JOSEPH_SPAWN_EGG, ModItems.AVDOL_SPAWN_EGG, ModItems.KAKYOIN_SPAWN_EGG, ModItems.POLNAREFF_SPAWN_EGG);
    }),
    MUSIC("music", () -> ModItems.MUSIC_DISC_STAND_PROUD.get(), out -> {
        add(out, ModItems.MUSIC_DISC_AWAKEN, ModItems.MUSIC_DISC_BLOODY_STREAM, ModItems.MUSIC_DISC_LAST_TRAIN_HOME,
                ModItems.MUSIC_DISC_OVERDRIVE, ModItems.MUSIC_DISC_PERSISTENCE_INNOCENT_SCREAM,
                ModItems.MUSIC_DISC_ROUNDABOUT, ModItems.MUSIC_DISC_SONO_CHI_NO_KIOKU,
                ModItems.MUSIC_DISC_SONO_CHI_NO_SADAME, ModItems.MUSIC_DISC_STAND_PROUD,
                ModItems.MUSIC_DISC_STARDUST_CRUSADERS, ModItems.MUSIC_DISC_WALK_LIKE_AN_EGYPTIAN);
    });

    private final String key;
    private final Supplier<ItemLike> icon;
    private final Consumer<Consumer<ItemStack>> fill;
    private Set<Item> items;

    ItemShelves(String key, Supplier<ItemLike> icon, Consumer<Consumer<ItemStack>> fill) {
        this.key = key;
        this.icon = icon;
        this.fill = fill;
    }

    @SafeVarargs
    private static void add(Consumer<ItemStack> out, RegistrySupplier<? extends Item>... items) {
        for (RegistrySupplier<? extends Item> item : items) {
            out.accept(new ItemStack(item.get()));
        }
    }

    public String key() {
        return key;
    }

    public ItemStack icon() {
        return new ItemStack(icon.get());
    }

    public List<ItemStack> stacks() {
        List<ItemStack> stacks = new ArrayList<>();
        fill.accept(stacks::add);
        return stacks;
    }

    public boolean holds(ItemStack stack) {
        if (items == null) {
            Set<Item> held = new HashSet<>();
            for (ItemStack shelved : stacks()) {
                held.add(shelved.getItem());
            }
            items = held;
        }
        return items.contains(stack.getItem());
    }
}
