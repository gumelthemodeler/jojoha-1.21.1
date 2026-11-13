package org.gumel.jojoha.entity;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.GroundBreakerFxPacket;
import org.gumel.jojoha.vampire.VampireFx;
import org.gumel.jojoha.vampire.VampireMinions;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public class Amalgamation extends RisenZombie {
    public static final String CONTROLLER = "main";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk_cycle");
    private static final RawAnimation DEATH = RawAnimation.begin().then("death", Animation.LoopType.HOLD_ON_LAST_FRAME);
    private static final RawAnimation DEATH_VARIANT =
            RawAnimation.begin().then("death_variant", Animation.LoopType.HOLD_ON_LAST_FRAME);

    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run_cycle");
    private static final RawAnimation HEAVY = RawAnimation.begin().then("heavy_atk", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation LEAP = RawAnimation.begin().then("leap", Animation.LoopType.HOLD_ON_LAST_FRAME);
    private static final RawAnimation LAND = RawAnimation.begin().then("land", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation RAMPAGE = RawAnimation.begin().then("rampage", Animation.LoopType.PLAY_ONCE);

    private static final EntityDataAccessor<Boolean> RAGING =
            SynchedEntityData.defineId(Amalgamation.class, EntityDataSerializers.BOOLEAN);

    private static final String[] SWINGS = {"atk_right", "atk_left", "bite_right", "bite_left"};

    private static final int TRANSITION_TICKS = 4;
    private static final int CLIP_LAG = TRANSITION_TICKS;
    private static final int SWING_TICKS = 32 + CLIP_LAG;
    private static final int DEATH_TICKS = 30;
    private static final double SWING_REACH = 0.6;

    private static final int PUNCH_FIRST = 19;
    private static final double[][] PUNCH_PATH = {
            {1.67, 0.96, 1.18}, {0.12, 0.34, 2.41}, {-1.05, 0.40, 2.30}, {-1.44, 0.46, 1.77}};
    private static final double PUNCH_RADIUS = 0.65;
    private static final int BITE_FIRST = 17;
    private static final double[][] BITE_PATH = {
            {0.45, 1.12, 1.51}, {-0.02, 0.73, 1.92}, {-0.50, 0.56, 1.68}, {-0.61, 0.61, 1.34}};
    private static final double BITE_RADIUS = 0.5;
    private static final int TRACK_UNTIL = 16 + CLIP_LAG;
    private static final int STEP_IN_UNTIL = 12 + CLIP_LAG;

    private static final double REACH_NEAR = 0.2;
    private static final double REACH_FAR = 2.5;
    private static final double REACH_SIDE = 1.5;
    private static final double BITE_FAR = 1.9;
    private static final double BITE_SIDE = 0.7;
    private static final int BITE_ODDS = 3;
    private static final int LEAD_TICKS = 8;
    private static final int REST_TICKS = 10;
    private static final int RAGE_REST_TICKS = 4;
    private static final int STUCK_TICKS = 40;
    private static final double CORNERED_LEAP_MIN = 3.0;
    private static final int BLOCKING_HEAVY_ODDS = 4;

    private static final double LEAP_MIN = 6.0;
    private static final double LEAP_MAX = 16.0;
    private static final double LEAP_RISE_MAX = 6.0;
    private static final int LEAP_COOLDOWN = 120;
    private static final int LEAP_WINDUP = 13;
    private static final int LEAP_LAUNCH = LEAP_WINDUP + CLIP_LAG;
    private static final int LEAP_CLIP_TICKS = 21;
    private static final int LAND_LEAD = LEAP_CLIP_TICKS - LEAP_WINDUP;
    private static final int LAND_SLAM_FRAME = 31;
    private static final int LEAP_FLIGHT = LAND_LEAD + CLIP_LAG + LAND_SLAM_FRAME;
    private static final int LAND_TICKS = 60;
    private static final int LEAP_AIR_MAX = LEAP_FLIGHT + 20;
    private static final double VAULT_PEAK_AT = 0.68;
    private static final double VAULT_ARC = 0.3;
    private static final double VAULT_ARC_MIN = 2.0;
    private static final double VAULT_ARC_MAX = 4.5;
    private static final double VAULT_REACH_MAX = 18.0;
    private static final double VAULT_DROP = 0.9;
    private static final double STEP_UP = 1.1;
    private static final double SLAM_RADIUS = 2.5;
    private static final float SLAM_SHARE = 0.8F;

    private static final float RAGE_AT = 0.5F;
    private static final int RAGE_TICKS = 35 + CLIP_LAG;
    private static final int RAGE_ROAR = 15 + CLIP_LAG;
    private static final ResourceLocation RAGE_ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "amalgamation_rampage");
    private static final double RAGE_DAMAGE = 3.0;
    private static final double RAGE_ARMOR = 8.0;
    private static final double RAGE_TOUGHNESS = 4.0;
    private static final double RAGE_KNOCKBACK = 0.3;
    private static final double RAGE_SPEED = 0.1;

    private static final int HEAVY_CONTACT = 28 + CLIP_LAG;
    private static final int HEAVY_TICKS = 50 + CLIP_LAG;
    private static final int HEAVY_RECOVER = 40 + CLIP_LAG;
    private static final int HEAVY_AIM_UNTIL = 20 + CLIP_LAG;
    private static final int HEAVY_COOLDOWN = 200;
    private static final double HEAVY_REACH = 5.0;
    private static final double HEAVY_AHEAD = 1.8;
    private static final double HEAVY_RADIUS = 5.0;
    private static final int HEAVY_WAVE_TICKS = 10;
    private static final double HEAVY_LAUNCH = 1.0;
    private static final double HEAVY_SHOVE = 0.35;
    private static final int HEAVY_CROWD = 2;
    private static final int HEAVY_SOLO_ODDS = 40;
    private static final int LEAP_WAVE_TICKS = 6;

    private int swinging = -1;
    private boolean biting;
    private boolean mirrored;
    private boolean struck;
    private boolean cornered;
    private boolean leaping;
    private long leapReadyAt;
    private int vaultTick;
    private double vaultArc;

    @Nullable
    private Vec3 vaultFrom;

    @Nullable
    private Vec3 vaultTo;
    private int heavy = -1;
    private int landing = -1;
    private int enraging = -1;
    private long heavyReadyAt;
    private int waveAge;
    private float waveDamage;
    private final Set<UUID> caught = new HashSet<>();

    @Nullable
    private Vec3 waveCentre;

    @Nullable
    private LivingEntity swingAt;

    public Amalgamation(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        VampireMinions.Tier tier = VampireMinions.Tier.AMALGAM;
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, tier.health())
                .add(Attributes.ATTACK_DAMAGE, tier.damage())
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.STEP_HEIGHT, STEP_UP)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RAGING, false);
    }

    public boolean raging() {
        return entityData.get(RAGING);
    }

    private void enrage() {
        entityData.set(RAGING, true);
        boost(Attributes.ATTACK_DAMAGE, RAGE_DAMAGE);
        boost(Attributes.ARMOR, RAGE_ARMOR);
        boost(Attributes.ARMOR_TOUGHNESS, RAGE_TOUGHNESS);
        boost(Attributes.KNOCKBACK_RESISTANCE, RAGE_KNOCKBACK);
        boost(Attributes.MOVEMENT_SPEED, RAGE_SPEED);
    }

    private void boost(Holder<Attribute> attribute, double amount) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.addOrUpdateTransientModifier(
                    new AttributeModifier(RAGE_ID, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private boolean busy() {
        return swinging >= 0 || leaping || heavy >= 0 || landing >= 0 || enraging >= 0;
    }

    private void tickRage(ServerLevel level) {
        if (enraging < 0) {
            if (!raging() && isAlive() && getHealth() <= getMaxHealth() * RAGE_AT && !busy() && onGround()) {
                enraging = 0;
                getNavigation().stop();
                triggerAnim(CONTROLLER, "rampage");
                playSound(SoundEvents.ZOMBIE_AMBIENT, 1.6F, 0.4F);
            }
            return;
        }
        enraging++;
        if (enraging == RAGE_ROAR) {
            enrage();
            level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.6F, 0.5F);
            VampireFx.blood(level, getX(), getY() + getBbHeight() * 0.6, getZ(), 30, 0.8, 0.2);
            dust(level, 24);
        }
        if (enraging >= RAGE_TICKS) {
            enraging = -1;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Raging", raging());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("Raging")) {
            enrage();
        }
    }

    @Override
    protected void addBehaviourGoals() {
        super.addBehaviourGoals();
        goalSelector.removeAllGoals(goal -> goal instanceof ZombieAttackGoal);
        goalSelector.addGoal(1, new LeapGoal(this));
        goalSelector.addGoal(1, new HeavyGoal(this));
        goalSelector.addGoal(2, new BrawlGoal(this));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (busy()) {
            return false;
        }
        if (!(target instanceof LivingEntity living)) {
            return super.doHurtTarget(target);
        }
        strike(living);
        return true;
    }

    private Vec3 local(Vec3 point) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 offset = point.subtract(position());
        return new Vec3(offset.x * -forward.z + offset.z * forward.x, offset.y,
                offset.x * forward.x + offset.z * forward.z);
    }

    private Vec3 toWorld(double right, double up, double ahead) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot());
        return position().add(forward.x * ahead - forward.z * right, up, forward.z * ahead + forward.x * right);
    }

    private Vec3 predicted(LivingEntity target, int lead) {
        Vec3 drift = target.getDeltaMovement().multiply(1.0, 0.0, 1.0);
        return target.position().add(drift.scale(lead));
    }

    private boolean inReach(LivingEntity target, int lead) {
        Vec3 at = local(predicted(target, lead));
        double half = target.getBbWidth() * 0.5;
        return at.z + half >= REACH_NEAR && at.z - half <= REACH_FAR
                && Math.abs(at.x) <= REACH_SIDE + half
                && at.y <= 1.8 && at.y + target.getBbHeight() >= 0.2;
    }

    private void strike(LivingEntity target) {
        Vec3 at = local(predicted(target, LEAD_TICKS));
        double half = target.getBbWidth() * 0.5;
        boolean biteFits = at.z - half <= BITE_FAR && Math.abs(at.x) <= BITE_SIDE + half;
        biting = biteFits && getRandom().nextInt(BITE_ODDS) == 0;
        boolean leftSide = Math.abs(at.x) < 0.3 ? getRandom().nextBoolean() : at.x < 0.0;
        mirrored = leftSide;
        String swing = biting ? mirrored ? "bite_left" : "bite_right" : mirrored ? "atk_left" : "atk_right";
        swingAt = target;
        swinging = 0;
        struck = false;
        face(target);
        triggerAnim(CONTROLLER, swing);
        playSound(SoundEvents.ZOMBIE_AMBIENT, 1.2F, 0.5F);
    }

    private boolean swingConnects(LivingEntity target) {
        double[][] path = biting ? BITE_PATH : PUNCH_PATH;
        int first = (biting ? BITE_FIRST : PUNCH_FIRST) + CLIP_LAG;
        int segment = swinging - first;
        if (segment < 1 || segment >= path.length) {
            return false;
        }
        double side = mirrored ? -1.0 : 1.0;
        double[] from = path[segment - 1];
        double[] to = path[segment];
        AABB body = target.getBoundingBox().inflate(biting ? BITE_RADIUS : PUNCH_RADIUS);
        Vec3 a = toWorld(from[0] * side, from[1], from[2]);
        Vec3 b = toWorld(to[0] * side, to[1], to[2]);
        return body.contains(a) || body.contains(b) || body.clip(a, b).isPresent();
    }

    private boolean swingActive() {
        int first = (biting ? BITE_FIRST : PUNCH_FIRST) + CLIP_LAG;
        int last = first + (biting ? BITE_PATH : PUNCH_PATH).length - 1;
        return swinging > first && swinging <= last;
    }

    @Override
    protected boolean isImmobile() {
        int last = (biting ? BITE_FIRST + BITE_PATH.length : PUNCH_FIRST + PUNCH_PATH.length) + CLIP_LAG;
        return super.isImmobile() || swinging > STEP_IN_UNTIL && swinging < last || heavy >= 0 && heavy < HEAVY_RECOVER
                || landing >= 0 || enraging >= 0;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return !leaping && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    public void tick() {
        if (leaping && !level().isClientSide()) {
            steerVault();
        }
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        tickRage(level);
        tickHeavy(level);
        tickWave(level);
        if (landing >= 0 && ++landing >= LAND_TICKS - LAND_SLAM_FRAME) {
            landing = -1;
        }
        if (swinging < 0) {
            return;
        }
        swinging++;
        if (swinging < TRACK_UNTIL && swingAt != null && swingAt.isAlive()) {
            face(swingAt);
        }
        if (swinging == (biting ? BITE_FIRST : PUNCH_FIRST) + CLIP_LAG + 1) {
            level.playSound(null, blockPosition(),
                    biting ? SoundEvents.EVOKER_FANGS_ATTACK : SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.HOSTILE, 1.2F, 0.6F);
        }
        if (!struck && swingActive() && swingAt != null && swingAt.isAlive() && swingConnects(swingAt)) {
            struck = true;
            land(level, swingAt);
        }
        if (swinging >= SWING_TICKS) {
            swinging = -1;
            swingAt = null;
        }
    }

    private boolean canHeavy(LivingEntity target) {
        if (busy() || !onGround() || isInWater() || isPassenger()
                || level().getGameTime() < heavyReadyAt || !target.isAlive()) {
            return false;
        }
        double flat = Math.sqrt(distanceToSqr(target.getX(), getY(), target.getZ()));
        if (flat > HEAVY_REACH || Math.abs(target.getY() - getY()) > 2.0 || !hasLineOfSight(target)) {
            return false;
        }
        if (target.isBlocking() && getRandom().nextInt(BLOCKING_HEAVY_ODDS) == 0) {
            return true;
        }
        int crowd = level().getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(HEAVY_RADIUS, 2.0, HEAVY_RADIUS),
                other -> slams(other) && (other instanceof Player || other == getTarget())).size();
        return crowd >= HEAVY_CROWD || getRandom().nextInt(HEAVY_SOLO_ODDS) == 0;
    }

    private void beginHeavy() {
        heavy = 0;
        swingAt = getTarget();
        getNavigation().stop();
        triggerAnim(CONTROLLER, "heavy_atk");
        playSound(SoundEvents.RAVAGER_ROAR, 1.2F, 0.7F);
    }

    private void tickHeavy(ServerLevel level) {
        if (heavy < 0) {
            return;
        }
        heavy++;
        if (heavy < HEAVY_AIM_UNTIL && swingAt != null && swingAt.isAlive()) {
            face(swingAt);
        }
        if (heavy == HEAVY_CONTACT) {
            quake(level);
        }
        if (heavy >= HEAVY_TICKS) {
            heavy = -1;
            swingAt = null;
            heavyReadyAt = level.getGameTime() + HEAVY_COOLDOWN;
        }
    }

    private void quake(ServerLevel level) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 at = groundAt(level, position().add(forward.scale(HEAVY_AHEAD)));
        tremor(level, at, HEAVY_RADIUS, HEAVY_WAVE_TICKS);
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 1.0F, 0.6F);
        level.playSound(null, BlockPos.containing(at), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6F, 0.5F);
        waveCentre = at;
        waveAge = 0;
        waveDamage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        caught.clear();
    }

    private void tremor(ServerLevel level, Vec3 at, double radius, int ticks) {
        NetworkHandler.broadcastGroundBreaker(level, this, at, (float) radius, GroundBreakerFxPacket.SLAM, ticks);
        BlockState ground = level.getBlockState(BlockPos.containing(at).below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    at.x, at.y + 0.1, at.z, (int) (radius * 12), radius * 0.5, 0.1, radius * 0.5, 0.25);
        }
    }

    private static Vec3 groundAt(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        for (int down = 0; down <= 3; down++) {
            BlockPos below = pos.below(down + 1);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                return new Vec3(at.x, below.getY() + 1.0, at.z);
            }
        }
        return at;
    }

    private void tickWave(ServerLevel level) {
        Vec3 centre = waveCentre;
        if (centre == null) {
            return;
        }
        double was = HEAVY_RADIUS * (waveAge / (double) HEAVY_WAVE_TICKS);
        waveAge++;
        double now = HEAVY_RADIUS * (waveAge / (double) HEAVY_WAVE_TICKS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(centre.subtract(now, 1.5, now), centre.add(now, 2.5, now)), this::slams)) {
            double away = Math.sqrt(target.distanceToSqr(centre.x, target.getY(), centre.z));
            if (away > now || away < was - 0.5 || !caught.add(target.getUUID())) {
                continue;
            }
            if (target.hurt(damageSources().mobAttack(this), waveDamage)) {
                Stun.stagger(target);
            }
            Vec3 out = new Vec3(target.getX() - centre.x, 0.0, target.getZ() - centre.z);
            out = out.lengthSqr() < 1.0E-6 ? Vec3.ZERO : out.normalize().scale(HEAVY_SHOVE);
            target.setDeltaMovement(out.x, HEAVY_LAUNCH, out.z);
            target.hurtMarked = true;
        }
        if (waveAge >= HEAVY_WAVE_TICKS) {
            waveCentre = null;
            caught.clear();
        }
    }

    private void face(Entity target) {
        lookAt(target, 30.0F, 30.0F);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
    }

    private void land(ServerLevel level, LivingEntity target) {
        if (super.doHurtTarget(target)) {
            VampireFx.blood(level, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                    biting ? 18 : 12, 0.3, 0.1);
        }
    }

    private boolean canLeapAt(LivingEntity target) {
        if (busy() || !onGround() || isInWater() || isPassenger()
                || level().getGameTime() < leapReadyAt || !target.isAlive()) {
            return false;
        }
        double rise = target.getY() - getY();
        double flat = Math.sqrt(distanceToSqr(target.getX(), getY(), target.getZ()));
        return flat >= (cornered ? CORNERED_LEAP_MIN : LEAP_MIN) && flat <= LEAP_MAX && Math.abs(rise) <= LEAP_RISE_MAX
                && hasLineOfSight(target);
    }

    private void launch(LivingEntity target) {
        Vec3 from = position();
        Vec3 aim = target.position().add(target.getDeltaMovement().multiply(LEAP_FLIGHT * 0.5, 0.0, LEAP_FLIGHT * 0.5));
        Vec3 flat = new Vec3(aim.x - from.x, 0.0, aim.z - from.z);
        double reach = flat.horizontalDistance();
        if (reach > VAULT_REACH_MAX) {
            flat = flat.scale(VAULT_REACH_MAX / reach);
            reach = VAULT_REACH_MAX;
        }
        double rise = aim.y - from.y;
        vaultFrom = from;
        vaultTo = from.add(flat.x, rise, flat.z);
        vaultArc = Mth.clamp(reach * VAULT_ARC, VAULT_ARC_MIN, VAULT_ARC_MAX) + Math.max(0.0, rise);
        vaultTick = 0;
        if (reach > 1.0E-4) {
            float yaw = (float) (Mth.atan2(flat.z, flat.x) * Mth.RAD_TO_DEG) - 90.0F;
            setYRot(yaw);
            setYBodyRot(yaw);
            setYHeadRot(yaw);
        }
        leaping = true;
        steerVault();
        if (level() instanceof ServerLevel level) {
            dust(level, 14);
            level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.9F, 1.4F);
        }
    }

    private void steerVault() {
        Vec3 from = vaultFrom;
        Vec3 to = vaultTo;
        if (from == null || to == null || isInWater()) {
            return;
        }
        vaultTick++;
        setZza(0.0F);
        setXxa(0.0F);
        Vec3 next;
        if (vaultTick <= LEAP_FLIGHT) {
            double t = vaultTick / (double) LEAP_FLIGHT;
            Vec3 along = from.lerp(to, t);
            next = along.add(0.0, vaultArc * hump(t), 0.0);
        } else {
            next = position().add(0.0, -VAULT_DROP, 0.0);
        }
        setDeltaMovement(next.subtract(position()));
        hurtMarked = true;
    }

    private static double hump(double t) {
        if (t <= VAULT_PEAK_AT) {
            return Math.sin(Math.PI * 0.5 * t / VAULT_PEAK_AT);
        }
        return Math.cos(Math.PI * 0.5 * (t - VAULT_PEAK_AT) / (1.0 - VAULT_PEAK_AT));
    }

    private void endVault() {
        leaping = false;
        vaultFrom = null;
        vaultTo = null;
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
    }

    private void slam(@Nullable LivingEntity target, boolean landed) {
        endVault();
        landing = landed ? 0 : -1;
        if (!landed) {
            stopTriggeredAnim(CONTROLLER, "leap");
        }
        resetFallDistance();
        leapReadyAt = level().getGameTime() + LEAP_COOLDOWN;
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        dust(level, 28);
        tremor(level, position(), SLAM_RADIUS, LEAP_WAVE_TICKS);
        level.playSound(null, blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.4F, 0.5F);
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * SLAM_SHARE;
        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().inflate(SLAM_RADIUS, 0.5, SLAM_RADIUS), this::slams)) {
            if (hit.hurt(damageSources().mobAttack(this), damage)) {
                Vec3 push = hit.position().subtract(position()).multiply(1.0, 0.0, 1.0);
                if (push.lengthSqr() > 1.0E-4) {
                    push = push.normalize();
                    hit.knockback(0.6, -push.x, -push.z);
                }
            }
        }
        if (target != null && target.isAlive()
                && getAttackBoundingBox().inflate(SWING_REACH).intersects(target.getBoundingBox())) {
            biting = false;
            land(level, target);
        }
    }

    private boolean slams(LivingEntity other) {
        return other != this && other.isAlive() && !(other instanceof RisenZombie)
                && !other.getUUID().equals(master()) && !sharesMasterWith(other) && !isAlliedTo(other);
    }

    private void dust(ServerLevel level, int count) {
        BlockPos below = blockPosition().below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    getX(), getY() + 0.1, getZ(), count, 0.9, 0.05, 0.9, 0.15);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        swinging = -1;
        swingAt = null;
        endVault();
        heavy = -1;
        landing = -1;
        enraging = -1;
        waveCentre = null;
        stopTriggeredAnim(CONTROLLER, null);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DEATH_TICKS && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Amalgamation> controller =
                new AnimationController<>(this, CONTROLLER, TRANSITION_TICKS, this::animate);
        for (String swing : SWINGS) {
            controller.triggerableAnim(swing, RawAnimation.begin().then(swing, Animation.LoopType.PLAY_ONCE));
        }
        controller.triggerableAnim("heavy_atk", HEAVY);
        controller.triggerableAnim("leap", LEAP);
        controller.triggerableAnim("land", LAND);
        controller.triggerableAnim("rampage", RAMPAGE);
        controllers.add(controller);
    }

    private PlayState animate(AnimationState<Amalgamation> state) {
        if (isDeadOrDying()) {
            return state.setAndContinue((getUUID().getLeastSignificantBits() & 1L) == 0L ? DEATH : DEATH_VARIANT);
        }
        return state.setAndContinue(state.isMoving() ? raging() ? RUN : WALK : IDLE);
    }

    private static final class HeavyGoal extends Goal {
        private final Amalgamation mob;

        HeavyGoal(Amalgamation mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity current = mob.getTarget();
            return current != null && mob.canHeavy(current);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            mob.beginHeavy();
        }
    }

    private static final class BrawlGoal extends Goal {
        private final Amalgamation mob;
        private int repath;
        private int stuck;
        private int rest;

        BrawlGoal(Amalgamation mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            return target != null && target.isAlive() && target.canBeSeenAsEnemy() && !mob.isPassenger();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            repath = 0;
            stuck = 0;
            rest = 0;
            mob.setAggressive(true);
        }

        @Override
        public void stop() {
            mob.setAggressive(false);
            mob.cornered = false;
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = mob.getTarget();
            if (target == null) {
                return;
            }
            mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (mob.leaping || mob.heavy >= 0 || mob.landing >= 0 || mob.enraging >= 0) {
                return;
            }
            if (mob.swinging >= 0) {
                if (mob.swinging <= STEP_IN_UNTIL && !mob.inReach(target, 0)) {
                    mob.getNavigation().moveTo(target, 0.6);
                } else {
                    mob.getNavigation().stop();
                }
                return;
            }
            if (rest > 0) {
                rest--;
            }
            if (mob.inReach(target, LEAD_TICKS) && mob.hasLineOfSight(target)) {
                mob.getNavigation().stop();
                stuck = 0;
                mob.cornered = false;
                if (rest == 0) {
                    mob.strike(target);
                    rest = (mob.raging() ? RAGE_REST_TICKS : REST_TICKS) + mob.getRandom().nextInt(6);
                }
                return;
            }
            if (--repath > 0) {
                return;
            }
            double far = mob.distanceToSqr(target);
            repath = far > 256.0 ? 12 : far > 64.0 ? 8 : 4;
            repath += mob.getRandom().nextInt(3);
            Path path = mob.getNavigation().createPath(target, 0);
            if (path != null && path.canReach()) {
                stuck = 0;
                mob.cornered = false;
            } else {
                stuck += repath;
                mob.cornered = stuck >= STUCK_TICKS;
            }
            if (path != null) {
                mob.getNavigation().moveTo(path, 1.0);
            }
        }
    }

    private static final class LeapGoal extends Goal {
        private final Amalgamation mob;
        private int ticks;
        private boolean launched;
        private boolean landed;

        @Nullable
        private LivingEntity target;

        LeapGoal(Amalgamation mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity current = mob.getTarget();
            return current != null && mob.canLeapAt(current);
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && mob.isAlive() && (launched || target.isAlive()) && ticks <= LEAP_LAUNCH + LEAP_AIR_MAX;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            target = mob.getTarget();
            ticks = 0;
            launched = false;
            landed = false;
            mob.cornered = false;
            mob.getNavigation().stop();
            mob.triggerAnim(CONTROLLER, "leap");
            mob.playSound(SoundEvents.ZOMBIE_AMBIENT, 1.4F, 0.4F);
        }

        @Override
        public void tick() {
            ticks++;
            if (target == null) {
                return;
            }
            if (!launched) {
                mob.getNavigation().stop();
                mob.face(target);
                if (ticks >= LEAP_LAUNCH) {
                    if (!mob.onGround()) {
                        target = null;
                        return;
                    }
                    mob.launch(target);
                    launched = true;
                }
                return;
            }
            if (!landed && ticks >= LEAP_LAUNCH + LAND_LEAD) {
                mob.triggerAnim(CONTROLLER, "land");
                landed = true;
            }
            if (ticks > LEAP_LAUNCH + 2 && (mob.onGround() || mob.isInWater())) {
                mob.slam(target, landed);
                target = null;
            }
        }

        @Override
        public void stop() {
            if (mob.leaping) {
                mob.endVault();
                mob.leapReadyAt = mob.level().getGameTime() + LEAP_COOLDOWN;
            } else if (!launched) {
                mob.leapReadyAt = mob.level().getGameTime() + LEAP_COOLDOWN / 2;
                mob.stopTriggeredAnim(CONTROLLER, "leap");
            }
            target = null;
            launched = false;
        }
    }
}
