package org.gumel.jojoha.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import org.gumel.jojoha.item.DaggerItem;
import org.gumel.jojoha.item.StandArrowItem;
import org.gumel.jojoha.item.StoneMaskItem;

import static org.gumel.jojoha.registry.ModRegistries.ITEMS;

/**
 * Items named in the design doc, registered here to exercise the registry pipeline end to
 * end. Only Stand Arrow has real behavior wired up so far (see StandArrowItem); the rest are
 * still inert placeholders.
 */
public final class ModItems {
    public static final RegistrySupplier<Item> STAND_ARROW =
            ITEMS.register("stand_arrow", () -> new StandArrowItem(
                    named("stand_arrow", net.minecraft.ChatFormatting.AQUA)));
    /** How a shard is priced: same gate as a whole arrow, far worse odds, capped short of certain. */
    private static final int SHARD_REQUIRED_WORTHINESS = 15;
    private static final float SHARD_CHANCE_AT_THRESHOLD = 0.20F;
    private static final float SHARD_CHANCE_AT_FULL_ODDS = 0.55F;

    /**
     * A broken piece of an arrow, and a worse bet than one.
     *
     * <p>Used exactly as a whole arrow is - same threshold, same stab, same ritual - and it can
     * still wake a Stand. What it cannot do is promise one. A whole arrow at or above the worthiness
     * threshold always works; a shard is a coin weighted by how worthy you are, from one in five at
     * the threshold to a little better than half at a hundred, and no further. Being worthy does not
     * make a broken thing whole, so the ceiling is deliberately short of certain - a shard is
     * something you gather several of, not something you save up for.
     *
     * <p>Failure costs the shard and a little blood; see StandArrowRitual for why the roll happens
     * at the end of the ritual rather than at the stab.
     */
    public static final RegistrySupplier<Item> STAND_ARROW_SHARD =
            ITEMS.register("stand_arrow_shard", () -> new StandArrowItem(
                    named("stand_arrow_shard", net.minecraft.ChatFormatting.AQUA),
                    SHARD_REQUIRED_WORTHINESS,
                    SHARD_CHANCE_AT_THRESHOLD, SHARD_CHANCE_AT_FULL_ODDS, true));

    /**
     * The Stone Mask: one use, and it is not an equipment slot.
     *
     * <p>Worn on the face rather than in a slot, because nothing about it is removable - putting it
     * on is a thing that happens to you. See StoneMaskRitual for the sequence and StoneMaskLayer for
     * what is drawn afterwards.
     */
    public static final RegistrySupplier<Item> STONE_MASK =
            ITEMS.register("stone_mask", () -> new StoneMaskItem(
                    named("stone_mask", net.minecraft.ChatFormatting.DARK_RED).stacksTo(1)));

    /**
     * The other arrows.
     *
     * <p>Registered so they exist, are named, and render - nothing more. None of them carries any
     * behaviour yet: only {@link #STAND_ARROW} does anything when used, and these three do not
     * pierce, grant, or transform. They are here because the art is, and because an item that is
     * not registered cannot be tested, given out, or built on.
     *
     * <p>All three came with authored Blockbench models rather than flat sprites, so they are held
     * at an angle in the hand the way the Stand Arrow already is.
     */
    /**
     * A camera, which exists to be set down and broken.
     *
     * <p>A BlockItem now rather than a plain one, because the move works on a placed camera - you
     * stand it somewhere, point it, and break it. See CameraBlock and CameraCrushSkill.
     *
     * <p>The inventory icon is unchanged and deliberately so. {@code models/item/camera.json} is
     * still the flat sprite, which a BlockItem is perfectly happy to use; taking the default block
     * item model would have shown the animated body shrunk into a slot, which is a worse picture of
     * it than the drawing already is.
     */
    public static final RegistrySupplier<Item> CAMERA =
            ITEMS.register("camera", () -> new net.minecraft.world.item.BlockItem(
                    ModRegistries.CAMERA_BLOCK.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> HEAVENS_ARROW =
            ITEMS.register("heavens_arrow",
                    () -> new Item(named("heavens_arrow", net.minecraft.ChatFormatting.YELLOW)));
    public static final RegistrySupplier<Item> REQUIEM_ARROW =
            ITEMS.register("requiem_arrow",
                    () -> new Item(named("requiem_arrow", net.minecraft.ChatFormatting.RED)));
    public static final RegistrySupplier<Item> FRACTURED_SKIN_ARROW =
            ITEMS.register("fractured_skin_arrow",
                    () -> new org.gumel.jojoha.item.FracturedSkinArrowItem(new Item.Properties()));

    /**
     * The four daggers: throwable blades, after the trident.
     *
     * <p>Priced against the sword of the same tier and deliberately under it. A dagger swings
     * faster and hits softer, and what it buys with the difference is the throw - which is worth
     * paying for, because no sword has one. The figures below are the {@code +damage} and the
     * attack speed vanilla's own weapons are built from, so they can be read against a sword
     * directly: a sword is {@code (3, -2.4F)}.
     *
     * <pre>
     *   dagger  +1 dmg, -2.0 speed     sword   +3 dmg, -2.4 speed
     *   iron        4.0 at 2.0/s = 8.0 dps       6.0 at 1.6/s =  9.6 dps
     *   diamond     5.0 at 2.0/s = 10.0          7.0 at 1.6/s = 11.2
     *   netherite   6.0 at 2.0/s = 12.0          8.0 at 1.6/s = 12.8
     *   gold        2.0 at 2.0/s = 4.0           4.0 at 1.6/s =  6.4
     * </pre>
     *
     * <p>Every tier lands a little under its sword, which is the point - a weapon that beat the
     * sword in melee <em>and</em> threw would simply replace it.
     *
     * <p>The throw is worth a little more than a swing. It has to be aimed, it costs durability
     * whether or not it lands, and it leaves the thrower holding nothing until they walk over and
     * pick it up - see {@link DaggerItem} for why nothing brings it back on its own.
     */
    /**
     * The meteorite tier: the metal, the raw form, and the five tools.
     *
     * <p>Fire-resistant like netherite, since a tier that burns up in lava would be a downgrade
     * dressed as an upgrade. The attack speeds are vanilla's per tool - what makes these better is
     * the tier behind them, not a bespoke swing.
     */
    public static final RegistrySupplier<Item> RAW_METEORITE =
            ITEMS.register("raw_meteorite", () -> new Item(new Item.Properties().fireResistant()));

    public static final RegistrySupplier<Item> METEORITE_INGOT =
            ITEMS.register("meteorite_ingot", () -> new Item(new Item.Properties().fireResistant()));

    /**
     * The four pieces, all fire-resistant like the tools.
     *
     * <p>The material is looked up lazily inside the supplier: armour materials live in their own
     * registry and it has not been filled at the moment these fields are initialised.
     */
    public static final RegistrySupplier<Item> METEORITE_HELMET =
            ITEMS.register("meteorite_helmet", () -> new org.gumel.jojoha.item.OutfitArmorItem(
                    ModRegistries.METEORITE_ARMOR,
                    net.minecraft.world.item.ArmorItem.Type.HELMET,
                    golden("meteorite_helmet", new Item.Properties()).fireResistant()
                            .durability(net.minecraft.world.item.ArmorItem.Type.HELMET
                                    .getDurability(48))));

    public static final RegistrySupplier<Item> METEORITE_CHESTPLATE =
            ITEMS.register("meteorite_chestplate", () -> new org.gumel.jojoha.item.OutfitArmorItem(
                    ModRegistries.METEORITE_ARMOR,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                    golden("meteorite_chestplate", new Item.Properties()).fireResistant()
                            .durability(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE
                                    .getDurability(48))));

    public static final RegistrySupplier<Item> METEORITE_LEGGINGS =
            ITEMS.register("meteorite_leggings", () -> new org.gumel.jojoha.item.OutfitArmorItem(
                    ModRegistries.METEORITE_ARMOR,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS,
                    golden("meteorite_leggings", new Item.Properties()).fireResistant()
                            .durability(net.minecraft.world.item.ArmorItem.Type.LEGGINGS
                                    .getDurability(48))));

    public static final RegistrySupplier<Item> METEORITE_BOOTS =
            ITEMS.register("meteorite_boots", () -> new org.gumel.jojoha.item.OutfitArmorItem(
                    ModRegistries.METEORITE_ARMOR,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS,
                    golden("meteorite_boots", new Item.Properties()).fireResistant()
                            .durability(net.minecraft.world.item.ArmorItem.Type.BOOTS
                                    .getDurability(48))));

    public static final RegistrySupplier<Item> METEORITE_SCYTHE = ITEMS.register("meteorite_scythe",
            () -> new SwordItem(org.gumel.jojoha.item.MeteoriteTier.INSTANCE,
                    golden("meteorite_scythe", new Item.Properties()).fireResistant().attributes(
                            SwordItem.createAttributes(
                                    org.gumel.jojoha.item.MeteoriteTier.INSTANCE, 3, -2.4F))));

    public static final RegistrySupplier<Item> METEORITE_PICKAXE = ITEMS.register("meteorite_pickaxe",
            () -> new net.minecraft.world.item.PickaxeItem(
                    org.gumel.jojoha.item.MeteoriteTier.INSTANCE,
                    golden("meteorite_pickaxe", new Item.Properties()).fireResistant().attributes(
                            net.minecraft.world.item.DiggerItem.createAttributes(
                                    org.gumel.jojoha.item.MeteoriteTier.INSTANCE, 1.0F, -2.8F))));

    public static final RegistrySupplier<Item> METEORITE_AXE = ITEMS.register("meteorite_axe",
            () -> new net.minecraft.world.item.AxeItem(
                    org.gumel.jojoha.item.MeteoriteTier.INSTANCE,
                    golden("meteorite_axe", new Item.Properties()).fireResistant().attributes(
                            net.minecraft.world.item.DiggerItem.createAttributes(
                                    org.gumel.jojoha.item.MeteoriteTier.INSTANCE, 5.0F, -3.0F))));

    public static final RegistrySupplier<Item> METEORITE_SHOVEL = ITEMS.register("meteorite_shovel",
            () -> new net.minecraft.world.item.ShovelItem(
                    org.gumel.jojoha.item.MeteoriteTier.INSTANCE,
                    golden("meteorite_shovel", new Item.Properties()).fireResistant().attributes(
                            net.minecraft.world.item.DiggerItem.createAttributes(
                                    org.gumel.jojoha.item.MeteoriteTier.INSTANCE, 1.5F, -3.0F))));

    public static final RegistrySupplier<Item> METEORITE_HOE = ITEMS.register("meteorite_hoe",
            () -> new net.minecraft.world.item.HoeItem(
                    org.gumel.jojoha.item.MeteoriteTier.INSTANCE,
                    golden("meteorite_hoe", new Item.Properties()).fireResistant().attributes(
                            net.minecraft.world.item.DiggerItem.createAttributes(
                                    org.gumel.jojoha.item.MeteoriteTier.INSTANCE, -4.0F, 0.0F))));

    /**
     * Pluck.
     *
     * <p>Iron's numbers for now, through the ordinary sword frame. It is a named weapon and will
     * almost certainly want a tier and behaviour of its own - this registers the item so the sprite,
     * the name and the slot all work, and leaves the design to be filled in.
     */
    public static final RegistrySupplier<Item> PLUCK = ITEMS.register("pluck",
            () -> new SwordItem(Tiers.IRON, named("pluck", net.minecraft.ChatFormatting.AQUA)
                    .attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F))));

    /**
     * The soul cards.
     *
     * <p>Sixteen to a stack. They are consumed by the smithing table to dress armour, so they are a
     * material you gather rather than a keepsake you own one of, and a full sixty-four would let a
     * single slot hold more of a person than anyone will ever need.
     *
     * <p>Epic rather than a hand-coloured name, because a card is not just purple - it is the rarest
     * thing in the mod, and Rarity says so in the one place a colour would only imply it. They carry
     * no behaviour of their own yet.
     */
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

    private static final int DAGGER_ATTACK_BONUS = 1;
    private static final float DAGGER_ATTACK_SPEED = -2.0F;

    public static final RegistrySupplier<Item> DAGGER_WOODEN = dagger("dagger_wooden", Tiers.WOOD, 2F);
    public static final RegistrySupplier<Item> DAGGER_STONE = dagger("dagger_stone", Tiers.STONE, 4F);
    public static final RegistrySupplier<Item> DAGGER_IRON = dagger("dagger_iron", Tiers.IRON, 5F);
    public static final RegistrySupplier<Item> DAGGER_GOLD = dagger("dagger_gold", Tiers.GOLD, 3F);
    public static final RegistrySupplier<Item> DAGGER_DIAMOND = dagger("dagger_diamond", Tiers.DIAMOND, 6F);
    public static final RegistrySupplier<Item> DAGGER_NETHERITE = dagger("dagger_netherite", Tiers.NETHERITE, 7F);
    public static final RegistrySupplier<Item> DAGGER_METEORITE = dagger("dagger_meteorite",
            org.gumel.jojoha.item.MeteoriteTier.INSTANCE, 8F,
            golden("dagger_meteorite", new Item.Properties()).fireResistant());

    /**
     * Puts the item's own name in a colour.
     *
     * <p>Through ITEM_NAME rather than through Rarity, because Rarity is a closed enum of four
     * colours and most of the ones wanted here are not among them. The translation key is the item's
     * usual one, so the words still come from the language file and only the colour is added.
     *
     * <p>Where a Rarity would do - the soul cards want epic's purple and nothing more - the Rarity is
     * used instead, since that also carries the item's standing and not just its colour.
     */
    private static Item.Properties named(String name, net.minecraft.ChatFormatting colour,
                                         Item.Properties properties) {
        return properties.component(
                net.minecraft.core.component.DataComponents.ITEM_NAME,
                net.minecraft.network.chat.Component.translatable("item.jojoha." + name)
                        .withStyle(colour));
    }

    private static Item.Properties named(String name, net.minecraft.ChatFormatting colour) {
        return named(name, colour, new Item.Properties());
    }

    /** The meteorite set's gold, which is the whole set and so worth its own name. */
    private static Item.Properties golden(String name, Item.Properties properties) {
        return named(name, net.minecraft.ChatFormatting.GOLD, properties);
    }

    /** One dagger, with a sword's frame and a dagger's numbers. */
    private static RegistrySupplier<Item> dagger(String name, Tier tier, float throwDamage) {
        return dagger(name, tier, throwDamage, new Item.Properties());
    }

    /**
     * The same, for a dagger that needs something on its properties before the rest is filled in -
     * the meteorite one is fire-resistant and gold-named like everything else in that set.
     */
    private static RegistrySupplier<Item> dagger(String name, Tier tier, float throwDamage,
                                                 Item.Properties properties) {
        return ITEMS.register(name, () -> new DaggerItem(tier, throwDamage,
                properties
                        .durability(tier.getUses())
                        .attributes(SwordItem.createAttributes(tier, DAGGER_ATTACK_BONUS,
                                DAGGER_ATTACK_SPEED))));
    }

    /** Every soul card is the same item; only the sprite and the name differ. */
    private static Item soulCard() {
        return new Item(new Item.Properties().stacksTo(16)
                .rarity(net.minecraft.world.item.Rarity.EPIC));
    }

    private ModItems() {
    }

    /** No-op call site to force this class's static initializers to run before {@link ModRegistries#ITEMS} registers. */
    public static void bootstrap() {
    }
}
