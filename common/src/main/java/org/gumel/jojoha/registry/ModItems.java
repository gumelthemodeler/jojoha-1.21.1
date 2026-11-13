package org.gumel.jojoha.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.accessory.AccessoryItem;
import org.gumel.jojoha.accessory.AccessoryRarity;
import org.gumel.jojoha.accessory.AccessorySlot;
import org.gumel.jojoha.data.StatPoints;
import org.gumel.jojoha.dream.DreamRealm;
import org.gumel.jojoha.item.AncientBowItem;
import org.gumel.jojoha.item.BladeItem;
import org.gumel.jojoha.item.CigaretteItem;
import org.gumel.jojoha.item.ClackersItem;
import org.gumel.jojoha.item.CurioItem;
import org.gumel.jojoha.item.Curios;
import org.gumel.jojoha.item.DaggerItem;
import org.gumel.jojoha.item.DariosLetterItem;
import org.gumel.jojoha.item.FamilyRecordItem;
import org.gumel.jojoha.item.FracturedSkinArrowItem;
import org.gumel.jojoha.item.HeadJarItem;
import org.gumel.jojoha.item.HeavensArrowItem;
import org.gumel.jojoha.item.MemoryDiscItem;
import org.gumel.jojoha.item.MeteorMapItem;
import org.gumel.jojoha.item.MeteoriteTablet;
import org.gumel.jojoha.item.MeteoriteTier;
import org.gumel.jojoha.item.OutfitArmorItem;
import org.gumel.jojoha.item.PathScrollItem;
import org.gumel.jojoha.item.ProgenitorMaskItem;
import org.gumel.jojoha.item.Provisions;
import org.gumel.jojoha.item.RequiemArrowItem;
import org.gumel.jojoha.item.RoadRollerKeyItem;
import org.gumel.jojoha.item.StandArrowItem;
import org.gumel.jojoha.item.StandDiscItem;
import org.gumel.jojoha.item.StatCrystalItem;
import org.gumel.jojoha.item.StoneMaskItem;
import org.gumel.jojoha.item.SwordBladeItem;
import org.gumel.jojoha.item.TossableItem;
import org.gumel.jojoha.item.WeaponClass;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.anubis.AnubisItem;

import java.util.List;

import static org.gumel.jojoha.registry.ModRegistries.ITEMS;

public final class ModItems {
    private static final int ARROW_STACK = 8;

    private static final int UNIQUE_ARROW_STACK = 1;

    public static final RegistrySupplier<Item> STAND_ARROW =
            ITEMS.register("stand_arrow", () -> new StandArrowItem(
                    named("stand_arrow", ChatFormatting.AQUA)
                            .stacksTo(ARROW_STACK)));
    private static final int SHARD_REQUIRED_WORTHINESS = 15;
    private static final float SHARD_CHANCE_AT_THRESHOLD = 0.10F;
    private static final float SHARD_CHANCE_AT_FULL_ODDS = 0.35F;

    public static final RegistrySupplier<Item> STAND_ARROW_SHARD =
            ITEMS.register("stand_arrow_shard", () -> new StandArrowItem(
                    named("stand_arrow_shard", ChatFormatting.AQUA)
                            .stacksTo(ARROW_STACK),
                    SHARD_REQUIRED_WORTHINESS,
                    SHARD_CHANCE_AT_THRESHOLD, SHARD_CHANCE_AT_FULL_ODDS, true));

    public static final RegistrySupplier<Item> ANCIENT_BOW =
            ITEMS.register("ancient_bow", () -> new AncientBowItem(new Item.Properties()
                    .durability(384).rarity(Rarity.RARE)));

    public static final RegistrySupplier<Item> MUSIC_DISC_AWAKEN =
            ITEMS.register("music_disc_awaken", () -> disc("awaken"));
    public static final RegistrySupplier<Item> MUSIC_DISC_BLOODY_STREAM =
            ITEMS.register("music_disc_bloody_stream", () -> disc("bloody_stream"));
    public static final RegistrySupplier<Item> MUSIC_DISC_LAST_TRAIN_HOME =
            ITEMS.register("music_disc_last_train_home", () -> disc("last_train_home"));
    public static final RegistrySupplier<Item> MUSIC_DISC_OVERDRIVE =
            ITEMS.register("music_disc_overdrive", () -> disc("overdrive"));
    public static final RegistrySupplier<Item> MUSIC_DISC_PERSISTENCE_INNOCENT_SCREAM =
            ITEMS.register("music_disc_persistence_innocent_scream", () -> disc("persistence_innocent_scream"));
    public static final RegistrySupplier<Item> MUSIC_DISC_ROUNDABOUT =
            ITEMS.register("music_disc_roundabout", () -> disc("roundabout"));
    public static final RegistrySupplier<Item> MUSIC_DISC_SONO_CHI_NO_KIOKU =
            ITEMS.register("music_disc_sono_chi_no_kioku", () -> disc("sono_chi_no_kioku"));
    public static final RegistrySupplier<Item> MUSIC_DISC_SONO_CHI_NO_SADAME =
            ITEMS.register("music_disc_sono_chi_no_sadame", () -> disc("sono_chi_no_sadame"));
    public static final RegistrySupplier<Item> MUSIC_DISC_STAND_PROUD =
            ITEMS.register("music_disc_stand_proud", () -> disc("stand_proud"));
    public static final RegistrySupplier<Item> MUSIC_DISC_STARDUST_CRUSADERS =
            ITEMS.register("music_disc_stardust_crusaders", () -> disc("stardust_crusaders"));
    public static final RegistrySupplier<Item> MUSIC_DISC_WALK_LIKE_AN_EGYPTIAN =
            ITEMS.register("music_disc_walk_like_an_egyptian", () -> disc("walk_like_an_egyptian"));

    public static final List<RegistrySupplier<Item>> CLACKERS = List.of(
            ITEMS.register("clackers_black", () -> new ClackersItem(
                    named("clackers_black", ChatFormatting.AQUA).stacksTo(1))),
            ITEMS.register("clackers_blue", () -> new ClackersItem(
                    named("clackers_blue", ChatFormatting.AQUA).stacksTo(1))),
            ITEMS.register("clackers_green", () -> new ClackersItem(
                    named("clackers_green", ChatFormatting.AQUA).stacksTo(1))),
            ITEMS.register("clackers_orange", () -> new ClackersItem(
                    named("clackers_orange", ChatFormatting.AQUA).stacksTo(1))),
            ITEMS.register("clackers_pink", () -> new ClackersItem(
                    named("clackers_pink", ChatFormatting.AQUA).stacksTo(1))),
            ITEMS.register("clackers_purple", () -> new ClackersItem(
                    named("clackers_purple", ChatFormatting.AQUA).stacksTo(1))),
            ITEMS.register("clackers_red", () -> new ClackersItem(
                    named("clackers_red", ChatFormatting.AQUA).stacksTo(1))),
            ITEMS.register("clackers_white", () -> new ClackersItem(
                    named("clackers_white", ChatFormatting.AQUA).stacksTo(1))),
            ITEMS.register("clackers_yellow", () -> new ClackersItem(
                    named("clackers_yellow", ChatFormatting.AQUA).stacksTo(1))));

    public static final RegistrySupplier<Item> BUBBLE_GLOVES =
            ITEMS.register("bubble_gloves", () -> new AccessoryItem(
                    AccessorySlot.GLOVES,
                    AccessoryRarity.COMMON, new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> LISA_SCARF =
            ITEMS.register("lisa_scarf", () -> new AccessoryItem(
                    AccessorySlot.NECK,
                    AccessoryRarity.COMMON, new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> STAR_MIRROR =
            ITEMS.register("star_mirror", () -> new AccessoryItem(
                    AccessorySlot.CHARM,
                    AccessoryRarity.RARE, new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> CREATIVE_CUBE =
            ITEMS.register("creative_cube", () -> new AccessoryItem(
                    AccessorySlot.CHARM,
                    AccessoryRarity.RARE, new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> SKULL_OF_FORTUNE =
            ITEMS.register("skull_of_fortune", () -> new AccessoryItem(
                    AccessorySlot.CHARM,
                    AccessoryRarity.RARE, new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> WARDING_CHARM =
            ITEMS.register("warding_charm", () -> new AccessoryItem(
                    AccessorySlot.CHARM,
                    AccessoryRarity.RARE, new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> FRACTURED_SHOULDER_PLATE =
            ITEMS.register("fractured_shoulder_plate", () -> new AccessoryItem(
                    AccessorySlot.CHARM,
                    AccessoryRarity.RARE, new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> JOSEPHS_FINGERLESS_GLOVES =
            ITEMS.register("josephs_fingerless_gloves", () -> new AccessoryItem(
                    AccessorySlot.GLOVES,
                    AccessoryRarity.COMMON, new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> JOTAROS_CHAIN =
            ITEMS.register("jotaros_chain", () -> new AccessoryItem(
                    AccessorySlot.BACK,
                    AccessoryRarity.COMMON, new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> SPEEDWAGONS_BOWLER_HAT =
            ITEMS.register("speedwagons_bowler_hat", () -> new AccessoryItem(
                    AccessorySlot.HEAD,
                    AccessoryRarity.COMMON, new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> CAESARS_HEADBAND =
            ITEMS.register("caesars_headband", () -> new AccessoryItem(
                    AccessorySlot.HEAD,
                    AccessoryRarity.UNCOMMON, new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> KAKYOIN_EARRING =
            ITEMS.register("kakyoin_earring", () -> new AccessoryItem(
                    AccessorySlot.HEAD,
                    AccessoryRarity.UNCOMMON, new Item.Properties().stacksTo(1)).forStand(StandTypes.HIEROPHANT_GREEN_ID));

    public static final RegistrySupplier<Item> POLNAREFF_EARRING =
            ITEMS.register("polnareff_earring", () -> new AccessoryItem(
                    AccessorySlot.HEAD,
                    AccessoryRarity.UNCOMMON, new Item.Properties().stacksTo(1)).forStand(StandTypes.SILVER_CHARIOT_ID));

    public static final RegistrySupplier<Item> JOTAROS_CAP =
            ITEMS.register("jotaros_cap", () -> new AccessoryItem(
                    AccessorySlot.HEAD,
                    AccessoryRarity.RARE, new Item.Properties().stacksTo(1)).forStand(StandTypes.STAR_PLATINUM_ID));

    public static final RegistrySupplier<Item> SHERRYS_LOCKET =
            ITEMS.register("sherrys_locket", () -> new AccessoryItem(
                    AccessorySlot.NECK,
                    AccessoryRarity.RARE, new Item.Properties().stacksTo(1)).forStand(StandTypes.SILVER_CHARIOT_ID));

    public static final RegistrySupplier<Item> HAMON_CONTROL_MASK =
            ITEMS.register("hamon_control_mask", () -> new AccessoryItem(
                    AccessorySlot.HEAD,
                    AccessoryRarity.RARE, new Item.Properties().stacksTo(1)).burdened());

    public static final RegistrySupplier<Item> RING_OF_AJA =
            ITEMS.register("ring_of_aja", () -> new AccessoryItem(
                    AccessorySlot.RING,
                    AccessoryRarity.LEGENDARY, new Item.Properties().stacksTo(1)).burdened());

    public static final RegistrySupplier<Item> DIOS_HEADBAND =
            ITEMS.register("dios_headband", () -> new AccessoryItem(
                    AccessorySlot.HEAD,
                    AccessoryRarity.LEGENDARY, new Item.Properties().stacksTo(1)).forStand(StandTypes.THE_WORLD_ID));

    public static final RegistrySupplier<Item> WAMUUS_HEADDRESS =
            ITEMS.register("wamuus_headdress", () -> new AccessoryItem(
                    AccessorySlot.HEAD,
                    AccessoryRarity.LEGENDARY, new Item.Properties().stacksTo(1)).forPillarMen());

    public static final RegistrySupplier<Item> WEDDING_RING_OF_DEATH =
            ITEMS.register("wedding_ring_of_death", () -> new AccessoryItem(
                    AccessorySlot.RING,
                    AccessoryRarity.ANCIENT, new Item.Properties().stacksTo(1)).burdened());

    public static final RegistrySupplier<Item> PATH_SCROLL =
            ITEMS.register("path_scroll", () -> new PathScrollItem(
                    named("path_scroll", ChatFormatting.YELLOW).stacksTo(1)));

    public static final RegistrySupplier<Item> STONE_MASK =
            ITEMS.register("stone_mask", () -> new StoneMaskItem(
                    named("stone_mask", ChatFormatting.DARK_RED).stacksTo(1), false));

    public static final RegistrySupplier<Item> RED_STONE_MASK =
            ITEMS.register("red_stone_mask", () -> new StoneMaskItem(
                    named("red_stone_mask", ChatFormatting.RED).stacksTo(1), true));

    public static final RegistrySupplier<Item> PROGENITOR_MASK =
            ITEMS.register("progenitor_mask", () -> new ProgenitorMaskItem(
                    named("progenitor_mask", ChatFormatting.DARK_RED).stacksTo(1)));

    public static final RegistrySupplier<Item> FAMILY_RECORD =
            ITEMS.register("family_record", () -> new FamilyRecordItem(
                    named("family_record", ChatFormatting.GOLD).stacksTo(1)));

    public static final RegistrySupplier<Item> RED_STONE_OF_AJA =
            ITEMS.register("red_stone_of_aja", () -> new Item(
                    named("red_stone_of_aja", ChatFormatting.RED).stacksTo(1)));

    public static final RegistrySupplier<Item> CAMERA =
            ITEMS.register("camera", () -> new BlockItem(
                    ModRegistries.CAMERA_BLOCK.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> HEAD_JAR =
            ITEMS.register("head_jar", () -> new HeadJarItem(
                    ModRegistries.HEAD_JAR_BLOCK.get(), new Item.Properties().stacksTo(16)));

    public static final RegistrySupplier<Item> HEAVENS_ARROW =
            ITEMS.register("heavens_arrow",
                    () -> new HeavensArrowItem(named("heavens_arrow", ChatFormatting.YELLOW)
                            .stacksTo(ARROW_STACK)));

    public static final RegistrySupplier<Item> REQUIEM_ARROW =
            ITEMS.register("requiem_arrow",
                    () -> new RequiemArrowItem(named("requiem_arrow", ChatFormatting.RED)
                            .stacksTo(UNIQUE_ARROW_STACK)));

    public static final RegistrySupplier<Item> FRACTURED_SKIN_ARROW =
            ITEMS.register("fractured_skin_arrow",
                    () -> new FracturedSkinArrowItem(
                            named("fractured_skin_arrow", ChatFormatting.YELLOW).stacksTo(UNIQUE_ARROW_STACK)));

    public static final RegistrySupplier<Item> MEMORY_DISC =
            ITEMS.register("memory_disc",
                    () -> new MemoryDiscItem(
                            named("memory_disc", ChatFormatting.WHITE).stacksTo(16)));
    public static final RegistrySupplier<Item> STAND_DISC =
            ITEMS.register("stand_disc",
                    () -> new StandDiscItem(
                            named("stand_disc", ChatFormatting.LIGHT_PURPLE).stacksTo(1)));

    public static final RegistrySupplier<Item> RAW_METEORITE =
            ITEMS.register("raw_meteorite", () -> new Item(new Item.Properties().fireResistant()));

    public static final RegistrySupplier<Item> RAW_METEORITE_SCRAP =
            ITEMS.register("raw_meteorite_scrap", () -> new Item(new Item.Properties().fireResistant()));

    public static final RegistrySupplier<Item> TOTEM_KEY =
            ITEMS.register("totem_key", () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistrySupplier<Item> LABYRINTH_KEY =
            ITEMS.register("labyrinth_key", () -> new Item(new Item.Properties().stacksTo(16)
                    .rarity(Rarity.UNCOMMON)));

    public static final RegistrySupplier<Item> ADVANCEMENT_PART_1 =
            ITEMS.register("advancement_part_1", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ADVANCEMENT_PART_2 =
            ITEMS.register("advancement_part_2", () -> new Item(new Item.Properties()));
    public static final RegistrySupplier<Item> ADVANCEMENT_PART_3 =
            ITEMS.register("advancement_part_3", () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> ANCIENT_ARROWHEAD =
            ITEMS.register("ancient_arrowhead", () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> METEORITE_INGOT =
            ITEMS.register("meteorite_ingot", () -> new Item(new Item.Properties().fireResistant()));

    public static final RegistrySupplier<Item> METEORITE_TABLET =
            ITEMS.register("meteorite_tablet", MeteoriteTablet::create);

    public static final RegistrySupplier<Item> METEOR_MAP =
            ITEMS.register("meteor_map", () -> new MeteorMapItem(
                    new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> STAT_CRYSTAL =
            ITEMS.register("stat_crystal", () -> new StatCrystalItem(
                    new Item.Properties().rarity(Rarity.RARE)));

    public static final RegistrySupplier<Item> SPEEDWAGON_TONIC =
            ITEMS.register("speedwagon_tonic", () -> CurioItem.drink(
                    new Item.Properties().stacksTo(16), Curios::tonic));

    public static final RegistrySupplier<Item> COFFEE_FLAVORED_GUM =
            ITEMS.register("coffee_flavored_gum", () -> CurioItem.snack(
                    new Item.Properties().stacksTo(16), Curios::gum));

    public static final RegistrySupplier<Item> ZEPPELI_WINE =
            ITEMS.register("zeppeli_wine", () -> CurioItem.drink(
                    new Item.Properties().stacksTo(16), Curios::wine));

    public static final RegistrySupplier<Item> BLOOD_VIAL =
            ITEMS.register("blood_vial", () -> CurioItem.drink(
                    new Item.Properties().stacksTo(16), Curios::vial));

    public static final RegistrySupplier<Item> CHERRIES =
            ITEMS.register("cherries", () -> CurioItem.snack(
                    new Item.Properties().stacksTo(16), Curios::cherries));

    public static final RegistrySupplier<Item> LUCID_LILY =
            ITEMS.register("lucid_lily", () -> CurioItem.snack(
                    new Item.Properties().stacksTo(16), DreamRealm::eatLily));

    public static final RegistrySupplier<Item> TAROT_CARD =
            ITEMS.register("tarot_card", () -> CurioItem.instant(
                    new Item.Properties().stacksTo(16), Curios::tarot));

    public static final RegistrySupplier<Item> LUCKY_COIN =
            ITEMS.register("lucky_coin", () -> CurioItem.instant(
                    new Item.Properties().stacksTo(16), Curios::coin));

    public static final RegistrySupplier<Item> TORN_MANGA_PAGE =
            ITEMS.register("torn_manga_page", () -> CurioItem.instant(
                    new Item.Properties().stacksTo(16), Curios::page));

    public static final RegistrySupplier<Item> ROAD_ROLLER_KEY =
            ITEMS.register("road_roller_key", () -> new RoadRollerKeyItem(
                    new Item.Properties().stacksTo(16)));

    private static final int STAKE_USES = 120;
    private static final int STAKE_DAMAGE = 2;
    private static final float STAKE_SPEED = -2.4F;

    public static final RegistrySupplier<Item> JOJO_COIN =
            ITEMS.register("jojo_coin", () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> POWER_GEM =
            ITEMS.register("power_gem", () -> CurioItem.instant(
                    new Item.Properties().stacksTo(16).rarity(Rarity.RARE), player -> Provisions.gem(player, StatPoints.STRENGTH)));

    public static final RegistrySupplier<Item> AGILITY_GEM =
            ITEMS.register("agility_gem", () -> CurioItem.instant(
                    new Item.Properties().stacksTo(16).rarity(Rarity.RARE), player -> Provisions.gem(player, StatPoints.AGILITY)));

    public static final RegistrySupplier<Item> ENDURANCE_GEM =
            ITEMS.register("endurance_gem", () -> CurioItem.instant(
                    new Item.Properties().stacksTo(16).rarity(Rarity.RARE), player -> Provisions.gem(player, StatPoints.ENDURANCE)));

    public static final RegistrySupplier<Item> VITALITY_GEM =
            ITEMS.register("vitality_gem", () -> CurioItem.instant(
                    new Item.Properties().stacksTo(16).rarity(Rarity.RARE), player -> Provisions.gem(player, StatPoints.VITALITY)));

    public static final RegistrySupplier<Item> WORTHINESS_GEM =
            ITEMS.register("worthiness_gem", () -> CurioItem.instant(
                    new Item.Properties().stacksTo(16).rarity(Rarity.RARE), player -> Provisions.gem(player, StatPoints.WORTHINESS)));

    public static final RegistrySupplier<Item> WILD_GEM =
            ITEMS.register("wild_gem", () -> CurioItem.instant(
                    new Item.Properties().stacksTo(16).rarity(Rarity.RARE), Provisions::wildGem));

    public static final RegistrySupplier<Item> GARLIC =
            ITEMS.register("garlic", () -> CurioItem.snack(
                    new Item.Properties().stacksTo(64), Provisions::garlic));

    public static final RegistrySupplier<Item> STAKE =
            ITEMS.register("stake", () -> new Item(new Item.Properties().durability(STAKE_USES)
                    .attributes(SwordItem.createAttributes(Tiers.WOOD, STAKE_DAMAGE, STAKE_SPEED))));

    public static final RegistrySupplier<Item> DARIOS_LETTER =
            ITEMS.register("darios_letter", () -> new DariosLetterItem(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> BRANDYWINE =
            ITEMS.register("brandywine", () -> CurioItem.drink(
                    new Item.Properties().stacksTo(16), Provisions::brandywine));

    public static final RegistrySupplier<Item> ROYAL_CROWN =
            ITEMS.register("royal_crown", () -> new AccessoryItem(
                    AccessorySlot.HEAD,
                    AccessoryRarity.LEGENDARY, new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> AJA_SHARD =
            ITEMS.register("aja_shard", () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> DEFECTED_AJA =
            ITEMS.register("defected_aja", () -> new Item(
                    named("red_stone_of_aja", ChatFormatting.RED).stacksTo(1)));

    public static final RegistrySupplier<Item> HEAVY_RING =
            ITEMS.register("heavy_ring", () -> new AccessoryItem(
                    AccessorySlot.RING,
                    AccessoryRarity.UNCOMMON, new Item.Properties().stacksTo(1)).burdened());

    public static final RegistrySupplier<Item> TOBACCO_LEAVES =
            ITEMS.register("tobacco_leaves", () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> CIGARETTE =
            ITEMS.register("cigarette", () -> new CigaretteItem(new Item.Properties().stacksTo(16), false));

    public static final RegistrySupplier<Item> CIGARETTE_LIT =
            ITEMS.register("cigarette_lit", () -> new CigaretteItem(
                    new Item.Properties().durability(CigaretteItem.PUFFS), true));

    public static final RegistrySupplier<Item> COCA_COLA =
            ITEMS.register("coca_cola", () -> new TossableItem(new Item.Properties().stacksTo(16), Provisions::cola));

    public static final RegistrySupplier<Item> TEQUILA =
            ITEMS.register("tequila", () -> CurioItem.drink(
                    new Item.Properties().stacksTo(16), Provisions::tequila));

    public static final RegistrySupplier<Item> ZOMBIE_HORSE =
            ITEMS.register("zombie_horse", () -> CurioItem.instant(
                    new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON), Provisions::thread));

    public static final RegistrySupplier<Item> STEEL_INGOT =
            ITEMS.register("steel_ingot", () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> EMPTY_MUG =
            ITEMS.register("empty_mug", () -> new Item(new Item.Properties()));

    public static final RegistrySupplier<Item> GYROS_COFFEE =
            ITEMS.register("gyros_coffee", () -> CurioItem.drink(
                    new Item.Properties().stacksTo(16), Provisions::coffee));

    public static final RegistrySupplier<Item> HERBAL_TEA =
            ITEMS.register("herbal_tea", () -> CurioItem.drink(
                    new Item.Properties().stacksTo(16), Provisions::tea));

    public static final RegistrySupplier<Item> VALENTINES_NAPKIN =
            ITEMS.register("valentines_napkin", () -> CurioItem.instant(
                    new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), Provisions::napkin));

    private static final int WILLOW_EGG_BASE = 0x3C1F5C;
    private static final int WILLOW_EGG_OVERLAY = 0xB35CFF;

    public static final RegistrySupplier<Item> WILLOW_SPAWN_EGG =
            ITEMS.register("willow_spawn_egg", () -> new SpawnEggItem(
                    ModRegistries.WILLOW.get(),
                    WILLOW_EGG_BASE, WILLOW_EGG_OVERLAY, new Item.Properties()));

    private static final int RISEN_ZOMBIE_EGG_BASE = 0x4A2626;
    private static final int RISEN_ZOMBIE_EGG_OVERLAY = 0x9E0E17;

    public static final RegistrySupplier<Item> RISEN_ZOMBIE_SPAWN_EGG =
            ITEMS.register("zombie_spawn_egg", () -> new SpawnEggItem(
                    ModRegistries.RISEN_ZOMBIE.get(),
                    RISEN_ZOMBIE_EGG_BASE, RISEN_ZOMBIE_EGG_OVERLAY, new Item.Properties()));

    private static final int AMALGAMATION_EGG_BASE = 0x3A2020;
    private static final int AMALGAMATION_EGG_OVERLAY = 0x6E8A4A;

    public static final RegistrySupplier<Item> AMALGAMATION_SPAWN_EGG =
            ITEMS.register("amalgamation_spawn_egg", () -> new SpawnEggItem(
                    ModRegistries.AMALGAMATION.get(),
                    AMALGAMATION_EGG_BASE, AMALGAMATION_EGG_OVERLAY, new Item.Properties()));

    private static final int VAMPIRE_EGG_BASE = 0x1E1414;
    private static final int VAMPIRE_EGG_OVERLAY = 0xC9A640;

    public static final RegistrySupplier<Item> VAMPIRE_SPAWN_EGG =
            ITEMS.register("vampire_spawn_egg", () -> new SpawnEggItem(
                    ModRegistries.VAMPIRE.get(),
                    VAMPIRE_EGG_BASE, VAMPIRE_EGG_OVERLAY, new Item.Properties()));

    public static final RegistrySupplier<Item> HAMON_STUDENT_SPAWN_EGG =
            ITEMS.register("hamon_student_spawn_egg", () -> new SpawnEggItem(
                    ModRegistries.HAMON_STUDENT.get(), 0xE8C36A, 0x8A5A2B, new Item.Properties()));

    public static final RegistrySupplier<Item> STAND_USER_SPAWN_EGG =
            ITEMS.register("stand_user_spawn_egg", () -> new SpawnEggItem(
                    ModRegistries.STAND_USER.get(), 0x3B2E6E, 0xE0C040, new Item.Properties()));

    public static final RegistrySupplier<Item> JOTARO_SPAWN_EGG =
            ITEMS.register("jotaro_spawn_egg", () -> new SpawnEggItem(
                    ModRegistries.JOTARO.get(), 0x2A2A3A, 0xD4AF37, new Item.Properties()));

    public static final RegistrySupplier<Item> JOSEPH_SPAWN_EGG =
            ITEMS.register("joseph_spawn_egg", () -> new SpawnEggItem(
                    ModRegistries.JOSEPH.get(), 0x6B4F2A, 0x8FB8DE, new Item.Properties()));

    public static final RegistrySupplier<Item> AVDOL_SPAWN_EGG =
            ITEMS.register("avdol_spawn_egg", () -> new SpawnEggItem(
                    ModRegistries.AVDOL.get(), 0xC0392B, 0xF4D03F, new Item.Properties()));

    public static final RegistrySupplier<Item> KAKYOIN_SPAWN_EGG =
            ITEMS.register("kakyoin_spawn_egg", () -> new SpawnEggItem(
                    ModRegistries.KAKYOIN.get(), 0x2E8B57, 0xE6C229, new Item.Properties()));

    public static final RegistrySupplier<Item> POLNAREFF_SPAWN_EGG =
            ITEMS.register("polnareff_spawn_egg", () -> new SpawnEggItem(
                    ModRegistries.POLNAREFF.get(), 0xE8E8F0, 0x5A5A6A, new Item.Properties()));

    private static final int GHOST_EGG_BASE = 0xDDE8F5;
    private static final int GHOST_EGG_OVERLAY = 0x7FEBD6;

    public static final RegistrySupplier<Item> GHOST_SPAWN_EGG =
            ITEMS.register("ghost_spawn_egg", () -> new SpawnEggItem(
                    ModRegistries.GHOST.get(),
                    GHOST_EGG_BASE, GHOST_EGG_OVERLAY, new Item.Properties()));

    private static final int VAMPIRE_RAT_EGG_BASE = 0x2A2430;
    private static final int VAMPIRE_RAT_EGG_OVERLAY = 0x9E0E17;

    public static final RegistrySupplier<Item> VAMPIRE_RAT_SPAWN_EGG =
            ITEMS.register("vampire_rat_spawn_egg", () -> new SpawnEggItem(
                    ModRegistries.VAMPIRE_RAT.get(),
                    VAMPIRE_RAT_EGG_BASE, VAMPIRE_RAT_EGG_OVERLAY, new Item.Properties()));

    public static final RegistrySupplier<Item> METEORITE_HELMET =
            ITEMS.register("meteorite_helmet", () -> new OutfitArmorItem(
                    ModRegistries.METEORITE_ARMOR,
                    ArmorItem.Type.HELMET,
                    golden("meteorite_helmet", new Item.Properties()).fireResistant()
                            .durability(ArmorItem.Type.HELMET
                                    .getDurability(48))));

    public static final RegistrySupplier<Item> METEORITE_CHESTPLATE =
            ITEMS.register("meteorite_chestplate", () -> new OutfitArmorItem(
                    ModRegistries.METEORITE_ARMOR,
                    ArmorItem.Type.CHESTPLATE,
                    golden("meteorite_chestplate", new Item.Properties()).fireResistant()
                            .durability(ArmorItem.Type.CHESTPLATE
                                    .getDurability(48))));

    public static final RegistrySupplier<Item> METEORITE_LEGGINGS =
            ITEMS.register("meteorite_leggings", () -> new OutfitArmorItem(
                    ModRegistries.METEORITE_ARMOR,
                    ArmorItem.Type.LEGGINGS,
                    golden("meteorite_leggings", new Item.Properties()).fireResistant()
                            .durability(ArmorItem.Type.LEGGINGS
                                    .getDurability(48))));

    public static final RegistrySupplier<Item> METEORITE_BOOTS =
            ITEMS.register("meteorite_boots", () -> new OutfitArmorItem(
                    ModRegistries.METEORITE_ARMOR,
                    ArmorItem.Type.BOOTS,
                    golden("meteorite_boots", new Item.Properties()).fireResistant()
                            .durability(ArmorItem.Type.BOOTS
                                    .getDurability(48))));

    public static final RegistrySupplier<Item> METEORITE_SCYTHE = ITEMS.register("meteorite_scythe",
            () -> new BladeItem(MeteoriteTier.INSTANCE,
                    WeaponClass.SCYTHE,
                    golden("meteorite_scythe", new Item.Properties()).fireResistant().attributes(
                            SwordItem.createAttributes(
                                    MeteoriteTier.INSTANCE, 3, -2.4F))));

    public static final RegistrySupplier<Item> METEORITE_PICKAXE = ITEMS.register("meteorite_pickaxe",
            () -> new PickaxeItem(
                    MeteoriteTier.INSTANCE,
                    golden("meteorite_pickaxe", new Item.Properties()).fireResistant().attributes(
                            DiggerItem.createAttributes(
                                    MeteoriteTier.INSTANCE, 1.0F, -2.8F))));

    public static final RegistrySupplier<Item> METEORITE_AXE = ITEMS.register("meteorite_axe",
            () -> new AxeItem(
                    MeteoriteTier.INSTANCE,
                    golden("meteorite_axe", new Item.Properties()).fireResistant().attributes(
                            DiggerItem.createAttributes(
                                    MeteoriteTier.INSTANCE, 5.0F, -3.0F))));

    public static final RegistrySupplier<Item> METEORITE_SHOVEL = ITEMS.register("meteorite_shovel",
            () -> new ShovelItem(
                    MeteoriteTier.INSTANCE,
                    golden("meteorite_shovel", new Item.Properties()).fireResistant().attributes(
                            DiggerItem.createAttributes(
                                    MeteoriteTier.INSTANCE, 1.5F, -3.0F))));

    public static final RegistrySupplier<Item> METEORITE_HOE = ITEMS.register("meteorite_hoe",
            () -> new HoeItem(
                    MeteoriteTier.INSTANCE,
                    golden("meteorite_hoe", new Item.Properties()).fireResistant().attributes(
                            DiggerItem.createAttributes(
                                    MeteoriteTier.INSTANCE, -4.0F, 0.0F))));

    public static final RegistrySupplier<Item> PLUCK = ITEMS.register("pluck",
            () -> new SwordBladeItem(Tiers.IRON,
                    named("pluck", ChatFormatting.AQUA)
                    .attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));

    public static final RegistrySupplier<Item> ANUBIS = ITEMS.register("anubis",
            () -> new AnubisItem(Tiers.IRON,
                    new Item.Properties().fireResistant().rarity(Rarity.EPIC)
                            .component(DataComponents.UNBREAKABLE,
                                    new Unbreakable(true))
                            .attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));

    public static final RegistrySupplier<Item> SOUL_CARD_JOTARO =
            ITEMS.register("soul_card_jotaro", ModItems::soulCard);

    public static final RegistrySupplier<Item> SOUL_CARD_KAKYOIN =
            ITEMS.register("soul_card_kakyoin", ModItems::soulCard);

    public static final RegistrySupplier<Item> SOUL_CARD_POLNAREFF =
            ITEMS.register("soul_card_polnareff", ModItems::soulCard);

    public static final RegistrySupplier<Item> SOUL_CARD_AVDOL =
            ITEMS.register("soul_card_avdol", ModItems::soulCard);

    public static final RegistrySupplier<Item> SOUL_CARD_JOSEPH =
            ITEMS.register("soul_card_joseph", ModItems::soulCard);

    public static final RegistrySupplier<Item> SOUL_CARD_DIO =
            ITEMS.register("soul_card_dio", ModItems::soulCard);

    public static final RegistrySupplier<Item> SOUL_CARD_ZEPPELI =
            ITEMS.register("soul_card_zeppeli", ModItems::soulCard);

    public static final RegistrySupplier<Item> SOUL_CARD_JONATHAN =
            ITEMS.register("soul_card_jonathan", ModItems::soulCard);

    public static final RegistrySupplier<Item> SOUL_CARD_DIO_P1 =
            ITEMS.register("soul_card_dio_p1", ModItems::soulCard);

    public static final RegistrySupplier<Item> SOUL_CARD_JOSEPH_P2 =
            ITEMS.register("soul_card_joseph_p2", ModItems::soulCard);

    public static final RegistrySupplier<Item> SOUL_CARD_CAESAR =
            ITEMS.register("soul_card_caesar", ModItems::soulCard);

    private static final int RAPIER_ATTACK_BONUS = 2;

    private static final float RAPIER_ATTACK_SPEED = -2.2F;

    private static final int SABRE_ATTACK_BONUS = 4;
    private static final float SABRE_ATTACK_SPEED = -2.8F;

    public static final RegistrySupplier<Item> RAPIER_WOODEN = rapier("wooden_rapier", Tiers.WOOD);
    public static final RegistrySupplier<Item> RAPIER_STONE = rapier("stone_rapier", Tiers.STONE);
    public static final RegistrySupplier<Item> RAPIER_IRON = rapier("iron_rapier", Tiers.IRON);
    public static final RegistrySupplier<Item> RAPIER_GOLD = rapier("gold_rapier", Tiers.GOLD);
    public static final RegistrySupplier<Item> RAPIER_DIAMOND = rapier("diamond_rapier", Tiers.DIAMOND);
    public static final RegistrySupplier<Item> RAPIER_NETHERITE = rapier("netherite_rapier", Tiers.NETHERITE);
    public static final RegistrySupplier<Item> RAPIER_METEORITE = rapier("meteorite_rapier", MeteoriteTier.INSTANCE,
            golden("meteorite_rapier", new Item.Properties()).fireResistant());

    public static final RegistrySupplier<Item> SABRE_WOODEN = sabre("wooden_sabre", Tiers.WOOD);
    public static final RegistrySupplier<Item> SABRE_STONE = sabre("stone_sabre", Tiers.STONE);
    public static final RegistrySupplier<Item> SABRE_IRON = sabre("iron_sabre", Tiers.IRON);
    public static final RegistrySupplier<Item> SABRE_GOLD = sabre("gold_sabre", Tiers.GOLD);
    public static final RegistrySupplier<Item> SABRE_DIAMOND = sabre("diamond_sabre", Tiers.DIAMOND);
    public static final RegistrySupplier<Item> SABRE_NETHERITE = sabre("netherite_sabre", Tiers.NETHERITE);
    public static final RegistrySupplier<Item> SABRE_METEORITE = sabre("meteorite_sabre", MeteoriteTier.INSTANCE,
            golden("meteorite_sabre", new Item.Properties()).fireResistant());

    private static final int DAGGER_ATTACK_BONUS = 1;
    private static final float DAGGER_ATTACK_SPEED = -2.0F;

    public static final RegistrySupplier<Item> DAGGER_WOODEN = dagger("dagger_wooden", Tiers.WOOD, 2F);
    public static final RegistrySupplier<Item> DAGGER_STONE = dagger("dagger_stone", Tiers.STONE, 4F);
    public static final RegistrySupplier<Item> DAGGER_IRON = dagger("dagger_iron", Tiers.IRON, 5F);
    public static final RegistrySupplier<Item> DAGGER_GOLD = dagger("dagger_gold", Tiers.GOLD, 3F);
    public static final RegistrySupplier<Item> DAGGER_DIAMOND = dagger("dagger_diamond", Tiers.DIAMOND, 6F);
    public static final RegistrySupplier<Item> DAGGER_NETHERITE = dagger("dagger_netherite", Tiers.NETHERITE, 7F);
    public static final RegistrySupplier<Item> DAGGER_METEORITE = dagger("dagger_meteorite",
            MeteoriteTier.INSTANCE, 8F,
            golden("dagger_meteorite", new Ammunition()).fireResistant());

    private static Item.Properties named(String name, ChatFormatting colour,
                                         Item.Properties properties) {
        return properties.component(
                DataComponents.ITEM_NAME,
                Component.translatable("item.jojoha." + name)
                        .withStyle(colour));
    }

    private static Item.Properties named(String name, ChatFormatting colour) {
        return named(name, colour, new Item.Properties());
    }

    private static Item.Properties golden(String name, Item.Properties properties) {
        return named(name, ChatFormatting.GOLD, properties);
    }

    private static RegistrySupplier<Item> rapier(String name, Tier tier) {
        return rapier(name, tier, new Item.Properties());
    }

    private static RegistrySupplier<Item> rapier(String name, Tier tier, Item.Properties properties) {
        return blade(name, tier, properties, RAPIER_ATTACK_BONUS, RAPIER_ATTACK_SPEED,
                WeaponClass.RAPIER);
    }

    private static RegistrySupplier<Item> sabre(String name, Tier tier) {
        return sabre(name, tier, new Item.Properties());
    }

    private static RegistrySupplier<Item> sabre(String name, Tier tier, Item.Properties properties) {
        return blade(name, tier, properties, SABRE_ATTACK_BONUS, SABRE_ATTACK_SPEED,
                WeaponClass.SABRE);
    }

    private static RegistrySupplier<Item> blade(String name, Tier tier, Item.Properties properties,
                                                int bonus, float speed,
                                                WeaponClass weaponClass) {
        return ITEMS.register(name, () -> new BladeItem(tier, weaponClass,
                properties
                        .durability(tier.getUses())
                        .attributes(reach(SwordItem.createAttributes(tier, bonus, speed),
                                weaponClass))));
    }

    private static ItemAttributeModifiers reach(
            ItemAttributeModifiers modifiers,
            WeaponClass weaponClass) {
        if (weaponClass.bonusReach() <= 0.0) {
            return modifiers;
        }

        return modifiers.withModifierAdded(
                Attributes.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(
                                Jojoha.MOD_ID, "blade_reach"),
                        weaponClass.bonusReach(),
                        AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND);
    }

    private static RegistrySupplier<Item> dagger(String name, Tier tier, float throwDamage) {
        return dagger(name, tier, throwDamage, new Ammunition());
    }

    private static RegistrySupplier<Item> dagger(String name, Tier tier, float throwDamage,
                                                 Item.Properties properties) {
        return ITEMS.register(name, () -> new DaggerItem(tier, throwDamage,
                properties
                        .stacksTo(DAGGER_STACK)
                        .attributes(SwordItem.createAttributes(tier, DAGGER_ATTACK_BONUS,
                                DAGGER_ATTACK_SPEED))));
    }

    private static final int DAGGER_STACK = 16;

    private static final class Ammunition extends Item.Properties {
        @Override
        public Item.Properties durability(int uses) {
            return this;
        }
    }

    private static Item disc(String song) {
        return new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                .jukeboxPlayable(ResourceKey.create(Registries.JUKEBOX_SONG,
                        ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, song))));
    }

    private static Item soulCard() {
        return new Item(new Item.Properties().stacksTo(16)
                .rarity(Rarity.EPIC));
    }

    private ModItems() {
    }

    public static void bootstrap() {
    }
}
