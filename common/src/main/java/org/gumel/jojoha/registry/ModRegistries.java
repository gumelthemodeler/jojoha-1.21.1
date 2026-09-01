package org.gumel.jojoha.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.StandEntity;

public final class ModRegistries {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.ITEM);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.CREATIVE_MODE_TAB);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.ENTITY_TYPE);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.PARTICLE_TYPE);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.SOUND_EVENT);
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.MOB_EFFECT);
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.BLOCK);
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>>
            RECIPE_SERIALIZERS = DeferredRegister.create(Jojoha.MOD_ID, Registries.RECIPE_SERIALIZER);

    /**
     * The recipe that dresses armour in an outfit.
     *
     * <p>A special recipe: it carries no ingredients of its own in JSON, because what it matches is
     * a relationship between two stacks rather than a fixed shape. The JSON is one line naming this
     * serializer, and everything else is in OutfitSmithingRecipe.
     */
    public static final RegistrySupplier<net.minecraft.world.item.crafting.RecipeSerializer<
            org.gumel.jojoha.item.OutfitSmithingRecipe>> OUTFIT_SMITHING =
            RECIPE_SERIALIZERS.register("apply_outfit",
                    org.gumel.jojoha.item.OutfitSmithingRecipe.Serializer::new);

    public static final DeferredRegister<net.minecraft.world.item.ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.ARMOR_MATERIAL);

    /**
     * Meteorite plate: a step past netherite, on the same terms the tools are.
     *
     * <p>Netherite protects 3/6/8/3 with toughness 3 and a tenth of knockback resistance. This adds
     * one to the head, body and feet and two to the legs, and lifts toughness and knockback a little
     * - enough to be worth the climb, not enough to make the climb pointless. Enchantability matches
     * the tier's 18.
     *
     * <p>The layer list is empty on purpose. Layers are the flat two-sheet armour skin, and this
     * armour is not drawn that way - it is a model, drawn by MeteoriteArmorLayer. Naming a layer
     * here would ask the game to paint a sheet that does not exist over the top of it.
     */
    public static final RegistrySupplier<net.minecraft.world.item.ArmorMaterial> METEORITE_ARMOR =
            ARMOR_MATERIALS.register("meteorite", () -> new net.minecraft.world.item.ArmorMaterial(
                    java.util.Map.of(
                            net.minecraft.world.item.ArmorItem.Type.HELMET, 4,
                            net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, 9,
                            net.minecraft.world.item.ArmorItem.Type.LEGGINGS, 7,
                            net.minecraft.world.item.ArmorItem.Type.BOOTS, 4,
                            net.minecraft.world.item.ArmorItem.Type.BODY, 11),
                    18,
                    net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> net.minecraft.world.item.crafting.Ingredient.of(
                            ModItems.METEORITE_INGOT.get()),
                    java.util.List.of(),
                    4.0F,
                    0.15F));

    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.FEATURE);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.placement
            .PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.PLACEMENT_MODIFIER_TYPE);

    /**
     * Spacing for anything that must not crowd itself.
     *
     * <p>PlacementModifierType has exactly one method, so the type is just its codec - hence a
     * lambda returning a lambda: the outer one is what the registry asks for, the inner one is the
     * type itself.
     */
    public static final RegistrySupplier<net.minecraft.world.level.levelgen.placement
            .PlacementModifierType<org.gumel.jojoha.level.placement.SpacedGridPlacement>>
            SPACED_GRID = PLACEMENT_MODIFIERS.register("spaced_grid",
                    () -> () -> org.gumel.jojoha.level.placement.SpacedGridPlacement.CODEC);

    /**
     * The pass that gives generated turf its slope connections.
     *
     * <p>A worldgen feature is an ordinary registry entry, and this one is only the code half - the
     * JSON under {@code worldgen/configured_feature} is what hands it to a biome. It lives here
     * rather than somewhere more worldgen-flavoured so that every {@code DeferredRegister} the mod
     * owns stays in the one file, which is the whole point of this class.
     */
    public static final RegistrySupplier<net.minecraft.world.level.levelgen.feature.Feature<
            net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>>
            PHANTOM_GRASS_CONNECT = FEATURES.register("phantom_grass_connect", () ->
                    new org.gumel.jojoha.level.feature.PhantomGrassConnectFeature(
                            net.minecraft.world.level.levelgen.feature.configurations
                                    .NoneFeatureConfiguration.CODEC));


    /**
     * The camera, set down and waiting to be broken.
     *
     * <p>Glass rather than stone for the sound and the break time: it is a lens and a body, it comes
     * apart in one hit, and the noise it makes going is half of what sells the move.
     */
    public static final RegistrySupplier<net.minecraft.world.level.block.Block> CAMERA_BLOCK =
            BLOCKS.register("camera", () -> new org.gumel.jojoha.block.CameraBlock(
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                            .strength(0.4F)
                            .sound(net.minecraft.world.level.block.SoundType.GLASS)
                            .noOcclusion()));

    public static final RegistrySupplier<net.minecraft.world.level.block.entity.BlockEntityType<org.gumel.jojoha.block.CameraBlockEntity>> CAMERA_BLOCK_ENTITY =
            BLOCK_ENTITY_TYPES.register("camera", () ->
                    net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(
                            org.gumel.jojoha.block.CameraBlockEntity::new,
                            CAMERA_BLOCK.get()).build(null));

    /**
     * The spent mask, falling.
     *
     * <p>Not saved with the world: it exists for the second or two between coming off a face and
     * breaking on the floor, and a mask found still falling after a reload would be a mask that
     * never finished doing the one thing it exists to do.
     */
    public static final RegistrySupplier<EntityType<org.gumel.jojoha.item.FallingMask>> FALLING_MASK =
            ENTITY_TYPES.register("falling_mask", () ->
                    EntityType.Builder.<org.gumel.jojoha.item.FallingMask>of(
                                    org.gumel.jojoha.item.FallingMask::new, MobCategory.MISC)
                            .sized(0.6F, 0.5F)
                            .noSave()
                            .clientTrackingRange(6)
                            .updateInterval(1)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "falling_mask").toString()));

    /**
     * A dagger in flight.
     *
     * <p>Saved with the world, unlike the Stand: a thrown dagger that vanished on reload would be
     * the player's weapon quietly deleted, and it may sit in a wall for a long time before anybody
     * comes back for it. Tracked further than the Stand because it travels, and updated often
     * because it travels fast.
     */
    public static final RegistrySupplier<EntityType<org.gumel.jojoha.item.ThrownDagger>> THROWN_DAGGER =
            ENTITY_TYPES.register("thrown_dagger", () ->
                    EntityType.Builder.<org.gumel.jojoha.item.ThrownDagger>of(
                                    org.gumel.jojoha.item.ThrownDagger::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "thrown_dagger").toString()));

    /**
     * Hermit Purple's vine, and the point it anchors to.
     *
     * <p>Tracked far and updated often, unlike the dagger. A thrown weapon only has to look roughly
     * right on its way past; this one has a rope drawn to it every frame from a player who is
     * hanging off it, so a stale position is a rope that visibly lags behind its own anchor.
     */
    public static final RegistrySupplier<EntityType<org.gumel.jojoha.stand.grapple.HermitGrappleHook>>
            GRAPPLE_HOOK = ENTITY_TYPES.register("hermit_grapple_hook", () ->
                    EntityType.Builder.<org.gumel.jojoha.stand.grapple.HermitGrappleHook>of(
                                    org.gumel.jojoha.stand.grapple.HermitGrappleHook::new,
                                    MobCategory.MISC)
                            .sized(0.3F, 0.3F)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build(ResourceLocation.fromNamespaceAndPath(
                                    Jojoha.MOD_ID, "hermit_grapple_hook").toString()));

    // No AI (LivingEntity, not Mob), never written to the world save - summoned fresh each time.
    public static final RegistrySupplier<EntityType<StandEntity>> STAND = ENTITY_TYPES.register("stand", () ->
            EntityType.Builder.of(StandEntity::new, MobCategory.MISC)
                    .sized(0.8F, 1.9F)
                    .noSave()
                    .clientTrackingRange(10)
                    // Every tick, against a default of three. A Stand is the only entity in the
                    // game a player can put their camera inside, and a camera whose carrier reports
                    // its position twice a second is a camera that stutters however smoothly the
                    // thing is actually moving - the client is interpolating across a 150ms gap and
                    // there is nothing in between to interpolate with.
                    .updateInterval(1)
                    .build(ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "stand").toString()));

    // The anonymous subclass is the standard way to reach SimpleParticleType's protected
    // constructor from outside its package - see any vanilla ParticleTypes.register call.
    // overrideLimiter=true so this gameplay-relevant effect isn't silently thinned out by a
    // player's Particles graphics setting.
    public static final RegistrySupplier<SimpleParticleType> STAND_AURA =
            PARTICLE_TYPES.register("stand_aura", () -> new SimpleParticleType(true) {
            });

    /**
     * Motes gathering while a time stop is wound up - see TimeStopMoteParticle.
     *
     * <p>Anonymous subclass for the same reason as the rest: SimpleParticleType's constructor is
     * protected, and a subclass is the shortest way past that without an access widener.
     */
    public static final RegistrySupplier<SimpleParticleType> TIMESTOP_MOTE =
            PARTICLE_TYPES.register("timestop_mote", () -> new SimpleParticleType(true) {
            });

    /** The upward spiral thrown off during the awakening ritual - see StandTransformParticle. */
    public static final RegistrySupplier<SimpleParticleType> STAND_TRANSFORM =
            PARTICLE_TYPES.register("stand_transform", () -> new SimpleParticleType(true) {
            });

    /** The ring of marks over the head of anything that has been stunned - see StunParticle. */
    public static final RegistrySupplier<SimpleParticleType> STUN =
            PARTICLE_TYPES.register("stun", () -> new SimpleParticleType(true) {
            });

    /** Thorns caught on anything Hermit Purple has tangled - see TangledThornParticle. */
    public static final RegistrySupplier<SimpleParticleType> TANGLED_THORN =
            PARTICLE_TYPES.register("tangled_thorn", () -> new SimpleParticleType(true) {
            });

    /** The ring that snaps outward where a blow connects - see ImpactRingParticle. */
    public static final RegistrySupplier<SimpleParticleType> IMPACT_RING =
            PARTICLE_TYPES.register("impact_ring", () -> new SimpleParticleType(true) {
            });

    /** The stream of air Inhale drags or drives along - see InhaleWindParticle. */
    public static final RegistrySupplier<SimpleParticleType> INHALE_WIND =
            PARTICLE_TYPES.register("inhale_wind", () -> new SimpleParticleType(true) {
            });

    /** Smoke caught in that stream - see InhaleSmokeParticle. */
    public static final RegistrySupplier<SimpleParticleType> INHALE_SMOKE =
            PARTICLE_TYPES.register("inhale_smoke", () -> new SimpleParticleType(true) {
            });

    /** The guard coming apart, pane and shards both - see GuardBreakParticle. */
    public static final RegistrySupplier<SimpleParticleType> GUARD_BREAK =
            PARTICLE_TYPES.register("guard_break", () -> new SimpleParticleType(true) {
            });

    /** The drifting red motes that rise while the mask is turning - see AmbientMoteParticle. */
    public static final RegistrySupplier<SimpleParticleType> BLOOD_MOTE =
            PARTICLE_TYPES.register("blood_mote", () -> new SimpleParticleType(true) {
            });

    /**
     * The same burst, in red, for the mask.
     *
     * <p>A second type rather than a tint passed at spawn, because the particle already spends all
     * three velocity slots on its direction and has nothing left to carry a colour in. Both share
     * one sprite sheet and one particle class; only the tint differs.
     */
    public static final RegistrySupplier<SimpleParticleType> STAND_AWAKEN_RED =
            PARTICLE_TYPES.register("stand_awaken_red", () -> new SimpleParticleType(true) {
            });

    /**
     * The pair a Stand sheds while its skin is being rewritten - see StandAwakenParticle.
     *
     * <p>Separate types for the same reason the red one is separate: the particle spends all three
     * of its velocity slots on direction and has nothing left to carry a colour in.
     */
    public static final RegistrySupplier<SimpleParticleType> STAND_AWAKEN_BLUE =
            PARTICLE_TYPES.register("stand_awaken_blue", () -> new SimpleParticleType(true) {
            });

    public static final RegistrySupplier<SimpleParticleType> STAND_AWAKEN_PINK =
            PARTICLE_TYPES.register("stand_awaken_pink", () -> new SimpleParticleType(true) {
            });

    /** The outward burst at the moment a Stand awakens - see StandAwakenParticle. */
    public static final RegistrySupplier<SimpleParticleType> STAND_AWAKEN =
            PARTICLE_TYPES.register("stand_awaken", () -> new SimpleParticleType(true) {
            });

    /**
     * Three tabs: what you build with, what you carry, and everything else.
     *
     * <p>The set outgrew a single shelf, and the split follows what a player is doing when they
     * reach for something rather than what the thing technically is. Blocks are one shelf because
     * that is how you shop for them - by eye, in bulk, while building. Equipment is another, because
     * you go there to arm yourself and it is unhelpful to have a dagger four screens from the armour
     * that goes with it. Everything left is materials and oddments.
     *
     * <p>Within the blocks tab the natural ones come first and the worked ones after, which keeps
     * turf next to turf and planks next to stairs without needing two tabs to say so.
     *
     * <p>Each list is driven off {@code ModBlocks} rather than named again here, so a block added
     * there cannot be left out by forgetting a line in this file.
     */
    public static final RegistrySupplier<CreativeModeTab> BLOCKS_TAB =
            CREATIVE_TABS.register("blocks", () -> CreativeTabRegistry.create(builder -> {
                builder.title(Component.translatable("itemGroup.jojoha.blocks"));
                builder.icon(() -> new ItemStack(ModBlocks.PHANTOM_GRASS_BLOCK.get()));
                builder.displayItems((params, output) -> {
                    for (var block : ModBlocks.natural()) {
                        output.accept(block.get());
                    }
                    for (var block : ModBlocks.building()) {
                        output.accept(block.get());
                    }
                });
            }));

    /**
     * Weapons, tools and armour - anything you equip.
     *
     * <p>The tools sit here rather than with the oddments because a pickaxe is something you hold,
     * not something you keep in a chest, and separating them from the armour of the same metal would
     * scatter one set across two tabs.
     */
    public static final RegistrySupplier<CreativeModeTab> EQUIPMENT_TAB =
            CREATIVE_TABS.register("equipment", () -> CreativeTabRegistry.create(builder -> {
                builder.title(Component.translatable("itemGroup.jojoha.equipment"));
                builder.icon(() -> new ItemStack(ModItems.METEORITE_SCYTHE.get()));
                builder.displayItems((params, output) -> {
                    // Daggers in tier order, which is the order you get them in.
                    output.accept(ModItems.DAGGER_WOODEN.get());
                    output.accept(ModItems.DAGGER_STONE.get());
                    output.accept(ModItems.DAGGER_IRON.get());
                    output.accept(ModItems.DAGGER_GOLD.get());
                    output.accept(ModItems.DAGGER_DIAMOND.get());
                    output.accept(ModItems.DAGGER_NETHERITE.get());
                    output.accept(ModItems.DAGGER_METEORITE.get());

                    output.accept(ModItems.PLUCK.get());

                    output.accept(ModItems.METEORITE_SCYTHE.get());
                    output.accept(ModItems.METEORITE_PICKAXE.get());
                    output.accept(ModItems.METEORITE_AXE.get());
                    output.accept(ModItems.METEORITE_SHOVEL.get());
                    output.accept(ModItems.METEORITE_HOE.get());

                    output.accept(ModItems.METEORITE_HELMET.get());
                    output.accept(ModItems.METEORITE_CHESTPLATE.get());
                    output.accept(ModItems.METEORITE_LEGGINGS.get());
                    output.accept(ModItems.METEORITE_BOOTS.get());
                });
            }));

    /** The arrows, the masks, the materials - what is left once blocks and equipment are out. */
    public static final RegistrySupplier<CreativeModeTab> ITEMS_TAB =
            CREATIVE_TABS.register("items", () -> CreativeTabRegistry.create(builder -> {
                builder.title(Component.translatable("itemGroup.jojoha.items"));
                builder.icon(() -> new ItemStack(ModItems.STAND_ARROW.get()));
                builder.displayItems((params, output) -> {
                    output.accept(ModItems.STAND_ARROW.get());
                    output.accept(ModItems.STAND_ARROW_SHARD.get());
                    output.accept(ModItems.HEAVENS_ARROW.get());
                    output.accept(ModItems.REQUIEM_ARROW.get());
                    output.accept(ModItems.FRACTURED_SKIN_ARROW.get());
                    output.accept(ModItems.STONE_MASK.get());
                    // A block, but it belongs with the things you use rather than the things you
                    // build with - it is a tool that happens to be placed.
                    output.accept(ModItems.CAMERA.get());
                    output.accept(ModItems.RAW_METEORITE.get());
                    output.accept(ModItems.METEORITE_INGOT.get());

                    output.accept(ModItems.SOUL_CARD_JOTARO.get());
                    output.accept(ModItems.SOUL_CARD_KAKYOIN.get());
                    output.accept(ModItems.SOUL_CARD_POLNAREFF.get());
                    output.accept(ModItems.SOUL_CARD_AVDOL.get());
                    output.accept(ModItems.SOUL_CARD_JOSEPH.get());
                    output.accept(ModItems.SOUL_CARD_DIO.get());
                });
            }));

    private ModRegistries() {
    }

    public static void init() {
        ModBlocks.bootstrap();
        ModComponents.bootstrap();
        ModItems.bootstrap();
        ModTraits.bootstrap();
        ModSounds.bootstrap();
        ModEffects.bootstrap();
        // Before the items, and it has to be: the camera item is a BlockItem, so building it asks
        // for the block instance - which does not exist until this line has run.
        BLOCKS.register();
        BLOCK_ENTITY_TYPES.register();
        ARMOR_MATERIALS.register();
        RECIPE_SERIALIZERS.register();
        ModComponents.COMPONENTS.register();
        FEATURES.register();
        PLACEMENT_MODIFIERS.register();
        ITEMS.register();
        CREATIVE_TABS.register();
        ENTITY_TYPES.register();
        PARTICLE_TYPES.register();
        SOUND_EVENTS.register();
        MOB_EFFECTS.register();
        EntityAttributeRegistry.register(STAND, StandEntity::createAttributes);
    }
}
