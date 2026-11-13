package org.gumel.jojoha.ultimate;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public enum Adaptation {
    FIRE_VOLLEY(60, 20F, AdaptationKit::fireVolley),
    GHAST_FIREBALL(120, 30F, AdaptationKit::ghastFireball),
    DETONATE(160, 35F, AdaptationKit::detonate),
    ARROW_VOLLEY(40, 15F, player -> AdaptationKit.arrows(player, null)),
    FROST_ARROWS(50, 18F, player -> AdaptationKit.arrows(player, MobEffects.MOVEMENT_SLOWDOWN)),
    VENOM_ARROWS(50, 18F, player -> AdaptationKit.arrows(player, MobEffects.POISON)),
    WEB_SHOT(60, 15F, player -> AdaptationKit.webShot(player, false)),
    VENOM_WEB(70, 18F, player -> AdaptationKit.webShot(player, true)),
    HEX_FLASK(80, 20F, AdaptationKit::hexFlask),
    FANGS(100, 25F, AdaptationKit::fangs),
    SWOOP(60, 20F, AdaptationKit::swoop),
    WITHERING_STRIKE(80, 20F, AdaptationKit::witheringStrike),
    WITHER_SKULL(120, 30F, AdaptationKit::witherSkull),
    GUARDIAN_BEAM(80, 20F, player -> AdaptationKit.guardianBeam(player, false)),
    ELDER_BEAM(100, 25F, player -> AdaptationKit.guardianBeam(player, true)),
    SHULKER_BULLET(80, 20F, AdaptationKit::shulkerBullet),
    WIND_BURST(30, 10F, AdaptationKit::windBurst),
    SONIC_BOOM(200, 40F, AdaptationKit::sonicBoom),
    BLINK(80, 20F, AdaptationKit::blink),
    ROAR(120, 25F, AdaptationKit::roar),
    HEAVE(60, 20F, AdaptationKit::heave),
    SLAM(80, 20F, player -> AdaptationKit.slam(player, false)),
    MAGMA_SLAM(90, 22F, player -> AdaptationKit.slam(player, true)),
    SPIT(20, 5F, AdaptationKit::spit),
    SNOWBALLS(20, 5F, AdaptationKit::snowballs),
    STING(60, 15F, AdaptationKit::sting),
    SPIKES(100, 20F, AdaptationKit::spikes),
    TRIDENT(80, 20F, AdaptationKit::trident),
    LUNGE(50, 15F, player -> AdaptationKit.lunge(player, false)),
    FAMISHED_LUNGE(60, 18F, player -> AdaptationKit.lunge(player, true)),
    CLEAVE(70, 20F, player -> AdaptationKit.cleave(player, false)),
    MAUL(70, 20F, player -> AdaptationKit.cleave(player, true)),
    RAM(60, 18F, AdaptationKit::ram),
    TONGUE_LASH(50, 12F, AdaptationKit::tongueLash),
    DRAGON_BREATH(200, 40F, AdaptationKit::dragonBreath);

    private static final Map<EntityType<?>, Adaptation> BY_TYPE = new HashMap<>();
    private static final Map<Adaptation, EntityType<?>> FACE = new HashMap<>();
    private static final Map<String, Adaptation> BY_KEY = new HashMap<>();

    static {
        learn(FIRE_VOLLEY, EntityType.BLAZE);
        learn(GHAST_FIREBALL, EntityType.GHAST);
        learn(DETONATE, EntityType.CREEPER);
        learn(ARROW_VOLLEY, EntityType.SKELETON, EntityType.PILLAGER, EntityType.PIGLIN);
        learn(FROST_ARROWS, EntityType.STRAY);
        learn(VENOM_ARROWS, EntityType.BOGGED);
        learn(WEB_SHOT, EntityType.SPIDER);
        learn(VENOM_WEB, EntityType.CAVE_SPIDER);
        learn(HEX_FLASK, EntityType.WITCH);
        learn(FANGS, EntityType.EVOKER);
        learn(SWOOP, EntityType.VEX, EntityType.PHANTOM);
        learn(WITHERING_STRIKE, EntityType.WITHER_SKELETON);
        learn(WITHER_SKULL, EntityType.WITHER);
        learn(GUARDIAN_BEAM, EntityType.GUARDIAN);
        learn(ELDER_BEAM, EntityType.ELDER_GUARDIAN);
        learn(SHULKER_BULLET, EntityType.SHULKER);
        learn(WIND_BURST, EntityType.BREEZE);
        learn(SONIC_BOOM, EntityType.WARDEN);
        learn(BLINK, EntityType.ENDERMAN, EntityType.ENDERMITE);
        learn(ROAR, EntityType.RAVAGER);
        learn(HEAVE, EntityType.IRON_GOLEM, EntityType.HOGLIN, EntityType.ZOGLIN);
        learn(SLAM, EntityType.SLIME);
        learn(MAGMA_SLAM, EntityType.MAGMA_CUBE);
        learn(SPIT, EntityType.LLAMA, EntityType.TRADER_LLAMA);
        learn(SNOWBALLS, EntityType.SNOW_GOLEM);
        learn(STING, EntityType.BEE);
        learn(SPIKES, EntityType.PUFFERFISH);
        learn(TRIDENT, EntityType.DROWNED);
        learn(LUNGE, EntityType.ZOMBIE, EntityType.ZOMBIE_VILLAGER, EntityType.ZOMBIFIED_PIGLIN,
                EntityType.WOLF, EntityType.FOX, EntityType.CAT, EntityType.OCELOT, EntityType.SILVERFISH);
        learn(FAMISHED_LUNGE, EntityType.HUSK);
        learn(CLEAVE, EntityType.VINDICATOR, EntityType.PIGLIN_BRUTE);
        learn(MAUL, EntityType.POLAR_BEAR);
        learn(RAM, EntityType.GOAT);
        learn(TONGUE_LASH, EntityType.FROG);
        learn(DRAGON_BREATH, EntityType.ENDER_DRAGON);
    }

    private final int cooldownTicks;
    private final float energyCost;
    private final Consumer<ServerPlayer> use;

    Adaptation(int cooldownTicks, float energyCost, Consumer<ServerPlayer> use) {
        this.cooldownTicks = cooldownTicks;
        this.energyCost = energyCost;
        this.use = use;
    }

    private static void learn(Adaptation adaptation, EntityType<?>... types) {
        FACE.putIfAbsent(adaptation, types[0]);
        BY_KEY.put(adaptation.key(), adaptation);
        for (EntityType<?> type : types) {
            BY_TYPE.put(type, adaptation);
        }
    }

    public static Adaptation of(EntityType<?> type) {
        return BY_TYPE.get(type);
    }

    public static Adaptation named(String key) {
        return key == null ? null : BY_KEY.get(key);
    }

    public EntityType<?> face() {
        return FACE.get(this);
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String passiveKey() {
        return translationKey() + ".passive";
    }

    public static Component creatureName(ResourceLocation typeId) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).map(EntityType::getDescription)
                .orElse(Component.literal(typeId.toString()));
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public float energyCost() {
        return energyCost;
    }

    public String translationKey() {
        return "adaptation.jojoha." + name().toLowerCase(Locale.ROOT);
    }

    public String unavailable(ServerPlayer player) {
        return AdaptationKit.needsTarget(this) && AdaptationKit.aimed(player, AdaptationKit.reachOf(this)) == null
                ? "message.jojoha.adapt.no_target" : null;
    }

    public void use(ServerPlayer player) {
        use.accept(player);
    }
}
