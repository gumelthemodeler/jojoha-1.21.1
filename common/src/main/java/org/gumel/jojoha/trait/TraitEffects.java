package org.gumel.jojoha.trait;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.BonusHit;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.item.StandArrowProjectile;
import org.gumel.jojoha.item.StandArrowRitual;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.registry.ModTraits;
import org.gumel.jojoha.stand.hierophant.EmeraldSplash;
import org.gumel.jojoha.stand.skill.StandBeat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TraitEffects {
    public static final float SECRET_TECHNIQUE_BELOW = 0.30F;

    public static final double SECRET_TECHNIQUE_SPEED = 0.20;

    public static final int RUNNING_STYLE_RAMP_TICKS = 100;

    public static final double RUNNING_STYLE_SPEED = 0.18;

    public static final float KILLERS_LUCK_DAMAGE = 1.0F;
    public static final double KILLERS_LUCK_LUCK = 1.5;

    public static final float DEVILS_BLESSING_SCALE = 1.15F;

    public static final float DEVILS_BLESSING_CAP = 2.0F;

    public static final int BIRTHMARK_EVERY_TICKS = 600;

    public static final double BIRTHMARK_RANGE = 12.0;

    public static final float BIRTHMARK_CHANCE = 0.02F;

    public static final float SECOND_HAND_CHANCE = 0.20F;

    public static final float SECOND_HAND_SHARE = 0.5F;

    public static final float SECOND_HAND_CAP = 5F;

    public static final int SECOND_HAND_DELAY_TICKS = 3;

    public static final int SECOND_HAND_STAGGER_TICKS = 8;

    public static final int DOUBLE_BLOW_STREAK = 3;

    public static final int DOUBLE_BLOW_WINDOW_TICKS = 100;

    public static final float DOUBLE_BLOW_SCALE = 1.25F;

    public static final float DOUBLE_BLOW_CAP = 2.5F;

    public static final double DOUBLE_BLOW_KNOCKBACK = 0.8;

    public static final double KEEN_EYES_RANGE = 8.0;

    public static final float IRON_STOMACH_BLOOD = 0.10F;

    public static final int DEEP_BREATH_SAVE_EVERY = 3;

    public static final float DEEP_BREATH_REGEN = 1.10F;

    public static final float PLAYING_DEAD_BELOW = 0.20F;

    public static final int PLAYING_DEAD_STILL_TICKS = 60;

    public static final double PLAYING_DEAD_RANGE = 24.0;

    public static final int PLAYING_DEAD_EVERY = 2;

    public static final double STEADY_HAND_SPEED = 1.15;

    private static final String STEADY_TAG = "jojoha_steady_hand";

    private static final double STILL_DRIFT_SQR = 0.0025;

    private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("c", "bosses"));

    private static final Map<UUID, Integer> STILL = new HashMap<>();

    private static final Map<UUID, Vec3> LAST_SPOT = new HashMap<>();

    private static final int HURT_WINDOW_OPEN = 10;

    private static final Map<UUID, Deque<Long>> SECOND_HANDS = new HashMap<>();

    private static final Set<UUID> DOUBLE_BLOW_READY = new HashSet<>();

    private static boolean secondHandLanding;

    private static final ResourceLocation SECRET_TECHNIQUE_ID = id("trait_secret_technique");
    private static final ResourceLocation RUNNING_STYLE_ID = id("trait_running_style");
    private static final ResourceLocation KILLERS_LUCK_ID = id("trait_killers_luck");

    private static final Map<UUID, Integer> SPRINTING = new HashMap<>();

    private TraitEffects() {
    }

    public static void tick(Player player) {
        if (!(player instanceof ServerPlayer server) || !server.isAlive()) {
            return;
        }
        JojohaPlayerData data = PlayerDataAccess.get(server);
        if (data.trait == null) {
            clear(server);
            return;
        }

        secretTechnique(server, data);
        runningStyle(server, data);
        killersLuck(server, data);
        birthmark(server, data);
        deepBreath(server, data);
        playingDead(server, data);
    }

    private static void deepBreath(ServerPlayer player, JojohaPlayerData data) {
        if (!Traits.has(data, ModTraits.DEEP_BREATH) || !player.isUnderWater()
                || player.getAirSupply() >= player.getMaxAirSupply() || player.getAirSupply() <= 0
                || player.tickCount % DEEP_BREATH_SAVE_EVERY != 0) {
            return;
        }
        player.setAirSupply(player.getAirSupply() + 1);
    }

    public static float breathScale(JojohaPlayerData data) {
        return data.discipline == Discipline.HAMON && Traits.has(data, ModTraits.DEEP_BREATH)
                ? DEEP_BREATH_REGEN : 1F;
    }

    private static void playingDead(ServerPlayer player, JojohaPlayerData data) {
        UUID id = player.getUUID();
        if (!Traits.has(data, ModTraits.PLAYING_DEAD)) {
            STILL.remove(id);
            LAST_SPOT.remove(id);
            return;
        }
        Vec3 last = LAST_SPOT.put(id, player.position());
        boolean still = last != null && last.distanceToSqr(player.position()) < STILL_DRIFT_SQR
                && player.isShiftKeyDown() && player.onGround() && !player.swinging
                && player.getHealth() < player.getMaxHealth() * PLAYING_DEAD_BELOW;
        int held = still ? STILL.getOrDefault(id, 0) + 1 : 0;
        STILL.put(id, held);
        if (held == PLAYING_DEAD_STILL_TICKS) {
            player.displayClientMessage(Component.translatable("message.jojoha.trait.playing_dead"), true);
        }
        if (held < PLAYING_DEAD_STILL_TICKS || held % PLAYING_DEAD_EVERY != 0) {
            return;
        }
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(PLAYING_DEAD_RANGE),
                mob -> mob.getTarget() == player && !mob.getType().is(BOSSES))) {
            mob.setTarget(null);
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            if (mob instanceof NeutralMob angry) {
                angry.stopBeingAngry();
            }
        }
    }

    public static boolean ironStomach(LivingEntity eater, FoodProperties food) {
        if (!(eater instanceof ServerPlayer player) || !Traits.has(player, ModTraits.IRON_STOMACH)) {
            return false;
        }
        for (FoodProperties.PossibleEffect possible : food.effects()) {
            MobEffectInstance effect = possible.effect();
            if (effect.getEffect().value().isBeneficial() && player.getRandom().nextFloat() < possible.probability()) {
                player.addEffect(effect);
            }
        }
        return true;
    }

    public static int bloodDrawn(ServerPlayer player, int amount) {
        if (amount <= 0 || !Traits.has(player, ModTraits.IRON_STOMACH)) {
            return amount;
        }
        float extra = amount * IRON_STOMACH_BLOOD;
        int whole = (int) extra;
        return amount + whole + (player.getRandom().nextFloat() < extra - whole ? 1 : 0);
    }

    public static boolean keenEyes(JojohaPlayerData data) {
        return data != null && Traits.has(data, ModTraits.KEEN_EYES);
    }

    public static void steadyHand(Entity entity) {
        if (!(entity instanceof Projectile projectile) || entity.level().isClientSide()
                || !(projectile.getOwner() instanceof ServerPlayer thrower)
                || !thrown(projectile) || entity.getTags().contains(STEADY_TAG)
                || !Traits.has(thrower, ModTraits.STEADY_HAND)) {
            return;
        }
        entity.addTag(STEADY_TAG);
        entity.setDeltaMovement(entity.getDeltaMovement().scale(STEADY_HAND_SPEED));
        if (entity instanceof Arrow || entity instanceof SpectralArrow) {
            AbstractArrow arrow = (AbstractArrow) entity;
            arrow.setBaseDamage(arrow.getBaseDamage() / STEADY_HAND_SPEED);
        }
    }

    private static boolean thrown(Projectile projectile) {
        if (projectile instanceof StandArrowProjectile || projectile instanceof EmeraldSplash
                || projectile instanceof ThrownEnderpearl) {
            return false;
        }
        return projectile instanceof AbstractArrow || projectile instanceof ThrowableItemProjectile;
    }

    public static void forget(Player player) {
        STILL.remove(player.getUUID());
        LAST_SPOT.remove(player.getUUID());
        SPRINTING.remove(player.getUUID());
    }

    private static void secretTechnique(ServerPlayer player, JojohaPlayerData data) {
        boolean wounded = Traits.has(data, ModTraits.SECRET_TECHNIQUE)
                && player.getHealth() < player.getMaxHealth() * SECRET_TECHNIQUE_BELOW;
        set(player, Attributes.MOVEMENT_SPEED, SECRET_TECHNIQUE_ID,
                wounded ? SECRET_TECHNIQUE_SPEED : 0.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void runningStyle(ServerPlayer player, JojohaPlayerData data) {
        if (!Traits.has(data, ModTraits.RUNNING_STYLE)) {
            SPRINTING.remove(player.getUUID());
            set(player, Attributes.MOVEMENT_SPEED, RUNNING_STYLE_ID, 0.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            return;
        }

        int held = player.isSprinting() && player.onGround()
                ? Math.min(RUNNING_STYLE_RAMP_TICKS, SPRINTING.getOrDefault(player.getUUID(), 0) + 1)
                : 0;
        SPRINTING.put(player.getUUID(), held);
        set(player, Attributes.MOVEMENT_SPEED, RUNNING_STYLE_ID,
                RUNNING_STYLE_SPEED * held / RUNNING_STYLE_RAMP_TICKS,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    private static void killersLuck(ServerPlayer player, JojohaPlayerData data) {
        int payout = Traits.has(data, ModTraits.KILLERS_LUCK) ? Traits.luckPayout(player) : 0;
        set(player, Attributes.LUCK, KILLERS_LUCK_ID, KILLERS_LUCK_LUCK * payout,
                AttributeModifier.Operation.ADD_VALUE);
    }

    private static void birthmark(ServerPlayer player, JojohaPlayerData data) {
        if (!Traits.has(data, ModTraits.JOESTAR_BIRTHMARK) || !data.stand.isPresent()
                || player.tickCount % BIRTHMARK_EVERY_TICKS != 0) {
            return;
        }

        for (Player other : player.serverLevel().getEntitiesOfClass(Player.class,
                player.getBoundingBox().inflate(BIRTHMARK_RANGE))) {
            if (!(other instanceof ServerPlayer near) || near == player) {
                continue;
            }
            JojohaPlayerData theirs = PlayerDataAccess.get(near);
            if (theirs.stand.isPresent() || theirs.standArrowRitualTicks > 0
                    || theirs.worthiness < Traits.HIGH_WORTHINESS) {
                continue;
            }
            if (player.getRandom().nextFloat() < JojohaServerConfig.BIRTHMARK.chance()) {
                StandArrowRitual.grant(near, "message.jojoha.trait.birthmark");
            }
        }
    }

    private static void clear(ServerPlayer player) {
        set(player, Attributes.MOVEMENT_SPEED, SECRET_TECHNIQUE_ID, 0.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        set(player, Attributes.MOVEMENT_SPEED, RUNNING_STYLE_ID, 0.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        set(player, Attributes.LUCK, KILLERS_LUCK_ID, 0.0, AttributeModifier.Operation.ADD_VALUE);
    }

    public static float incoming(LivingEntity victim, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof ServerPlayer attacker)
                || source.getDirectEntity() != attacker) {
            return amount;
        }
        JojohaPlayerData data = PlayerDataAccess.get(attacker);
        if (data.trait == null) {
            return amount;
        }
        float dealt = amount;

        if (Traits.has(data, ModTraits.TWO_RIGHT_HANDS) && !secondHandLanding
                && victim.invulnerableTime <= HURT_WINDOW_OPEN) {
            dealt += twoRightHands(attacker, victim, amount);
        }

        if (Traits.has(data, ModTraits.KILLERS_LUCK)) {
            dealt += KILLERS_LUCK_DAMAGE * Traits.luckPayout(attacker);
        }

        if (Traits.has(data, ModTraits.DEVILS_BLESSING) && victim instanceof ServerPlayer struck
                && PlayerDataAccess.get(struck).worthiness >= Traits.HIGH_WORTHINESS) {
            dealt += Math.min(amount * (DEVILS_BLESSING_SCALE - 1F), DEVILS_BLESSING_CAP);
        }
        return dealt;
    }

    private static float twoRightHands(ServerPlayer attacker, LivingEntity victim, float amount) {
        UUID id = attacker.getUUID();
        if (DOUBLE_BLOW_READY.remove(id)) {
            float yaw = attacker.getYRot() * Mth.DEG_TO_RAD;
            victim.knockback(DOUBLE_BLOW_KNOCKBACK, Mth.sin(yaw), -Mth.cos(yaw));
            attacker.serverLevel().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                    SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.0F, 0.8F);
            attacker.serverLevel().sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.6),
                    victim.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
            return Math.min(amount * (DOUBLE_BLOW_SCALE - 1F), DOUBLE_BLOW_CAP);
        }

        if (attacker.getRandom().nextFloat() >= JojohaServerConfig.SECOND_HAND.chance()) {
            return 0F;
        }
        float second = Math.min(amount * SECOND_HAND_SHARE, SECOND_HAND_CAP);
        StandBeat.after(SECOND_HAND_DELAY_TICKS, attacker, victim,
                (striker, struck) -> secondHand(striker, struck, second));

        long now = attacker.serverLevel().getGameTime();
        Deque<Long> streak = SECOND_HANDS.computeIfAbsent(id, key -> new ArrayDeque<>());
        while (!streak.isEmpty() && now - streak.peekFirst() > DOUBLE_BLOW_WINDOW_TICKS) {
            streak.pollFirst();
        }
        streak.addLast(now);
        if (streak.size() >= DOUBLE_BLOW_STREAK) {
            streak.clear();
            DOUBLE_BLOW_READY.add(id);
            attacker.playNotifySound(SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.6F, 1.6F);
        }
        return 0F;
    }

    private static void secondHand(ServerPlayer attacker, LivingEntity victim, float amount) {
        if (!victim.isAlive() || !attacker.isAlive()) {
            return;
        }
        attacker.swing(InteractionHand.OFF_HAND, true);
        secondHandLanding = true;
        try {
            BonusHit.land(victim, attacker.damageSources().playerAttack(attacker),
                    DamageScale.combat(attacker, victim, amount));
        } finally {
            secondHandLanding = false;
        }
        victim.addEffect(new MobEffectInstance(ModEffects.stun(),
                SECOND_HAND_STAGGER_TICKS, Stun.TIER_ONE, false, false, true));
        attacker.serverLevel().playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    public static boolean silentFootsteps(Player player) {
        return Traits.has(player, ModTraits.SILENT_FOOTSTEPS);
    }

    private static void set(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id,
                            double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && existing.amount() == amount) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
    }
}
