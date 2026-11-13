package org.gumel.jojoha.entity;

import org.gumel.jojoha.entity.npc.NpcStanding;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.VampireBlood;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.entity.npc.HamonStudent;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.vampire.VampireFx;
import org.gumel.jojoha.worldevent.WorldEvent;
import org.gumel.jojoha.worldevent.WorldEvents;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VampireEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(VampireEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(VampireEntity.class, EntityDataSerializers.INT);

    public enum Kind {
        STALKER("stalker", MAX_HEALTH, ATTACK_DAMAGE, MOVEMENT_SPEED, ARMOR, TECHNIQUE_GAP,
                BLITZ_COOLDOWN, FREEZE_COOLDOWN, RIPPER_COOLDOWN, 0, 0, 0, false),
        RIPPER("ripper", 28.0, 4.0, 0.26, 2.0, 25, 0, 200, 160, 70, 0, 0, true),
        SIRE("sire", 34.0, 5.0, 0.27, 3.0, 40, 0, 200, 0, 100, 0, 400, true),
        FROST("frost", 46.0, 6.0, 0.26, 6.0, 35, 200, 80, 0, 0, 180, 0, false),
        RAVAGER("ravager", 52.0, ATTACK_DAMAGE, 0.33, 6.0, 20, 60, 0, 0, 0, 0, 0, false);

        final String key;
        final double health;
        final double damage;
        final double speed;
        final double armor;
        final int gap;
        final int blitz;
        final int freeze;
        final int ripper;
        final int needles;
        final int nova;
        final int raise;
        final boolean keepsAway;

        Kind(String key, double health, double damage, double speed, double armor, int gap, int blitz, int freeze,
             int ripper, int needles, int nova, int raise, boolean keepsAway) {
            this.key = key;
            this.health = health;
            this.damage = damage;
            this.speed = speed;
            this.armor = armor;
            this.gap = gap;
            this.blitz = blitz;
            this.freeze = freeze;
            this.ripper = ripper;
            this.needles = needles;
            this.nova = nova;
            this.raise = raise;
            this.keepsAway = keepsAway;
        }
    }

    public static final int VARIANTS = 5;

    private static final double MAX_HEALTH = 40.0;
    private static final double ATTACK_DAMAGE = 7.0;
    private static final double MOVEMENT_SPEED = 0.28;
    private static final double FOLLOW_RANGE = 32.0;
    private static final double ARMOR = 4.0;
    private static final double KNOCKBACK_RESISTANCE = 0.3;
    private static final int XP_REWARD = 15;

    private static final float DRAIN_FRACTION = 0.5F;

    private static final float SUN_DAMAGE = 5F;
    private static final int SUN_BURN_EVERY = 4;

    private static final int TECHNIQUE_GAP = 50;

    private static final double BLITZ_MIN_RANGE = 5.0;
    private static final double BLITZ_MAX_RANGE = 12.0;
    private static final int BLITZ_WINDUP = 8;
    private static final int BLITZ_DASH_TICKS = 10;
    private static final double BLITZ_LAUNCH = 1.6;
    private static final double BLITZ_LIFT = 0.25;
    private static final double BLITZ_HIT_RADIUS = 1.1;
    private static final float BLITZ_DAMAGE = 5F;
    private static final int BLITZ_COOLDOWN = 120;

    private static final double FREEZE_RANGE = 3.0;
    private static final double FREEZE_REACH = 3.5;
    private static final int FREEZE_WINDUP = 8;
    private static final float FREEZE_DAMAGE = 7F;
    private static final int FREEZE_SLOW_TICKS = 60;
    private static final int FREEZE_COOLDOWN = 160;

    private static final double RIPPER_MIN_RANGE = 6.0;
    private static final double RIPPER_MAX_RANGE = 24.0;
    private static final int RIPPER_FIRE_FROM = 20;
    private static final int RIPPER_FIRE_TO = 35;
    private static final int RIPPER_TRAIL = 8;
    private static final double RIPPER_EYE_SPACING = 0.12;
    private static final float RIPPER_DAMAGE = 5F;
    private static final int RIPPER_COOLDOWN = 500;
    private static final int RIPPER_FIRST_DELAY = 100;
    private static final int RIPPER_SHARED_GAP = 140;
    private static final double RIPPER_TRACK_DEGREES = 1.2;
    private static final int RIPPER_LOCK_AHEAD = 8;
    private static final double RIPPER_HIT_SLACK = 0.25;

    private static final double NEEDLE_MIN_RANGE = 4.0;
    private static final double NEEDLE_RANGE = 20.0;
    private static final int NEEDLE_FIRST = 12;
    private static final int NEEDLE_GAP = 11;
    private static final int NEEDLE_LEAD = 6;
    private static final int NEEDLE_SHOTS = 3;
    private static final float NEEDLE_DAMAGE = 3F;
    private static final double NEEDLE_SLACK = 0.2;
    private static final double NEEDLE_TRAIL_STEP = 0.8;

    private static final double NOVA_TRIGGER = 4.5;
    private static final double NOVA_RADIUS = 5.0;
    private static final int NOVA_WINDUP = 14;
    private static final float NOVA_DAMAGE = 5F;

    private static final int RAISE_WINDUP = 20;
    private static final int RAISE_CAP = 3;
    private static final int RAISE_AT_ONCE = 2;
    private static final int RAISE_LIFETIME = 600;
    private static final double RAISE_SPREAD = 2.5;
    private static final double RAISE_SEEK = 32.0;
    private static final int RALLY_EVERY = 20;

    private static final double KEEP_NEAR = 7.0;
    private static final double KEEP_FAR = 15.0;
    private static final int KEEP_REPATH = 10;
    private static final int KEEP_SWING_GAP = 20;
    private static final float KEEP_STRAFE = 0.5F;

    private static final int FRENZY_EFFECT_TICKS = 60;

    private static final double ALLY_RANGE = 24.0;
    private static final int GRUDGE_TICKS = 6000;
    private static final int GRUDGE_SWEEP = 100;

    private static final float MIGHT_FLOOR = 0.65F;
    private static final float MAX_EFFECTIVE_DIFFICULTY = 6.75F;

    private static final Map<UUID, Long> RIPPED_AT = new ConcurrentHashMap<>();

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private long blitzReadyAt;
    private long freezeReadyAt;
    private long ripperReadyAt = -1L;
    private long needlesReadyAt;
    private long novaReadyAt;
    private long raiseReadyAt;
    private long techniqueReadyAt;

    private final Map<UUID, Long> grudges = new HashMap<>();
    private float might = 1F;

    public VampireEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = XP_REWARD;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.ARMOR, ARMOR)
                .add(Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TechniqueGoal());
        goalSelector.addGoal(2, new KeepAwayGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false) {
            @Override
            public boolean canUse() {
                return !kind().keepsAway && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !kind().keepsAway && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 10F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, target -> !spared(target)));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, HamonStudent.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(KIND, 0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        entityData.set(VARIANT, random.nextInt(VARIANTS));
        might = Mth.clamp(MIGHT_FLOOR + (1F - MIGHT_FLOOR) * difficulty.getEffectiveDifficulty() / MAX_EFFECTIVE_DIFFICULTY,
                MIGHT_FLOOR, 1F);
        entityData.set(KIND, rollKind().ordinal());
        applyKind();
        setHealth(getMaxHealth());
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData);
    }

    public void huntKind(Kind kind) {
        entityData.set(KIND, kind.ordinal());
        applyKind();
        setHealth(getMaxHealth());
    }

    public Kind kind() {
        return Kind.values()[Mth.clamp(entityData.get(KIND), 0, Kind.values().length - 1)];
    }

    private Kind rollKind() {
        float late = (might - MIGHT_FLOOR) / (1F - MIGHT_FLOOR);
        float total = 0F;
        for (Kind kind : Kind.values()) {
            total += weight(kind, late);
        }
        float pick = random.nextFloat() * total;
        for (Kind kind : Kind.values()) {
            pick -= weight(kind, late);
            if (pick < 0F) {
                return kind;
            }
        }
        return Kind.STALKER;
    }

    private static float weight(Kind kind, float late) {
        return switch (kind) {
            case STALKER -> 4F;
            case RIPPER -> 3F;
            case SIRE -> 2F;
            case FROST -> 1F + 2F * late;
            case RAVAGER -> 0.5F + 2.5F * late;
        };
    }

    public float might() {
        return might;
    }

    public boolean spared(LivingEntity target) {
        if (!(target instanceof Player player) || !VampireBlood.isVampire(player)) {
            return false;
        }
        Long until = grudges.get(player.getUUID());
        return until == null || until < level().getGameTime();
    }

    public void provokedBy(Player player) {
        if (!VampireBlood.isVampire(player)) {
            return;
        }
        long until = level().getGameTime() + GRUDGE_TICKS;
        if (NpcStanding.vampire(player) >= NpcStanding.FRIENDLY) {
            grudges.put(player.getUUID(), until);
            if (getTarget() == null) {
                setTarget(player);
            }
            return;
        }
        for (VampireEntity ally : level().getEntitiesOfClass(VampireEntity.class, getBoundingBox().inflate(ALLY_RANGE),
                LivingEntity::isAlive)) {
            ally.grudges.put(player.getUUID(), until);
            if (ally.getTarget() == null) {
                ally.setTarget(player);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide() && source.getEntity() instanceof Player player) {
            provokedBy(player);
        }
        return hurt;
    }

    private void applyKind() {
        Kind kind = kind();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(kind.health * might);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(kind.damage * might);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(kind.speed);
        getAttribute(Attributes.ARMOR).setBaseValue(kind.armor);
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.jojoha.vampire." + kind().key);
    }

    public int variant() {
        return Mth.clamp(entityData.get(VARIANT), 0, VARIANTS - 1);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean landed = super.doHurtTarget(target);
        if (landed && target instanceof LivingEntity victim
                && VampireBlood.hasBlood(victim)
                && level() instanceof ServerLevel level) {
            heal((float) getAttributeValue(Attributes.ATTACK_DAMAGE) * DRAIN_FRACTION);
            if (JojohaServerConfig.MOB_STATUSES.on()) {
                Bleed.bite(victim, kind() == Kind.RIPPER ? Bleed.MAX_AMPLIFIER : Bleed.MOB_CAP);
            }
            Vec3 from = victim.position().add(0.0, victim.getBbHeight() * 0.6, 0.0);
            VampireFx.blood(level, from.x, from.y, from.z, 8, 0.25, 0.08);
            level.playSound(null, blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.HOSTILE,
                    0.6F, 0.5F);
        }
        return landed;
    }

    @Override
    public void aiStep() {
        if (isAlive() && !level().isClientSide() && isSunBurnTick()) {
            igniteForSeconds(8F);
            if (tickCount % SUN_BURN_EVERY == 0 && level() instanceof ServerLevel level) {
                hurt(damageSources().onFire(), SUN_DAMAGE);
                level.sendParticles(ParticleTypes.ASH, getX(), getY() + getBbHeight() * 0.6,
                        getZ(), 12, 0.3, 0.5, 0.3, 0.02);
            }
        }
        if (isAlive() && level() instanceof ServerLevel level) {
            if (getTarget() != null && spared(getTarget())) {
                setTarget(null);
            }
            if (tickCount % GRUDGE_SWEEP == 0) {
                grudges.values().removeIf(until -> until < level.getGameTime());
            }
            if (kind() == Kind.RAVAGER && getHealth() < getMaxHealth() * 0.5F && getTarget() != null
                    && tickCount % 20 == 0) {
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, FRENZY_EFFECT_TICKS, 0, true, false));
            }
            if (kind() == Kind.SIRE && getTarget() != null && tickCount % RALLY_EVERY == 0) {
                for (RisenZombie minion : minions(level)) {
                    if (minion.getTarget() == null) {
                        minion.setTarget(getTarget());
                    }
                }
            }
        }
        super.aiStep();
    }

    private List<RisenZombie> minions(ServerLevel level) {
        return level.getEntitiesOfClass(RisenZombie.class, getBoundingBox().inflate(RAISE_SEEK),
                minion -> getUUID().equals(minion.master()));
    }

    private final class KeepAwayGoal extends Goal {
        private int repath;
        private int swingIn;
        private float strafe = KEEP_STRAFE;

        KeepAwayGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return kind().keepsAway && target != null && target.isAlive();
        }

        @Override
        public void start() {
            repath = 0;
            setAggressive(true);
        }

        @Override
        public void stop() {
            setAggressive(false);
            getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            getLookControl().setLookAt(target, 30F, 30F);
            lookAt(target, 30F, 30F);
            swingIn = Math.max(0, swingIn - 1);
            if (swingIn == 0 && isWithinMeleeAttackRange(target) && hasLineOfSight(target)) {
                swingIn = KEEP_SWING_GAP;
                swing(InteractionHand.MAIN_HAND);
                doHurtTarget(target);
            }
            double distance = distanceTo(target);
            if (--repath <= 0) {
                repath = KEEP_REPATH;
                if (distance < KEEP_NEAR) {
                    Vec3 away = DefaultRandomPos.getPosAway(VampireEntity.this, 10, 5, target.position());
                    if (away != null) {
                        getNavigation().moveTo(away.x, away.y, away.z, 1.25);
                    }
                } else if (distance > KEEP_FAR || !hasLineOfSight(target)) {
                    getNavigation().moveTo(target, 1.1);
                } else {
                    getNavigation().stop();
                }
            }
            if (getNavigation().isDone()) {
                if (random.nextInt(40) == 0) {
                    strafe = -strafe;
                }
                getMoveControl().strafe(0F, strafe);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    private enum Technique { BLITZ, FREEZE, RIPPER, NEEDLES, NOVA, RAISE }

    @Nullable
    private Technique choose(ServerLevel level, LivingEntity target) {
        long now = level.getGameTime();
        if (now < techniqueReadyAt) {
            return null;
        }
        double distance = distanceTo(target);
        boolean seen = hasLineOfSight(target);
        List<Technique> fit = new ArrayList<>();
        Kind kind = kind();
        if (kind.freeze > 0 && now >= freezeReadyAt && distance <= FREEZE_RANGE) {
            fit.add(Technique.FREEZE);
        }
        if (kind.blitz > 0 && now >= blitzReadyAt && seen && onGround() && distance >= BLITZ_MIN_RANGE && distance <= BLITZ_MAX_RANGE) {
            fit.add(Technique.BLITZ);
        }
        if (ripperReadyAt < 0L) {
            ripperReadyAt = now + RIPPER_FIRST_DELAY + random.nextInt(RIPPER_FIRST_DELAY);
        }
        if (kind.ripper > 0 && now >= ripperReadyAt && seen && distance >= RIPPER_MIN_RANGE && distance <= RIPPER_MAX_RANGE
                && now >= RIPPED_AT.getOrDefault(target.getUUID(), 0L)) {
            fit.add(Technique.RIPPER);
        }
        if (kind.needles > 0 && now >= needlesReadyAt && seen && distance >= NEEDLE_MIN_RANGE && distance <= NEEDLE_RANGE) {
            fit.add(Technique.NEEDLES);
        }
        if (kind.nova > 0 && now >= novaReadyAt && distance <= NOVA_TRIGGER) {
            fit.add(Technique.NOVA);
        }
        if (kind.raise > 0 && now >= raiseReadyAt && minions(level).size() < RAISE_CAP) {
            fit.add(Technique.RAISE);
        }
        return fit.isEmpty() ? null : fit.get(random.nextInt(fit.size()));
    }

    private static int length(Technique technique) {
        return switch (technique) {
            case BLITZ -> BLITZ_WINDUP + BLITZ_DASH_TICKS;
            case FREEZE -> FREEZE_WINDUP + 4;
            case RIPPER -> RIPPER_FIRE_TO + 2;
            case NEEDLES -> NEEDLE_FIRST + NEEDLE_GAP * (NEEDLE_SHOTS - 1) + 2;
            case NOVA -> NOVA_WINDUP + 4;
            case RAISE -> RAISE_WINDUP + 4;
        };
    }

    private final class TechniqueGoal extends Goal {
        @Nullable
        private Technique running;
        private int ticks;

        private final Set<UUID> struck = new HashSet<>();

        @Nullable
        private Vec3 ripperAim;

        @Nullable
        private Vec3 needleAim;

        TechniqueGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            if (target == null || !target.isAlive() || !(level() instanceof ServerLevel level)) {
                return false;
            }
            running = choose(level, target);
            return running != null;
        }

        @Override
        public boolean canContinueToUse() {
            return running != null && isAlive() && ticks < length(running);
        }

        @Override
        public void start() {
            ticks = 0;
            struck.clear();
            getNavigation().stop();
            if (!(level() instanceof ServerLevel level)) {
                return;
            }
            switch (running) {
                case BLITZ -> {
                    VampireFx.ring(level, getX(), getY() + 0.1, getZ(), 1.4, 1);
                    level.playSound(null, blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 0.8F, 0.6F);
                }
                case FREEZE -> {
                    VampireFx.frost(level, getX(), getY() + 1.2, getZ(), 12);
                    level.playSound(null, blockPosition(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE,
                            0.8F, 0.7F);
                }
                case RIPPER -> {
                    ripperAim = null;
                    LivingEntity marked = getTarget();
                    if (marked != null) {
                        RIPPED_AT.values().removeIf(until -> until < level.getGameTime());
                        RIPPED_AT.put(marked.getUUID(), level.getGameTime() + RIPPER_SHARED_GAP);
                    }
                    Vec3 eyes = getEyePosition();
                    VampireFx.ripperRing(level, eyes.x, eyes.y, eyes.z, 0.6, 1);
                    level.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE,
                            0.8F, 1.6F);
                }
                case NEEDLES -> {
                    needleAim = null;
                    VampireFx.blood(level, getX(), getY() + 1.4, getZ(), 8, 0.3, 0.03);
                    level.playSound(null, blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.HOSTILE, 0.8F, 0.7F);
                }
                case NOVA -> {
                    VampireFx.frost(level, getX(), getY() + 1.0, getZ(), 24);
                    level.playSound(null, blockPosition(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE,
                            1.0F, 0.5F);
                }
                case RAISE -> {
                    VampireFx.ring(level, getX(), getY() + 0.1, getZ(), 2.0, 1);
                    level.playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE,
                            1.0F, 0.6F);
                }
            }
        }

        @Override
        public void tick() {
            ticks++;
            LivingEntity target = getTarget();
            if (!(level() instanceof ServerLevel level)) {
                return;
            }
            boolean dashing = running == Technique.BLITZ && ticks > BLITZ_WINDUP;
            if (target != null && !dashing) {
                getLookControl().setLookAt(target, 30F, 30F);
            }
            switch (running) {
                case BLITZ -> blitz(level, target);
                case FREEZE -> {
                    if (ticks == FREEZE_WINDUP && target != null) {
                        freeze(level, target);
                    }
                }
                case RIPPER -> {
                    if (ticks == RIPPER_FIRE_FROM - RIPPER_LOCK_AHEAD && target != null) {
                        ripperAim = target.getBoundingBox().getCenter().subtract(getEyePosition()).normalize();
                    }
                    if (ticks >= RIPPER_FIRE_FROM && ticks <= RIPPER_FIRE_TO && target != null) {
                        ripper(level, target, ticks == RIPPER_FIRE_FROM);
                    }
                }
                case NEEDLES -> {
                    if (target != null && (ticks - NEEDLE_FIRST + NEEDLE_LEAD) % NEEDLE_GAP == 0) {
                        needleAim = target.getBoundingBox().getCenter();
                    }
                    if (target != null && needleAim != null && ticks >= NEEDLE_FIRST
                            && (ticks - NEEDLE_FIRST) % NEEDLE_GAP == 0) {
                        needle(level, target, needleAim);
                    }
                }
                case NOVA -> {
                    if (ticks == NOVA_WINDUP) {
                        nova(level);
                    }
                }
                case RAISE -> {
                    if (ticks == RAISE_WINDUP) {
                        raise(level, target);
                    }
                }
            }
        }

        private void blitz(ServerLevel level, @Nullable LivingEntity target) {
            if (ticks == BLITZ_WINDUP) {
                Vec3 toward = target == null ? getLookAngle() : target.position().subtract(position());
                Vec3 flat = new Vec3(toward.x, 0.0, toward.z);
                if (flat.lengthSqr() < 1.0E-4) {
                    return;
                }
                flat = flat.normalize();
                setDeltaMovement(flat.x * BLITZ_LAUNCH, BLITZ_LIFT, flat.z * BLITZ_LAUNCH);
                hurtMarked = true;
                VampireFx.ring(level, getX(), getY() + 0.1, getZ(), 2.0, 2);
                level.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE,
                        1.0F, 0.6F);
            } else if (ticks > BLITZ_WINDUP) {
                VampireFx.blood(level, getX(), getY() + 1.0, getZ(), 2, 0.2, 0.02);
                for (LivingEntity caught : level.getEntitiesOfClass(LivingEntity.class,
                        getBoundingBox().inflate(BLITZ_HIT_RADIUS),
                        body -> body != VampireEntity.this && body.isAlive() && !(body instanceof VampireEntity) && !spared(body))) {
                    if (struck.add(caught.getUUID())) {
                        caught.hurt(damageSources().mobAttack(VampireEntity.this), DamageScale.vampireMob(VampireEntity.this, caught, BLITZ_DAMAGE * might));
                    }
                }
            }
        }

        private void freeze(ServerLevel level, LivingEntity target) {
            swing(InteractionHand.MAIN_HAND);
            if (distanceTo(target) > FREEZE_REACH || !hasLineOfSight(target)) {
                return;
            }
            if (target.hurt(damageSources().mobAttack(VampireEntity.this), DamageScale.vampireMob(VampireEntity.this, target, FREEZE_DAMAGE * might))) {
                target.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, FREEZE_SLOW_TICKS, 1));
                target.setTicksFrozen(Math.max(target.getTicksFrozen(),
                        target.getTicksRequiredToFreeze() + FREEZE_SLOW_TICKS));
            }
            VampireFx.frost(level, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 30);
            level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.8F, 0.8F);
        }

        private void needle(ServerLevel level, LivingEntity target, Vec3 aim) {
            Vec3 from = getEyePosition();
            Vec3 direction = aim.subtract(from).normalize();
            Vec3 far = from.add(direction.scale(NEEDLE_RANGE));
            BlockHitResult wall = level.clip(new ClipContext(from, far, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, VampireEntity.this));
            Vec3 end = wall.getType() == HitResult.Type.MISS ? far : wall.getLocation();
            double length = end.distanceTo(from);
            for (double along = 0.0; along < length; along += NEEDLE_TRAIL_STEP) {
                Vec3 at = from.add(direction.scale(along));
                VampireFx.blood(level, at.x, at.y, at.z, 1, 0.02, 0.0);
            }
            swing(InteractionHand.MAIN_HAND);
            level.playSound(null, blockPosition(), SoundEvents.LLAMA_SPIT, SoundSource.HOSTILE, 0.9F, 0.7F);
            if (target.getBoundingBox().inflate(NEEDLE_SLACK).clip(from, end).isPresent()) {
                target.hurt(damageSources().mobAttack(VampireEntity.this), DamageScale.vampireMob(VampireEntity.this, target, NEEDLE_DAMAGE * might));
                Vec3 hit = target.getBoundingBox().getCenter();
                VampireFx.blood(level, hit.x, hit.y, hit.z, 8, 0.2, 0.05);
            }
        }

        private void nova(ServerLevel level) {
            swing(InteractionHand.MAIN_HAND);
            for (LivingEntity caught : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(NOVA_RADIUS),
                    body -> body != VampireEntity.this && body.isAlive() && !(body instanceof VampireEntity) && !spared(body)
                            && distanceTo(body) <= NOVA_RADIUS && hasLineOfSight(body))) {
                if (caught.hurt(damageSources().mobAttack(VampireEntity.this), DamageScale.vampireMob(VampireEntity.this, caught, NOVA_DAMAGE * might))) {
                    caught.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE_SLOW_TICKS, 1));
                    caught.setTicksFrozen(Math.max(caught.getTicksFrozen(),
                            caught.getTicksRequiredToFreeze() + FREEZE_SLOW_TICKS));
                }
                VampireFx.frost(level, caught.getX(), caught.getY() + caught.getBbHeight() * 0.6, caught.getZ(), 12);
            }
            for (int i = 0; i < 16; i++) {
                double angle = i * Math.PI * 2 / 16;
                VampireFx.frost(level, getX() + Math.cos(angle) * NOVA_RADIUS, getY() + 0.3,
                        getZ() + Math.sin(angle) * NOVA_RADIUS, 3);
            }
            level.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.0F, 0.6F);
        }

        private void raise(ServerLevel level, @Nullable LivingEntity target) {
            int room = Math.min(RAISE_AT_ONCE, RAISE_CAP - minions(level).size());
            for (int i = 0; i < room; i++) {
                RisenZombie minion = ModRegistries.RISEN_ZOMBIE.get().create(level);
                if (minion == null) {
                    return;
                }
                minion.moveTo(getX() + (random.nextDouble() - 0.5) * RAISE_SPREAD * 2.0, getY(),
                        getZ() + (random.nextDouble() - 0.5) * RAISE_SPREAD * 2.0, getYRot(), 0F);
                if (!level.noCollision(minion)) {
                    minion.moveTo(getX(), getY(), getZ(), getYRot(), 0F);
                }
                minion.finalizeSpawn(level, level.getCurrentDifficultyAt(minion.blockPosition()),
                        MobSpawnType.MOB_SUMMONED, null);
                minion.raisedBy(VampireEntity.this, RAISE_LIFETIME, true);
                minion.setTarget(target);
                level.addFreshEntity(minion);
                VampireFx.aura(level, minion.getX(), minion.getY() + 0.5, minion.getZ(), 10);
            }
            level.playSound(null, blockPosition(), SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 1.0F, 0.7F);
        }

        private Vec3 track(Vec3 wanted) {
            if (ripperAim == null) {
                ripperAim = wanted;
                return wanted;
            }
            double cos = Mth.clamp(ripperAim.dot(wanted), -1.0, 1.0);
            double angle = Math.acos(cos);
            double most = Math.toRadians(RIPPER_TRACK_DEGREES);
            if (angle > most) {
                double t = most / angle;
                wanted = ripperAim.scale(1.0 - t).add(wanted.scale(t)).normalize();
            }
            ripperAim = wanted;
            return wanted;
        }

        private void ripper(ServerLevel level, LivingEntity target, boolean first) {
            Vec3 aim = target.getBoundingBox().getCenter();
            float yaw = yHeadRot * ((float) Math.PI / 180F);
            Vec3 across = new Vec3(Math.cos(yaw), 0.0, Math.sin(yaw)).scale(RIPPER_EYE_SPACING);
            int firing = RIPPER_FIRE_TO - RIPPER_FIRE_FROM + 1;
            Vec3 direction = track(aim.subtract(getEyePosition()).normalize());
            for (int eye = 0; eye < 2; eye++) {
                Vec3 from = getEyePosition().add(eye == 0 ? across.scale(-1.0) : across);
                Vec3 far = from.add(direction.scale(RIPPER_MAX_RANGE));
                BlockHitResult wall = level.clip(new ClipContext(
                        from, far, ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, VampireEntity.this));
                Vec3 end = wall.getType() == HitResult.Type.MISS ? far : wall.getLocation();
                NetworkHandler.sendSpaceRipper(level, VampireEntity.this,
                        List.of(from, end), eye, firing, firing + RIPPER_TRAIL);
                if (first) {
                    VampireFx.ripperRing(level, from.x, from.y, from.z, 0.3, 1);
                }
                double along = aim.subtract(from).dot(direction);
                double off = aim.distanceTo(from.add(direction.scale(Math.max(0.0, along))));
                boolean reaches = along > 0.0 && end.distanceToSqr(from) + 0.5 >= along * along
                        && off <= target.getBbWidth() * 0.5 + RIPPER_HIT_SLACK;
                if (reaches && struck.add(target.getUUID())) {
                    target.hurt(damageSources().mobAttack(VampireEntity.this), DamageScale.vampireMob(VampireEntity.this, target, RIPPER_DAMAGE * might));
                    VampireFx.ripperMist(level, aim.x, aim.y, aim.z, 10, 0.2);
                }
            }
            if (first) {
                level.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.9F, 1.8F);
            }
        }

        @Override
        public void stop() {
            long now = level().getGameTime();
            if (running != null) {
                switch (running) {
                    case BLITZ -> blitzReadyAt = now + kind().blitz;
                    case FREEZE -> freezeReadyAt = now + kind().freeze;
                    case RIPPER -> ripperReadyAt = now + kind().ripper;
                    case NEEDLES -> needlesReadyAt = now + kind().needles;
                    case NOVA -> novaReadyAt = now + kind().nova;
                    case RAISE -> raiseReadyAt = now + kind().raise;
                }
            }
            techniqueReadyAt = now + kind().gap;
            running = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    @Override
    protected int getBaseExperienceReward() {
        int base = super.getBaseExperienceReward();
        return bloodMoon() ? base * 2 : base;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (bloodMoon() && source.getEntity() instanceof Player) {
            spawnAtLocation(new ItemStack(ModItems.JOJO_COIN.get(), 1 + random.nextInt(2)));
        }
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity killed) {
        boolean kept = super.killedEntity(level, killed);
        if (killed instanceof Villager villager) {
            ZombieVillager turned = villager.convertTo(EntityType.ZOMBIE_VILLAGER, false);
            if (turned != null) {
                turned.finalizeSpawn(level, level.getCurrentDifficultyAt(turned.blockPosition()), MobSpawnType.CONVERSION,
                        new Zombie.ZombieGroupData(false, true));
                turned.setVillagerData(villager.getVillagerData());
                turned.setGossips(villager.getGossips().store(NbtOps.INSTANCE));
                turned.setTradeOffers(villager.getOffers().copy());
                turned.setVillagerXp(villager.getVillagerXp());
                return false;
            }
        }
        return kept;
    }

    private boolean bloodMoon() {
        return getServer() != null && WorldEvents.active(getServer(), WorldEvent.VAMPIRE_NIGHT);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.8F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", variant());
        tag.putInt("Kind", kind().ordinal());
        tag.putFloat("Might", might);
        CompoundTag held = new CompoundTag();
        grudges.forEach((id, until) -> held.putLong(id.toString(), until));
        tag.put("Grudges", held);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(VARIANT, Mth.clamp(tag.getInt("Variant"), 0, VARIANTS - 1));
        might = tag.contains("Might") ? Mth.clamp(tag.getFloat("Might"), MIGHT_FLOOR, 1F) : 1F;
        CompoundTag held = tag.getCompound("Grudges");
        grudges.clear();
        for (String id : held.getAllKeys()) {
            grudges.put(UUID.fromString(id), held.getLong(id));
        }
        entityData.set(KIND, tag.contains("Kind") ? tag.getInt("Kind") : random.nextInt(Kind.values().length));
        applyKind();
        setHealth(getHealth());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
