package org.gumel.jojoha.stand.anubis;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.mob.MobStands;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class Bearer {
    public static final String TAG = Jojoha.MOD_ID + ".anubis_bearer";

    public static final int WORTHINESS = 50;
    private static final double WORTHY_RANGE = 64.0;

    public static final int CHANCE_IN = 500;

    private static final double ALONE_RANGE = 128.0;

    private static final Set<EntityType<?>> NO_SWORDSMEN = Set.of(EntityType.WITCH, EntityType.EVOKER,
            EntityType.PILLAGER);

    private static final ResourceLocation STRENGTH = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "anubis_bearer");
    private static final double HEALTH = 40.0;
    private static final double ARMOR = 6.0;
    private static final double SPEED = 0.25;
    private static final double FOOTING = 0.6;
    private static final double FOLLOW = 16.0;

    private static final double LUNGE_LEAST = 3.0;
    private static final double LUNGE_MOST = 7.0;
    private static final double LUNGE_SPEED = 1.0;
    private static final int LUNGE_EVERY_BEARER = 60;
    private static final int LUNGE_EVERY = 120;

    private static final int PARRY_EVERY_BEARER = 50;
    private static final int PARRY_EVERY = 120;
    private static final float COUNTER_BEARER = 5.0F;
    private static final float COUNTER = 3.0F;
    private static final double PARRY_REACH = 6.0;

    private static final Map<Mob, Long> NEXT_LUNGE = new WeakHashMap<>();
    private static final Map<Mob, Long> NEXT_PARRY = new WeakHashMap<>();

    private Bearer() {
    }

    static void init() {
        EntityEvent.LIVING_HURT.register((victim, source, amount) -> {
            if (victim instanceof Mob mob && mob.level() instanceof ServerLevel level && parries(level, mob, source)) {
                return EventResult.interruptFalse();
            }
            return EventResult.pass();
        });
    }

    public static boolean is(Mob mob) {
        return mob.getTags().contains(TAG);
    }

    public static boolean rise(Mob mob, MobSpawnType spawnType) {
        if (spawnType != MobSpawnType.NATURAL || !(mob.level() instanceof ServerLevel level)
                || NO_SWORDSMEN.contains(mob.getType()) || !JojohaServerConfig.oneIn(JojohaServerConfig.ANUBIS_BEARER, mob.getRandom())) {
            return false;
        }
        if (level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), WORTHY_RANGE, Bearer::worthy) == null) {
            return false;
        }
        if (!level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(ALONE_RANGE), Bearer::is).isEmpty()) {
            return false;
        }
        possess(mob);
        return true;
    }

    private static boolean worthy(Entity entity) {
        return entity instanceof ServerPlayer player && !player.isSpectator() && !player.isCreative()
                && !PlayerDataAccess.get(player).stand.isPresent()
                && PlayerDataAccess.get(player).worthiness >= WORTHINESS;
    }

    public static void possess(Mob mob) {
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        mob.setDropChance(EquipmentSlot.MAINHAND, 0F);
        MobStands.give(mob, Anubis.ID, 0);
        mob.addTag(TAG);
        raise(mob, Attributes.MAX_HEALTH, HEALTH, AttributeModifier.Operation.ADD_VALUE);
        raise(mob, Attributes.ARMOR, ARMOR, AttributeModifier.Operation.ADD_VALUE);
        raise(mob, Attributes.MOVEMENT_SPEED, SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        raise(mob, Attributes.KNOCKBACK_RESISTANCE, FOOTING, AttributeModifier.Operation.ADD_VALUE);
        raise(mob, Attributes.FOLLOW_RANGE, FOLLOW, AttributeModifier.Operation.ADD_VALUE);
        mob.setHealth(mob.getMaxHealth());
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0,
                false, false));
        mob.setCustomName(Component.translatable("entity.jojoha.anubis_bearer", mob.getType().getDescription()));
    }

    private static void raise(Mob mob, Holder<Attribute> attribute, double by,
                              AttributeModifier.Operation operation) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(STRENGTH);
        instance.addPermanentModifier(new AttributeModifier(STRENGTH, by, operation));
    }

    static void fight(Mob mob) {
        LivingEntity foe = mob.getTarget();
        if (foe == null || !foe.isAlive() || !Anubis.out(mob) || !mob.onGround()) {
            return;
        }
        long now = mob.level().getGameTime();
        int every = is(mob) ? LUNGE_EVERY_BEARER : LUNGE_EVERY;
        if (!ready(NEXT_LUNGE, mob, now, every)) {
            return;
        }
        double distance = mob.distanceTo(foe);
        if (distance < LUNGE_LEAST || distance > LUNGE_MOST || !mob.hasLineOfSight(foe)) {
            return;
        }
        NEXT_LUNGE.put(mob, now + every);
        Vec3 toward = foe.position().subtract(mob.position()).multiply(1, 0, 1).normalize();
        mob.setDeltaMovement(toward.x * LUNGE_SPEED, 0.25, toward.z * LUNGE_SPEED);
        mob.hurtMarked = true;
        mob.swing(InteractionHand.MAIN_HAND, true);
        mob.level().playSound(null, mob.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE,
                0.8F, 1.4F);
    }

    private static boolean parries(ServerLevel level, Mob mob, DamageSource source) {
        if (!Anubis.ID.equals(MobStands.standId(mob)) || !Anubis.out(mob)) {
            return false;
        }
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker
                || attacker == mob || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || attacker.distanceTo(mob) > PARRY_REACH || Defence.facing(mob, attacker) == Defence.Facing.BACK) {
            return false;
        }
        long now = level.getGameTime();
        boolean bearer = is(mob);
        int every = bearer ? PARRY_EVERY_BEARER : PARRY_EVERY;
        if (!ready(NEXT_PARRY, mob, now, every)) {
            return false;
        }
        NEXT_PARRY.put(mob, now + every);
        mob.swing(InteractionHand.MAIN_HAND, true);
        Vec3 toward = attacker.position().subtract(mob.position());
        Impacts.landed(level, attacker, toward,
                ImpactSpec.MEDIUM.silent(), Impacts.STEEL);
        level.playSound(null, mob.blockPosition(), ModSounds.PARRY.get(),
                SoundSource.HOSTILE, 1.0F, 1.0F);
        attacker.hurt(mob.damageSources().mobAttack(mob), DamageScale.mobStand(mob, attacker, bearer ? COUNTER_BEARER : COUNTER));
        attacker.knockback(0.5, mob.getX() - attacker.getX(), mob.getZ() - attacker.getZ());
        return true;
    }

    private static boolean ready(Map<Mob, Long> next, Mob mob, long now, int every) {
        Long at = next.get(mob);
        return at == null || now >= at || at - now > every;
    }
}
