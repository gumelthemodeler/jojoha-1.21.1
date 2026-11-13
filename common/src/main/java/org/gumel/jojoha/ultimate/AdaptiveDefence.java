package org.gumel.jojoha.ultimate;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AdaptiveDefence {
    private static final float[] RESIST = {0F, 0.25F, 0.40F, 0.55F};
    private static final float[] LIGHT_RESIST = {0F, 0.10F, 0.15F, 0.20F};
    private static final float[] CHANCE = {0F, 0.20F, 0.30F, 0.40F};
    private static final float[] THORNS = {0F, 1.0F, 1.5F, 2.0F};
    private static final int[] AFFLICT_TICKS = {0, 40, 60, 80};
    private static final float[] SHELL_ARMOR = {0F, 4F, 6F, 8F};

    private static final float HEALTH_PER_RANK = 1F;
    private static final float ARMOR_PER_RANK = 0.5F;
    private static final float ATTACK_PER_RANK = 0.25F;

    private static final float LEAST_KEPT = 0.45F;

    private static final int REACTIVE_MAX_STACKS = 5;
    private static final float REACTIVE_PER_STACK = 0.03F;
    private static final float REACTIVE_PER_STACK_PER_RANK = 0.0035F;
    private static final int REACTIVE_FADES_AFTER = 160;

    private static final int BLINK_COOLDOWN = 200;
    private static final int LAST_DITCH_COOLDOWN = 600;
    private static final float LAST_DITCH_AT = 0.3F;
    private static final int UPDATE_EVERY = 10;

    private static final ResourceLocation EVOLUTION_HEALTH = id("evolution_health");
    private static final ResourceLocation EVOLUTION_ARMOR = id("evolution_armor");
    private static final ResourceLocation EVOLUTION_ATTACK = id("evolution_attack");
    private static final ResourceLocation ADAPT_HEALTH = id("adapt_health");
    private static final ResourceLocation ADAPT_ARMOR = id("adapt_armor");
    private static final ResourceLocation ADAPT_TOUGHNESS = id("adapt_toughness");
    private static final ResourceLocation ADAPT_KNOCKBACK = id("adapt_knockback");
    private static final ResourceLocation ADAPT_FALL = id("adapt_fall");
    private static final ResourceLocation ADAPT_OXYGEN = id("adapt_oxygen");
    private static final ResourceLocation ADAPT_SHELL = id("adapt_shell");

    private static final List<Mod> MODS = List.of(
            new Mod(Attributes.MAX_HEALTH, EVOLUTION_HEALTH), new Mod(Attributes.ARMOR, EVOLUTION_ARMOR),
            new Mod(Attributes.ATTACK_DAMAGE, EVOLUTION_ATTACK), new Mod(Attributes.MAX_HEALTH, ADAPT_HEALTH),
            new Mod(Attributes.ARMOR, ADAPT_ARMOR), new Mod(Attributes.ARMOR_TOUGHNESS, ADAPT_TOUGHNESS),
            new Mod(Attributes.KNOCKBACK_RESISTANCE, ADAPT_KNOCKBACK), new Mod(Attributes.SAFE_FALL_DISTANCE, ADAPT_FALL),
            new Mod(Attributes.OXYGEN_BONUS, ADAPT_OXYGEN), new Mod(Attributes.ARMOR, ADAPT_SHELL));

    private record Mod(Holder<Attribute> attribute, ResourceLocation id) {
    }

    private static final class Stack {
        int stacks;
        long lastAt;
    }

    private static final Map<UUID, Map<String, Stack>> REACTIVE = new HashMap<>();
    private static final Map<UUID, Long> BLINKED_AT = new HashMap<>();
    private static final Map<UUID, Long> LAST_DITCH_AT_TIME = new HashMap<>();

    private AdaptiveDefence() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }

    private static Map<Adaptation, Integer> worn(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        return AdaptationLibrary.ultimate(data) ? AdaptationLibrary.passives(data) : Map.of();
    }

    private static int level(Map<Adaptation, Integer> worn, Adaptation... any) {
        int best = 0;
        for (Adaptation adaptation : any) {
            best = Math.max(best, worn.getOrDefault(adaptation, 0));
        }
        return best;
    }

    private static boolean melee(DamageSource source, ServerPlayer victim) {
        Entity direct = source.getDirectEntity();
        return direct instanceof LivingEntity && direct == source.getEntity() && direct != victim
                && !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypeTags.IS_EXPLOSION);
    }

    public static float incoming(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof ServerPlayer player) || amount <= 0F
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return amount;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!AdaptationLibrary.ultimate(data)) {
            return amount;
        }
        Map<Adaptation, Integer> worn = AdaptationLibrary.passives(data);
        float kept = 1F;
        if (source.is(DamageTypeTags.IS_FIRE)) {
            kept *= 1F - Math.max(RESIST[level(worn, Adaptation.FIRE_VOLLEY)],
                    LIGHT_RESIST[level(worn, Adaptation.MAGMA_SLAM)] * 2.5F);
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            kept *= 1F - RESIST[level(worn, Adaptation.GHAST_FIREBALL)];
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            kept *= 1F - RESIST[level(worn, Adaptation.ARROW_VOLLEY)];
        }
        if (source.is(DamageTypeTags.IS_FREEZING)) {
            kept *= 1F - RESIST[level(worn, Adaptation.FROST_ARROWS, Adaptation.SNOWBALLS)];
        }
        if (source.is(DamageTypeTags.IS_FALL)) {
            kept *= 1F - RESIST[level(worn, Adaptation.SWOOP, Adaptation.SLAM)];
        }
        if (source.is(DamageTypeTags.IS_DROWNING)) {
            kept *= 1F - RESIST[level(worn, Adaptation.TRIDENT)];
        }
        if (source.is(DamageTypeTags.WITCH_RESISTANT_TO)) {
            kept *= 1F - RESIST[level(worn, Adaptation.HEX_FLASK, Adaptation.DRAGON_BREATH)];
        }
        if (melee(source, player)) {
            kept *= 1F - LIGHT_RESIST[level(worn, Adaptation.CLEAVE, Adaptation.MAUL, Adaptation.ROAR)];
        }
        int wither = level(worn, Adaptation.WITHER_SKULL);
        if (wither > 0 && player.getHealth() <= player.getMaxHealth() * 0.5F) {
            kept *= 1F - LIGHT_RESIST[wither] * 1.75F;
        }
        kept *= 1F - reactive(player, data, source);
        return amount * Math.max(LEAST_KEPT, kept);
    }

    private static String kindOf(DamageSource source) {
        String type = source.typeHolder().unwrapKey().map(key -> key.location().toString()).orElse("unknown");
        Entity direct = source.getDirectEntity();
        return direct == null ? type : type + "/" + BuiltInRegistries.ENTITY_TYPE.getKey(direct.getType());
    }

    private static float reactive(ServerPlayer player, JojohaPlayerData data, DamageSource source) {
        Map<String, Stack> stacks = REACTIVE.get(player.getUUID());
        if (stacks == null) {
            return 0F;
        }
        Stack stack = stacks.get(kindOf(source));
        if (stack == null || player.level().getGameTime() - stack.lastAt > REACTIVE_FADES_AFTER) {
            return 0F;
        }
        float per = REACTIVE_PER_STACK + REACTIVE_PER_STACK_PER_RANK * AdaptationLibrary.rank(data);
        return Math.min(REACTIVE_MAX_STACKS, stack.stacks) * per;
    }

    public static boolean refuses(LivingEntity victim, DamageSource source) {
        if (!(victim instanceof ServerPlayer player) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        Map<Adaptation, Integer> worn = worn(player);
        if (worn.isEmpty()) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        int deflect = level(worn, Adaptation.WIND_BURST);
        if (deflect > 0 && source.getDirectEntity() instanceof Projectile projectile
                && player.getRandom().nextFloat() < CHANCE[deflect]) {
            projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-0.6));
            projectile.setOwner(player);
            projectile.hurtMarked = true;
            level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_DEFLECT, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.sendParticles(ParticleTypes.GUST, projectile.getX(), projectile.getY(), projectile.getZ(), 1, 0, 0, 0, 0);
            return true;
        }
        int blink = level(worn, Adaptation.BLINK);
        if (blink > 0 && source.getEntity() instanceof LivingEntity && source.getEntity() != player
                && ready(BLINKED_AT, player, BLINK_COOLDOWN) && player.getRandom().nextFloat() < CHANCE[blink] * 0.75F) {
            Vec3 before = player.position();
            for (int i = 0; i < 16; i++) {
                double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 12.0;
                double y = player.getY() + player.getRandom().nextInt(5) - 2;
                double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 12.0;
                if (player.randomTeleport(x, y, z, true)) {
                    BLINKED_AT.put(player.getUUID(), level.getGameTime());
                    player.fallDistance = 0F;
                    level.sendParticles(ParticleTypes.PORTAL, before.x, before.y + 1.0, before.z, 30, 0.3, 0.6, 0.3, 0.3);
                    level.playSound(null, before.x, before.y, before.z, SoundEvents.ENDERMAN_TELEPORT,
                            SoundSource.PLAYERS, 1.0F, 1.0F);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean ready(Map<UUID, Long> at, ServerPlayer player, int cooldown) {
        Long last = at.get(player.getUUID());
        return last == null || player.level().getGameTime() - last >= cooldown;
    }

    public static void struck(ServerPlayer player, DamageSource source, float lost) {
        if (lost <= 0F) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(player);
        if (!AdaptationLibrary.ultimate(data)) {
            return;
        }
        stack(player, source);
        Map<Adaptation, Integer> worn = AdaptationLibrary.passives(data);
        if (worn.isEmpty()) {
            return;
        }
        if (melee(source, player) && source.getEntity() instanceof LivingEntity attacker) {
            retaliate(player, attacker, worn);
        }
        if (player.getHealth() <= player.getMaxHealth() * LAST_DITCH_AT && player.isAlive()
                && ready(LAST_DITCH_AT_TIME, player, LAST_DITCH_COOLDOWN)) {
            int fangs = level(worn, Adaptation.FANGS);
            int burst = level(worn, Adaptation.DETONATE);
            if (fangs > 0 || burst > 0) {
                LAST_DITCH_AT_TIME.put(player.getUUID(), player.level().getGameTime());
                if (fangs > 0) {
                    fangRing(player, fangs);
                }
                if (burst > 0) {
                    burst(player, burst);
                }
            }
        }
    }

    private static void stack(ServerPlayer player, DamageSource source) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        long now = player.level().getGameTime();
        Map<String, Stack> stacks = REACTIVE.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        stacks.values().removeIf(stack -> now - stack.lastAt > REACTIVE_FADES_AFTER);
        Stack stack = stacks.computeIfAbsent(kindOf(source), kind -> new Stack());
        stack.lastAt = now;
        if (stack.stacks >= REACTIVE_MAX_STACKS) {
            return;
        }
        stack.stacks++;
        if (stack.stacks >= 2) {
            ServerLevel level = player.serverLevel();
            level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1.0, player.getZ(),
                    4 + stack.stacks * 2, 0.35, 0.5, 0.35, 0.1);
            level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.PLAYERS,
                    0.5F, 0.8F + stack.stacks * 0.12F);
        }
    }

    private static void retaliate(ServerPlayer player, LivingEntity attacker, Map<Adaptation, Integer> worn) {
        int poison = level(worn, Adaptation.VENOM_ARROWS, Adaptation.STING, Adaptation.SPIKES, Adaptation.VENOM_WEB);
        if (poison > 0) {
            afflict(attacker, MobEffects.POISON, AFFLICT_TICKS[poison], 0);
        }
        int web = level(worn, Adaptation.WEB_SHOT, Adaptation.VENOM_WEB);
        if (web > 0) {
            afflict(attacker, MobEffects.MOVEMENT_SLOWDOWN, AFFLICT_TICKS[web], 1);
        }
        int wither = level(worn, Adaptation.WITHERING_STRIKE);
        if (wither > 0) {
            afflict(attacker, MobEffects.WITHER, AFFLICT_TICKS[wither], 0);
        }
        int spit = level(worn, Adaptation.SPIT);
        if (spit > 0) {
            afflict(attacker, MobEffects.WEAKNESS, AFFLICT_TICKS[spit], 0);
        }
        int elder = level(worn, Adaptation.ELDER_BEAM);
        if (elder > 0) {
            afflict(attacker, MobEffects.DIG_SLOWDOWN, AFFLICT_TICKS[elder] * 2, 1);
        }
        float thorns = Math.max(THORNS[level(worn, Adaptation.GUARDIAN_BEAM, Adaptation.SPIKES)],
                THORNS[elder] + (elder > 0 ? 0.5F : 0F));
        if (thorns > 0F) {
            attacker.hurt(player.damageSources().thorns(player),
                    DamageScale.vampire(player, attacker, thorns));
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.THORNS_HIT, SoundSource.PLAYERS,
                    0.7F, 1.0F);
        }
    }

    private static void afflict(LivingEntity target, Holder<MobEffect> effect, int ticks, int amplifier) {
        target.addEffect(new MobEffectInstance(effect, ticks, amplifier));
    }

    private static void fangRing(ServerPlayer player, int mastery) {
        ServerLevel level = player.serverLevel();
        int count = 6 + mastery * 2;
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count;
            double x = player.getX() + Math.cos(angle) * 1.6;
            double z = player.getZ() + Math.sin(angle) * 1.6;
            Double floor = AdaptationKit.floor(level, x, player.getY(), z);
            if (floor != null) {
                level.addFreshEntity(new EvokerFangs(level, x, floor, z, (float) angle, 0, player));
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.PLAYERS,
                1.0F, 1.0F);
    }

    private static void burst(ServerPlayer player, int mastery) {
        ServerLevel level = player.serverLevel();
        double radius = 3.0 + mastery * 0.5;
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive())) {
            Vec3 away = near.position().subtract(player.position());
            near.knockback(0.8 + mastery * 0.3, -away.x, -away.z);
            near.hurt(player.damageSources().explosion(player, player),
                    DamageScale.vampire(player, near, 2.0F + mastery));
            near.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 0.8, player.getZ(),
                1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                1.0F, 1.2F);
    }

    public static void tick(ServerPlayer player, JojohaPlayerData data) {
        Map<Adaptation, Integer> worn = AdaptationLibrary.passives(data);
        set(player, Attributes.ARMOR, ADAPT_SHELL,
                player.isCrouching() ? SHELL_ARMOR[level(worn, Adaptation.SHULKER_BULLET)] : 0F);
        if (player.tickCount % UPDATE_EVERY != 0) {
            return;
        }
        int rank = AdaptationLibrary.rank(data);
        set(player, Attributes.MAX_HEALTH, EVOLUTION_HEALTH, rank * HEALTH_PER_RANK);
        set(player, Attributes.ARMOR, EVOLUTION_ARMOR, rank * ARMOR_PER_RANK);
        set(player, Attributes.ATTACK_DAMAGE, EVOLUTION_ATTACK, rank * ATTACK_PER_RANK);

        int lunge = level(worn, Adaptation.LUNGE);
        set(player, Attributes.MAX_HEALTH, ADAPT_HEALTH, lunge == 0 ? 0F : 1F + lunge);
        int heave = level(worn, Adaptation.HEAVE);
        set(player, Attributes.ARMOR, ADAPT_ARMOR, heave == 0 ? 0F : 1F + heave);
        int husk = level(worn, Adaptation.FAMISHED_LUNGE);
        set(player, Attributes.ARMOR_TOUGHNESS, ADAPT_TOUGHNESS, husk);
        int knock = Math.max(level(worn, Adaptation.SONIC_BOOM, Adaptation.RAM), level(worn, Adaptation.ROAR) - 1);
        set(player, Attributes.KNOCKBACK_RESISTANCE, ADAPT_KNOCKBACK, knock == 0 ? 0F : 0.1F + knock * 0.15F);
        int fall = level(worn, Adaptation.TONGUE_LASH);
        set(player, Attributes.SAFE_FALL_DISTANCE, ADAPT_FALL, fall == 0 ? 0F : 1F + fall);
        int gills = level(worn, Adaptation.TRIDENT);
        set(player, Attributes.OXYGEN_BONUS, ADAPT_OXYGEN, gills * 1.5F);
        if (level(worn, Adaptation.SONIC_BOOM) > 0) {
            player.removeEffect(MobEffects.DARKNESS);
        }

        long now = player.level().getGameTime();
        Map<String, Stack> stacks = REACTIVE.get(player.getUUID());
        if (stacks != null) {
            Iterator<Stack> it = stacks.values().iterator();
            while (it.hasNext()) {
                if (now - it.next().lastAt > REACTIVE_FADES_AFTER) {
                    it.remove();
                }
            }
            if (stacks.isEmpty()) {
                REACTIVE.remove(player.getUUID());
            }
        }
    }

    private static void set(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, float amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && existing.amount() == amount) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0F) {
            instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
        if (attribute == Attributes.MAX_HEALTH && player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static void strip(ServerPlayer player) {
        for (Mod mod : MODS) {
            AttributeInstance instance = player.getAttribute(mod.attribute());
            if (instance != null && instance.hasModifier(mod.id())) {
                instance.removeModifier(mod.id());
            }
        }
        REACTIVE.remove(player.getUUID());
    }

    public static void forget(UUID playerId) {
        REACTIVE.remove(playerId);
        BLINKED_AT.remove(playerId);
        LAST_DITCH_AT_TIME.remove(playerId);
    }
}
