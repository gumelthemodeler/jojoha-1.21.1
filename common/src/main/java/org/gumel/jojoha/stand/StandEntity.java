package org.gumel.jojoha.stand;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandPlacement;
import org.gumel.jojoha.stand.chariot.Chariot;
import org.gumel.jojoha.stand.combat.BarrageGenerator;
import org.gumel.jojoha.stand.combat.StandCombatProfile;
import org.gumel.jojoha.stand.combat.StandFist;
import org.gumel.jojoha.stand.combat.StandFists;
import org.gumel.jojoha.stand.magician.Magician;
import org.gumel.jojoha.stand.workshop.BarrageShout;
import org.gumel.jojoha.stand.workshop.RestPose;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.DoubleSupplier;

public final class StandEntity extends LivingEntity implements GeoEntity {

    public static final String CONTROLLER = "controller";
    static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    static final RawAnimation PUNCH = RawAnimation.begin().thenPlay("punch");
    static final RawAnimation PUNCH2 = RawAnimation.begin().thenPlay("punch2");

    static final RawAnimation BARRAGE = RawAnimation.begin().thenLoop("barrage");

    static final RawAnimation BLOCK = RawAnimation.begin().thenLoop("block");

    static final RawAnimation UPPERCUT = RawAnimation.begin().thenPlay("uppercut");

    static final RawAnimation KICK = RawAnimation.begin().thenPlay("kick");

    static final RawAnimation TW_TIMESTOP = RawAnimation.begin().thenPlay("tw_timestop");

    static final RawAnimation[] KNIFE_VOLLEYS = knifeVolleys();

    private static RawAnimation[] knifeVolleys() {
        return StandBarrage.knifeVolleys();
    }

    static final RawAnimation MR_RED_BIND = RawAnimation.begin().then("red_bind", Animation.LoopType.PLAY_ONCE);

    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_MOB_OWNER =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_STAND_TYPE =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_TRUST_TIER =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_DISMISS_START_TICK =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);

    static final EntityDataAccessor<Boolean> DATA_FRONT_STANCE =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);

    static final EntityDataAccessor<Boolean> DATA_STRIKING =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);

    static final EntityDataAccessor<Boolean> DATA_COMBAT_STANCE =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);

    static final EntityDataAccessor<Boolean> DATA_HELD_BARRAGE =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> DATA_CHARGING =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> DATA_FALLING =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);

    static final EntityDataAccessor<Boolean> DATA_POSING =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> DATA_INHALING =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);

    static final EntityDataAccessor<Integer> DATA_KNIVES =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);

    static final EntityDataAccessor<String> DATA_KNIFE_KINDS =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.STRING);

    static final EntityDataAccessor<Boolean> DATA_KICKING =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);

    static final EntityDataAccessor<Boolean> DATA_PEAK =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);

    static final EntityDataAccessor<Integer> DATA_FLOW =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);

    static final EntityDataAccessor<Boolean> DATA_HASTE =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);

    static final EntityDataAccessor<Byte> DATA_HOLD_POSE =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BYTE);

    public static final byte HOLD_NONE = 0;
    public static final byte HOLD_GRAB = 1;
    public static final byte HOLD_GRAB2 = 2;
    public static final byte HOLD_SABRE = 3;
    public static final byte HOLD_DEFLECT = 4;
    public static final byte HOLD_RIPOSTE = 5;
    public static final byte HOLD_FETCH = 6;

    private static final EntityDataAccessor<Boolean> DATA_ARMOR_OFF =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_AFTERIMAGES =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BLADE_OUT =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Vector3f> DATA_PLACEMENT =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> DATA_PLACEMENT_TURN =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Integer> DATA_FINGER_TARGET =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Vector3f> DATA_FINGER_POINT =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DATA_IDLE =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LOOK =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);
    private static final int LOOK_SHOUT_SHIFT = 4;

    private byte chariotHold = HOLD_NONE;
    private int holdReplayTicks;

    static final EntityDataAccessor<Float> DATA_GUARD_STRAIN =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.FLOAT);

    static final EntityDataAccessor<Integer> DATA_GUARD_BROKEN =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);

    static final EntityDataAccessor<Integer> DATA_GUARD_THREAT =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);

    static final EntityDataAccessor<Boolean> DATA_GUARDING =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ENGAGED =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> DATA_BARRAGING =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PILOTED =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.BOOLEAN);

    static final EntityDataAccessor<CompoundTag> DATA_CAUGHT =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.COMPOUND_TAG);
    static final EntityDataAccessor<ItemStack> DATA_HELD_ITEM =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DATA_SKIN =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SKIN_SWAP_TICK =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> DATA_MANIFEST_FOR =
            SynchedEntityData.defineId(StandEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private static final int SPAWN_FADE_TICKS = 30;
    private static final int DISMISS_FADE_TICKS = 12;

    private static final float EMERGING_PULSE_FLOOR = 0.65F;
    private static final double EMERGING_PULSE_SPEED = 0.22;

    private static final float DORMANT_PREVIEW_ALPHA = 0.35F;

    static final double UTILITY_STEP_SPEED = 1.1;

    static final int GUARD_THREAT_MEMORY = 60;

    static final int PUNCH_STANCE_TICKS = 12;

    static final int STRIKE_WAIT_TIMEOUT_TICKS = 10;

    private static final double PURSUIT_ABANDON_RANGE = 9.0;
    private static final double DEFENSE_ABANDON_RANGE = 18.0;

    public static final double BARRAGE_PULL_IN = 1.1;

    public static double barrageReach() {
        return StandBarrage.barrageReach();
    }

    static final double ENGAGE_RANGE = 1.6;

    static final double AUTHORED_CONTACT_TICKS = 15.83;

    public static final double ATTACK_ANIM_SPEED = 1.6;

    public static final int STRIKE_CONTACT_TICKS =
            (int) Math.round(AUTHORED_CONTACT_TICKS / ATTACK_ANIM_SPEED);

    public static final double JAB_ANIM_SPEED = 2.6;

    static final int BARRAGE_HITS = 52;
    static final int BARRAGE_HIT_INTERVAL = 3;
    private static final float BARRAGE_HIT_DAMAGE = 0.5F;

    private static final int KICK_HITS = 40;
    public static final int KICK_HIT_INTERVAL = 4;
    private static final float KICK_HIT_DAMAGE = 0.9F;
    private static final double KICK_PUSH = 0.18;
    private static final double KICK_LIFT = 0.12;

    public static final int SWORD_HITS = 50;
    public static final int SWORD_HIT_INTERVAL = 3;
    public static final float SWORD_HIT_DAMAGE = 0.8F;
    private static final double SWORD_PUSH = 0.06;

    public enum BarrageStyle {
        PUNCHES(BARRAGE_HITS, BARRAGE_HIT_INTERVAL, BARRAGE_HIT_DAMAGE, "barrage_hit", 0.0, 0.0),
        KICKS(KICK_HITS, KICK_HIT_INTERVAL, KICK_HIT_DAMAGE, "kick_flurry_hit", KICK_PUSH, KICK_LIFT),
        SWORD(SWORD_HITS, SWORD_HIT_INTERVAL, SWORD_HIT_DAMAGE, "sword_flurry_hit", SWORD_PUSH, 0.0);

        final int hits;
        final int interval;
        final float damage;
        final String tuningKey;
        final double push;
        final double lift;

        BarrageStyle(int hits, int interval, float damage, String tuningKey, double push, double lift) {
            this.hits = hits;
            this.interval = interval;
            this.damage = damage;
            this.tuningKey = tuningKey;
            this.push = push;
            this.lift = lift;
        }
    }

    BarrageStyle barrageStyle = BarrageStyle.PUNCHES;

    final StandFists fists = new StandFists();

    final StandBarrage barrage = new StandBarrage(this);
    final StandStrikes strikes = new StandStrikes(this);
    final StandGuard guard = new StandGuard(this);
    final StandMovement movement = new StandMovement(this);
    final StandPoses poses = new StandPoses(this);
    final StandHolding holding = new StandHolding(this);
    final StandAnimations animations = new StandAnimations(this);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    boolean spawnAnimationTriggered = false;

    float swayStrength = 0F;

    boolean guarding = false;

    LivingEntity guardThreat;
    int guardThreatTicks;
    int counterCooldownTicks;
    int punchStanceTicksRemaining = 0;

    static final int COMBAT_HOLD_TICKS = 50;
    static final float FALL_POSE_FROM = 2.5F;

    int combatHoldTicks = 0;

    private static final double GRAB_HAND_FORWARD = 0.55;
    private static final double GRAB_HAND_SIDE = 0.35;
    private static final double GRAB_HAND_HEIGHT = 1.15;

    private static final double CRADLE_FORWARD = 0.3;
    private static final double CRADLE_HEIGHT = 1.0;
    private static final double CRADLE_SINK = 0.3;

    private static final double OWNER_SEARCH_RADIUS = 48.0;

    final Map<Integer, Float> barrageDealt = new HashMap<>();

    private void tickBarrageCap() {
        barrage.tickBarrageCap();
    }

    void stopFlurryAnimation() {
        barrage.stopFlurryAnimation();
    }

    StandGrip grip;
    boolean gripCarried;

    int heldItemTicks = 0;

    Vec3 utilityAnchor;

    LivingEntity pursuitTarget = null;

    boolean pursuitUsePunch2 = false;

    public static final double CHARIOT_HIT_ANIM_SPEED = 0.8;

    boolean huntingSelfDirected = false;
    int retargetDelay = 0;
    private boolean previewMode = false;
    private final StandLimbFlow limbFlow = new StandLimbFlow();
    private Vec3 pilotVelocity = Vec3.ZERO;

    private boolean locallyPiloted;

    public Vec3 pilotVelocity() {
        return pilotVelocity;
    }

    public void setPilotVelocity(Vec3 velocity) {
        this.pilotVelocity = velocity;
    }

    public void setLocallyPiloted(boolean value) {
        this.locallyPiloted = value;
    }

    boolean returningFromPursuit = false;

    int attackCooldown = 0;
    int attacksThisEngagement = 0;
    int barrageHitsRemaining = 0;
    int barrageHitInterval = 0;
    int stationaryBarrageTicks = 0;

    int inhaleHoldTicks = 0;
    Vec3 inhaleAnchor;

    ComboStage comboStage = ComboStage.NONE;
    int comboStageTicks = 0;

    byte comboHoldPose = HOLD_GRAB;
    PendingStrike pendingStrike = null;

    int strikingTicks;
    double orbitAngle = 0.0;

    <T> T synced(EntityDataAccessor<T> key) {
        return this.entityData.get(key);
    }

    <T> void sync(EntityDataAccessor<T> key, T value) {
        this.entityData.set(key, value);
    }

    public StandEntity(EntityType<? extends StandEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        this.blocksBuilding = false;
        this.setInvisible(true);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide() || isRemoved()) {
            return false;
        }

        if (endsTheForm(source)) {
            breakForm();
            return true;
        }
        if (isInvulnerableTo(source)) {
            return false;
        }
        if (!StandHits.isStandAttack(source, this)) {
            return false;
        }

        absorbStrain(amount);
        boolean landed = StandHits.forward(this, source, amount);
        if (landed) {
            this.level().broadcastDamageEvent(this, source);
        }
        return landed;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    public void absorbStrain(float amount) {
        guard.absorbStrain(amount);
    }

    private static boolean endsTheForm(DamageSource source) {
        return source.is(DamageTypes.GENERIC_KILL)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (endsTheForm(source)) {
            return false;
        }
        return isManifesting() || source.getEntity() == null || source.getEntity() == getUser()
                || super.isInvulnerableTo(source);
    }

    private void breakForm() {
        collapse();
        if (!isRemoved() && !isDismissing()) {
            discard();
        }
    }

    @Override
    public void die(DamageSource source) {
        setHealth(getMaxHealth());
        if (!this.level().isClientSide()) {
            breakForm();
        }
    }

    @Override
    protected void tickDeath() {
        setHealth(getMaxHealth());
        if (!this.level().isClientSide()) {
            breakForm();
        }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    public void setRemainingFireTicks(int ticks) {
        super.setRemainingFireTicks(0);
    }

    @Override
    public void lavaHurt() {
    }

    @Override
    public void thunderHit(ServerLevel level,
                           LightningBolt bolt) {
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public void setTicksFrozen(int ticks) {
        super.setTicksFrozen(0);
    }

    @Override
    public void setAirSupply(int air) {
        super.setAirSupply(getMaxAirSupply());
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motion) {
    }

    @Override
    public void onInsideBubbleColumn(boolean down) {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier,
                                   DamageSource source) {
        return false;
    }

    private void collapse() {
        if (!(getOwner() instanceof ServerPlayer serverOwner)) {
            discard();
            return;
        }

        StandSummonHandler.collapse(serverOwner);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER, Optional.empty());
        builder.define(DATA_MOB_OWNER, -1);
        builder.define(DATA_STAND_TYPE, "");
        builder.define(DATA_TRUST_TIER, TrustTier.BONDED.level());
        builder.define(DATA_DISMISS_START_TICK, -1);
        builder.define(DATA_MODE, StandMode.ANALOG.ordinal());
        builder.define(DATA_BARRAGING, false);
        builder.define(DATA_PILOTED, false);
        builder.define(DATA_FRONT_STANCE, false);
        builder.define(DATA_STRIKING, false);
        builder.define(DATA_COMBAT_STANCE, false);
        builder.define(DATA_HELD_BARRAGE, false);
        builder.define(DATA_GUARDING, false);
        builder.define(DATA_GUARD_THREAT, -1);
        builder.define(DATA_GUARD_STRAIN, 0F);
        builder.define(DATA_CHARGING, false);
        builder.define(DATA_FALLING, false);
        builder.define(DATA_POSING, false);
        builder.define(DATA_INHALING, false);
        builder.define(DATA_KNIVES, 0);
        builder.define(DATA_KNIFE_KINDS, "");
        builder.define(DATA_KICKING, false);
        builder.define(DATA_PEAK, false);
        builder.define(DATA_FLOW, 0);
        builder.define(DATA_HASTE, false);
        builder.define(DATA_HOLD_POSE, HOLD_NONE);
        builder.define(DATA_GUARD_BROKEN, 0);
        builder.define(DATA_ENGAGED, false);
        builder.define(DATA_HELD_ITEM, ItemStack.EMPTY);
        builder.define(DATA_CAUGHT, new CompoundTag());
        builder.define(DATA_SKIN, 0);
        builder.define(DATA_SKIN_SWAP_TICK, -1);
        builder.define(DATA_MANIFEST_FOR, Optional.empty());
        builder.define(DATA_ARMOR_OFF, false);
        builder.define(DATA_AFTERIMAGES, 0);
        builder.define(DATA_BLADE_OUT, false);
        builder.define(DATA_PLACEMENT, new Vector3f());
        builder.define(DATA_PLACEMENT_TURN, 0F);
        builder.define(DATA_IDLE, 0);
        builder.define(DATA_LOOK, 0);
        builder.define(DATA_FINGER_TARGET, -1);
        builder.define(DATA_FINGER_POINT, new Vector3f());
    }

    public void setPlacement(StandPlacement placement) {
        this.entityData.set(DATA_PLACEMENT, new Vector3f(placement.side(), placement.height(), placement.ahead()));
        this.entityData.set(DATA_PLACEMENT_TURN, placement.turn());
    }

    public Vector3f placement() {
        return this.entityData.get(DATA_PLACEMENT);
    }

    public float placementTurn() {
        return this.entityData.get(DATA_PLACEMENT_TURN);
    }

    public void setLook(int rest, int shout) {
        this.entityData.set(DATA_LOOK, (shout << LOOK_SHOUT_SHIFT) | (rest & ((1 << LOOK_SHOUT_SHIFT) - 1)));
    }

    public RestPose restPose() {
        return RestPose.of(this.entityData.get(DATA_LOOK) & ((1 << LOOK_SHOUT_SHIFT) - 1));
    }

    public BarrageShout barrageShout() {
        return BarrageShout.of(this.entityData.get(DATA_LOOK) >> LOOK_SHOUT_SHIFT);
    }

    public void setIdleClip(int stamp) {
        this.entityData.set(DATA_IDLE, stamp);
    }

    public int idleClip() {
        return this.entityData.get(DATA_IDLE);
    }

    boolean restingBehindOwner() {
        return !this.entityData.get(DATA_GUARDING) && !this.entityData.get(DATA_FRONT_STANCE)
                && !this.entityData.get(DATA_COMBAT_STANCE)
                && !StandMode.fromOrdinal(this.entityData.get(DATA_MODE)).handlesItems();
    }

    public boolean armorOff() {
        return this.entityData.get(DATA_ARMOR_OFF);
    }

    public void setArmorOff(boolean off) {
        this.entityData.set(DATA_ARMOR_OFF, off);
    }

    public int afterimages() {
        return this.entityData.get(DATA_AFTERIMAGES);
    }

    public void setAfterimages(int count) {
        this.entityData.set(DATA_AFTERIMAGES, Math.max(0, count));
    }

    public boolean bladeOut() {
        return this.entityData.get(DATA_BLADE_OUT);
    }

    boolean rapierless() {
        return isChariot() && bladeOut();
    }

    public void setBladeOut(boolean out) {
        this.entityData.set(DATA_BLADE_OUT, out);
    }

    public byte holdPose() {
        return this.entityData.get(DATA_HOLD_POSE);
    }

    public void holdChariotPose(byte pose) {
        this.chariotHold = pose;
    }

    public byte chariotHold() {
        return chariotHold;
    }

    public void replayHold() {
        this.holdReplayTicks = 1;
    }

    boolean isChariot() {
        return Chariot.is(getStandType());
    }

    boolean isMagician() {
        return Magician.is(getStandType());
    }

    public static final int SKIN_SWAP_WHITE_RISE_TICKS = 9;

    public void setSkin(int skin) {
        this.entityData.set(DATA_SKIN, Math.max(0, skin));
    }

    public int getSkin() {
        return this.entityData.get(DATA_SKIN);
    }

    public void beginSkinSwap() {
        this.entityData.set(DATA_SKIN_SWAP_TICK, this.tickCount);
    }

    public float skinSwapWhite(float partialTick) {
        int start = this.entityData.get(DATA_SKIN_SWAP_TICK);
        if (start < 0) {
            return 0F;
        }

        float progress = Mth.clamp((this.tickCount - start + partialTick) / SKIN_SWAP_WHITE_RISE_TICKS, 0F, 1F);
        return progress * progress;
    }

    public void manifestFor(Player user) {
        this.entityData.set(DATA_MANIFEST_FOR, Optional.of(user.getUUID()));
    }

    public boolean isManifesting() {
        return this.entityData.get(DATA_MANIFEST_FOR).isPresent();
    }

    @Override
    public boolean isCurrentlyGlowing() {
        if (isManifesting()) {
            return StandManifestation.outlined(this.tickCount);
        }
        return super.isCurrentlyGlowing();
    }

    public Player manifestUser() {
        return this.entityData.get(DATA_MANIFEST_FOR).map(uuid -> this.level().getPlayerByUUID(uuid)).orElse(null);
    }

    public void beginDismissal() {
        if (!isDismissing()) {
            this.entityData.set(DATA_DISMISS_START_TICK, this.tickCount);
        }
    }

    public boolean isDismissing() {
        return this.entityData.get(DATA_DISMISS_START_TICK) >= 0;
    }

    float dismissProgress() {
        int start = this.entityData.get(DATA_DISMISS_START_TICK);
        if (start < 0) {
            return 0F;
        }
        return Mth.clamp((this.tickCount - start) / (float) DISMISS_FADE_TICKS, 0F, 1F);
    }

    public void setTrustTier(TrustTier tier) {
        this.entityData.set(DATA_TRUST_TIER, tier.level());
    }

    public TrustTier getTrustTier() {
        return TrustTier.fromLevel(this.entityData.get(DATA_TRUST_TIER));
    }

    public boolean hasOutlivedEmergingWindow() {
        return getTrustTier().isTimeLimited() && this.tickCount >= TrustTier.EMERGING_DURATION_TICKS;
    }

    public StandLimbFlow getLimbFlow() {
        return limbFlow;
    }

    public boolean isPreviewMode() {
        return previewMode;
    }

    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
    }

    public void setOwner(Player owner) {
        this.entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
    }

    public Optional<UUID> getOwnerUuid() {
        return this.entityData.get(DATA_OWNER);
    }

    public Player getOwner() {
        return this.entityData.get(DATA_OWNER).map(uuid -> this.level().getPlayerByUUID(uuid)).orElse(null);
    }

    public void setMobOwner(Mob mob) {
        this.entityData.set(DATA_MOB_OWNER, mob.getId());
    }

    public void setMobBarraging(boolean barraging) {
        this.entityData.set(DATA_BARRAGING, barraging);
    }

    public boolean isMobStand() {
        return this.entityData.get(DATA_MOB_OWNER) >= 0;
    }

    public Mob getMobOwner() {
        int id = this.entityData.get(DATA_MOB_OWNER);
        return id >= 0 && this.level().getEntity(id) instanceof Mob mob ? mob : null;
    }

    public LivingEntity getUser() {
        return isMobStand() ? getMobOwner() : getOwner();
    }

    public static StandEntity findFor(Player owner) {
        if (owner == null) {
            return null;
        }
        for (StandEntity stand : owner.level().getEntitiesOfClass(StandEntity.class,
                owner.getBoundingBox().inflate(OWNER_SEARCH_RADIUS))) {
            if (stand.getOwner() == owner) {
                return stand;
            }
        }
        return null;
    }

    public void setStandType(ResourceLocation id) {
        this.entityData.set(DATA_STAND_TYPE, id.toString());
    }

    public float getRenderAlpha() {
        if (isManifesting()) {
            return 1F;
        }

        float alpha = Mth.clamp(this.tickCount / (float) SPAWN_FADE_TICKS, 0F, 1F);

        if (isDismissing()) {
            return alpha * (1F - dismissProgress());
        }

        if (previewMode && getTrustTier() == TrustTier.DORMANT) {
            return alpha * DORMANT_PREVIEW_ALPHA;
        }

        if (!getTrustTier().isTimeLimited()) {
            return alpha;
        }

        if (StandShadowForm.of(this) != null) {
            return alpha;
        }

        float pulse = EMERGING_PULSE_FLOOR + (1F - EMERGING_PULSE_FLOOR)
                * (float) ((Math.sin(this.tickCount * EMERGING_PULSE_SPEED) + 1.0) * 0.5);
        return alpha * pulse;
    }

    public StandType getStandType() {
        String raw = this.entityData.get(DATA_STAND_TYPE);
        ResourceLocation id = raw.isEmpty() ? null : ResourceLocation.tryParse(raw);
        StandType type = id == null ? null : StandTypes.byId(id);
        return type != null ? type : StandTypes.byId(StandTypes.STAR_PLATINUM_ID);
    }

    @Override
    public void tick() {
        Vec3 before = this.position();
        float yawBefore = this.yBodyRot;
        tickStand();
        if (this.level().isClientSide()) {
            glide(before, yawBefore);
        }
    }

    static final double GLIDE_JUMP = 1.25;

    private void glide(Vec3 before, float yawBefore) {
        movement.glide(before, yawBefore);
    }

    public Vec3 glideOffset(float partialTick) {
        return movement.glideOffset(partialTick);
    }

    public float glideYaw(float partialTick) {
        return movement.glideYaw(partialTick);
    }

    public boolean isStriking() {
        return this.entityData.get(DATA_STRIKING);
    }

    private void tickStand() {
        super.tick();
        if (isManifesting()) {
            StandManifestation.tick(this);
            return;
        }
        tickTimeStopPose();
        tickChargingStance();

        if (isMobStand()) {
            tickMobStand();
            return;
        }

        Player owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            if (!this.level().isClientSide()) {
                this.discard();
            }
            return;
        }

        boolean partial = getTrustTier().isPartialManifestation();

        if (this.level().isClientSide()) {
            if (isPiloted()) {
                setYRot(owner.getYRot());
                setXRot(owner.getXRot());
                setYHeadRot(owner.getYRot());
            } else if (partial) {
                lockToOwner(owner);
            } else if (isClientPredictingFollow()) {
                tickFollow(owner);
            }

            limbFlow.tick(getX() - xo, getZ() - zo, getYRot());
            return;
        }

        if (isDismissing() && dismissProgress() >= 1F) {
            this.discard();
            return;
        }

        if (!isDismissing() && this.tickCount > 1
                && !this.getUUID().equals(PlayerDataAccess.get(owner).summonedStandEntityUuid)) {
            this.discard();
            return;
        }

        if (!spawnAnimationTriggered) {
            spawnAnimationTriggered = true;
            Jojoha.LOGGER.info("StandEntity {} triggering spawn animation on tick {}", this.getId(), this.tickCount);
            this.setInvisible(false);
            this.triggerAnim(CONTROLLER, StandTypes.spawnAnimation(this.getStandType()));
        }

        if (punchStanceTicksRemaining > 0) {
            punchStanceTicksRemaining--;
        }
        if (combatHoldTicks > 0) {
            combatHoldTicks--;
        }
        tickGrip();
        tickHeldBarrage();
        tickBarrageCap();
        poses.tickCatch();

        if (heldItemTicks > 0 && --heldItemTicks <= 0 && !this.level().isClientSide()) {
            this.entityData.set(DATA_HELD_ITEM, ItemStack.EMPTY);
        }

        if (stationaryBarrageTicks > 0) {
            stationaryBarrageTicks--;
        }

        if (inhaleHoldTicks > 0 && --inhaleHoldTicks <= 0) {
            inhaleAnchor = null;
            this.stopTriggeredAnim(CONTROLLER, "inhale");
        }

        if (attackCooldown > 0) {
            attackCooldown--;
        }
        tickPendingStrike(owner);
        tickStrikeContact();
        tickBarrage(owner);

        double abandonRange = huntingSelfDirected ? DEFENSE_ABANDON_RANGE : PURSUIT_ABANDON_RANGE;
        if (pursuitTarget != null && (!pursuitTarget.isAlive() || pursuitTarget.distanceTo(owner) > abandonRange)) {
            endEngagement();
        }

        if (retargetDelay > 0) {
            retargetDelay--;
        }

        if (guardThreatTicks > 0 && --guardThreatTicks <= 0) {
            guardThreat = null;
        }

        if (counterCooldownTicks > 0) {
            counterCooldownTicks--;
        }

        int brokenTicks = this.entityData.get(DATA_GUARD_BROKEN);
        if (brokenTicks > 0) {
            this.entityData.set(DATA_GUARD_BROKEN, brokenTicks - 1);
        }

        if (guarding) {
            pushBackFromGuard(owner);
        }

        if (guarding && guardThreat == null) {
            LivingEntity nearby = findNearbyHostile(owner);
            if (nearby != null) {
                guardThreat = nearby;
                guardThreatTicks = GUARD_THREAT_MEMORY;
            }
        }

        if (pursuitTarget == null && retargetDelay <= 0 && stationaryBarrageTicks <= 0
                && inhaleHoldTicks <= 0
                && getMode().isAutonomous() && getTrustTier().canActAtRange()) {
            LivingEntity hostile = findNearbyHostile(owner);
            if (hostile != null) {
                pursueAndPunch(hostile, pursuitUsePunch2);
                huntingSelfDirected = true;
            }
        }

        this.entityData.set(DATA_FRONT_STANCE,
                guarding || punchStanceTicksRemaining > 0 || stationaryBarrageTicks > 0);
        this.entityData.set(DATA_STRIKING, strikingTicks > 0 && !guarding && stationaryBarrageTicks <= 0);
        this.entityData.set(DATA_COMBAT_STANCE, combatHoldTicks > 0);
        this.entityData.set(DATA_FALLING, owner.fallDistance > FALL_POSE_FROM && !owner.onGround()
                && !owner.getAbilities().flying && !owner.isFallFlying() && !owner.isInWater());
        this.entityData.set(DATA_GUARDING, guarding);
        this.entityData.set(DATA_GUARD_THREAT,
                guarding && guardThreat != null && guardThreat.isAlive() && guardThreatTicks > 0
                        ? guardThreat.getId()
                        : -1);
        this.entityData.set(DATA_BARRAGING, barrageHitsRemaining > 0 || isHeldBarrageRunning() || fists.anyLive()
                || closingTicks > 0);
        this.entityData.set(DATA_PEAK, (barrageHitsRemaining > 0 || isHeldBarrageRunning())
                && barrageStyle == BarrageStyle.PUNCHES && fists.momentum() >= BarrageGenerator.PEAK_AT);
        this.entityData.set(DATA_FLOW, flow());
        if (hasteTicks > 0) {
            hasteTicks--;
        }
        this.entityData.set(DATA_HASTE, hasteTicks > 0);
        this.entityData.set(DATA_ENGAGED,
                pursuitTarget != null || returningFromPursuit || utilityAnchor != null);
        this.entityData.set(DATA_HOLD_POSE,
                grip != null ? (gripCarried ? HOLD_FETCH : HOLD_GRAB)
                        : comboStage == ComboStage.HOLD ? comboHoldPose
                        : holdReplayTicks > 0 ? HOLD_NONE
                        : chariotHold);
        if (holdReplayTicks > 0) {
            holdReplayTicks--;
        }
        this.entityData.set(DATA_POSING, timeStopPoseTicks > 0 || isCharging());
        this.entityData.set(DATA_INHALING, inhaleHoldTicks > 0);

        if (isPiloted()) {
            faceOwnerlessIdle();
        } else if (inhaleHoldTicks > 0) {
            tickInhaleHold();
        } else if (utilityAnchor != null) {
            tickUtilityWork();
        } else if (pursuitTarget != null) {
            tickPursuit(owner);
        } else if (returningFromPursuit) {
            tickReturn(owner);
        } else if (partial) {
            lockToOwner(owner);
        } else {
            tickFollow(owner);
        }
    }

    private void tickMobStand() {
        movement.tickMobStand();
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (isClientPredictingFollow()) {
            return;
        }

        if (isPiloted() && locallyPiloted) {
            return;
        }
        boolean far = this.position().distanceToSqr(x, y, z) > GLIDE_JUMP * GLIDE_JUMP * 4;
        super.lerpTo(x, y, z, yRot, xRot, far ? 1 : steps);
    }

    @Override
    public float getViewYRot(float partialTicks) {
        Player owner = pilotView();
        return owner == null ? super.getViewYRot(partialTicks) : owner.getViewYRot(partialTicks);
    }

    @Override
    public float getViewXRot(float partialTicks) {
        Player owner = pilotView();
        return owner == null ? super.getViewXRot(partialTicks) : owner.getViewXRot(partialTicks);
    }

    private Player pilotView() {
        return locallyPiloted && isPiloted() ? getOwner() : null;
    }

    private boolean isClientPredictingFollow() {
        return movement.isClientPredictingFollow();
    }

    StandMovementProfile movement() {
        return StandMovementProfile.forStand(getStandType());
    }

    private void tickFollow(Player owner) {
        movement.tickFollow(owner);
    }

    Vec3 carried(Player owner) {
        return movement.carried(owner);
    }

    Vec3 nextBarrageSway(Player owner, StandMovementProfile profile) {
        return barrage.nextBarrageSway(owner, profile);
    }

    private void pushBackFromGuard(Player owner) {
        guard.pushBackFromGuard(owner);
    }

    void deflect(LivingEntity attacker, Player owner) {
        guard.deflect(attacker, owner);
    }

    public void sendToWork(Vec3 anchor) {
        movement.sendToWork(anchor);
    }

    public void sendToWork(Vec3 anchor, double blocksPerTick) {
        movement.sendToWork(anchor, blocksPerTick);
    }

    void sendToWork(Vec3 anchor, double blocksPerTick, boolean forUtilityJob) {
        movement.sendToWork(anchor, blocksPerTick, forUtilityJob);
    }

    public void sendToUtilityWork(Vec3 anchor) {
        movement.sendToUtilityWork(anchor);
    }

    public void clearWork() {
        movement.clearWork();
    }

    public void clearUtilityWork() {
        movement.clearUtilityWork();
    }

    public boolean isUtilityWorking() {
        return movement.isUtilityWorking();
    }

    public boolean hasArrivedAtWork() {
        return movement.hasArrivedAtWork();
    }

    private void tickUtilityWork() {
        movement.tickUtilityWork();
    }

    private void tickInhaleHold() {
        poses.tickInhaleHold();
    }

    void faceGuard(Player owner) {
        guard.faceGuard(owner);
    }

    public boolean isEngaged() {
        return this.entityData.get(DATA_ENGAGED);
    }

    Vec3 followAnchor(Player owner) {
        return movement.followAnchor(owner);
    }

    Vec3 guardDirection(Player owner, Vec3 forward) {
        return guard.guardDirection(owner, forward);
    }

    public void onGuardHit(LivingEntity attacker, Player owner) {
        guard.onGuardHit(attacker, owner);
    }

    public void setMode(StandMode mode) {
        this.entityData.set(DATA_MODE, mode.ordinal());
    }

    public boolean isGuarding() {
        return this.entityData.get(DATA_GUARDING);
    }

    public StandMode getMode() {
        return StandMode.fromOrdinal(this.entityData.get(DATA_MODE));
    }

    private void tickReturn(Player owner) {
        movement.tickReturn(owner);
    }

    private void lockToOwner(Player owner) {
        movement.lockToOwner(owner);
    }

    private void tickPursuit(Player owner) {
        movement.tickPursuit(owner);
    }

    void tickCommandedStrikes(Player owner, double distance) {
        strikes.tickCommandedStrikes(owner, distance);
    }

    int commandedStrikes;

    public void showHeldItem(ItemStack stack, int ticks) {
        holding.showHeldItem(stack, ticks);
    }

    public void reachTowards(Vec3 point) {
        movement.reachTowards(point);
    }

    void faceTowards(LivingEntity target) {
        movement.faceTowards(target);
    }

    private void faceOwnerlessIdle() {
        movement.faceOwnerlessIdle();
    }

    enum ComboStage {
        NONE,
        LAUNCH,
        CHASE,
        HOLD,
        SLAM,
        BARRAGE,
        SIGNATURE
    }

    boolean tickCombo(Player owner) {
        return strikes.tickCombo(owner);
    }

    void performAttack(Player owner) {
        strikes.performAttack(owner);
    }

    void dealHit(Player owner, float damage) {
        strikes.dealHit(owner, damage);
    }

    void dealHit(Player owner, float damage, boolean scaled) {
        strikes.dealHit(owner, damage, scaled);
    }

    private void tickBarrage(Player owner) {
        barrage.tickBarrage(owner);
    }

    boolean windingUp() {
        return barrage.windingUp();
    }

    static final int BARRAGE_RING_CLUSTER = 3;

    void impactRings(ServerLevel level, LivingEntity target, float damage, int count) {
        barrage.impactRings(level, target, damage, count);
    }

    void impactRings(ServerLevel level, LivingEntity target, Vec3 at, float damage, int count) {
        barrage.impactRings(level, target, at, damage, count);
    }

    public StandCombatProfile combatProfile() {
        return barrage.combatProfile();
    }

    public StandFists fists() {
        return barrage.fists();
    }

    public void fistKnockedBack(StandFist fist) {
        barrage.fistKnockedBack(fist);
    }

    public void fistsClashed(StandEntity other, Vec3 at, Vec3 heading, boolean perfect, StandEntity through) {
        barrage.fistsClashed(other, at, heading, perfect, through);
    }

    static Vec3 struck(LivingEntity victim, Vec3 fist, Vec3 heading) {
        return StandBarrage.struck(victim, fist, heading);
    }

    void playFlurryAnimation() {
        barrage.playFlurryAnimation();
    }

    public boolean isKickBarrage() {
        return barrage.isKickBarrage();
    }

    public boolean atPeak() {
        return barrage.atPeak();
    }

    int closingTicks;

    int flow() {
        return barrage.flow();
    }

    public float barrageMomentum() {
        return barrage.barrageMomentum();
    }

    public float barrageClosing() {
        return barrage.barrageClosing();
    }

    public static final double LINK_HASTE = 1.6;

    int hasteTicks;

    public void hasten(int ticks) {
        barrage.hasten(ticks);
    }

    public boolean hastened() {
        return barrage.hastened();
    }

    void faceTarget() {
        movement.faceTarget();
    }

    private LivingEntity findNearbyHostile(Player owner) {
        return strikes.findNearbyHostile(owner);
    }

    public void triggerLeapPose() {
        poses.triggerLeapPose();
    }

    public CompoundTag caughtProjectile() {
        return this.entityData.get(DATA_CAUGHT);
    }

    public void catchProjectile(Projectile projectile) {
        poses.catchProjectile(projectile);
    }

    public void triggerPunchAt(LivingEntity target, boolean usePunch2) {
        strikes.triggerPunchAt(target, usePunch2);
    }

    public void triggerStrikeAt(LivingEntity target, String animation) {
        strikes.triggerStrikeAt(target, animation);
    }

    public void triggerStrikeForward(String animation) {
        strikes.triggerStrikeForward(animation);
    }

    public void strikeFrom(Vec3 at, LivingEntity target, String animation) {
        strikes.strikeFrom(at, target, animation);
    }

    public void triggerStarFingerPose(LivingEntity target) {
        poses.triggerStarFingerPose(target);
    }

    public void triggerStarFingerAt(Vec3 point) {
        poses.triggerStarFingerAt(point);
    }

    public Vec3 fingerGoal(float partialTick) {
        return poses.fingerGoal(partialTick);
    }

    public void beginInhale(Player owner, int ticks) {
        poses.beginInhale(owner, ticks);
    }

    public void holdInhale(Player owner, int ticks) {
        poses.holdInhale(owner, ticks);
    }

    public void releaseInhale(int overrunTicks) {
        poses.releaseInhale(overrunTicks);
    }

    public Vec3 leftHandAnchor() {
        return poses.leftHandAnchor();
    }

    public void triggerGroundBreakerPose() {
        poses.triggerGroundBreakerPose();
    }

    public void triggerBeatdownPose() {
        poses.triggerBeatdownPose();
    }

    public void triggerKnifeThrowPose() {
        poses.triggerKnifeThrowPose();
    }

    public void triggerThrowPose() {
        poses.triggerThrowPose();
    }

    public void triggerKnifeVolleyPose(int knives) {
        poses.triggerKnifeVolleyPose(knives);
    }

    public int knivesHeld() {
        return poses.knivesHeld();
    }

    public void setKnivesHeld(int count) {
        poses.setKnivesHeld(count);
    }

    public void setKnivesHeld(List<ItemStack> knives) {
        poses.setKnivesHeld(knives);
    }

    public ItemStack knifeHeld(int index) {
        return poses.knifeHeld(index);
    }

    public boolean isInhaling() {
        return poses.isInhaling();
    }

    public void interceptAt(Vec3 point, boolean usePunch2) {
        movement.interceptAt(point, usePunch2);
    }

    int timeStopPoseTicks;

    public boolean isPosing() {
        return poses.isPosing();
    }

    public boolean isCharging() {
        return poses.isCharging();
    }

    public void setCharging(boolean charging) {
        poses.setCharging(charging);
    }

    private void tickChargingStance() {
        poses.tickChargingStance();
    }

    public int triggerTimeStopPose() {
        return poses.triggerTimeStopPose();
    }

    public int chooseTimeStopPose() {
        return poses.chooseTimeStopPose();
    }

    private void tickTimeStopPose() {
        poses.tickTimeStopPose();
    }

    public void stopTimeStopPose() {
        poses.stopTimeStopPose();
    }

    public boolean startChargeLoop() {
        return poses.startChargeLoop();
    }

    public void stopChargeLoop() {
        poses.stopChargeLoop();
    }

    public void triggerGrabAt(LivingEntity target) {
        holding.triggerGrabAt(target);
    }

    public void beginPiloting() {
        endEngagement();
        returningFromPursuit = false;
        this.entityData.set(DATA_PILOTED, true);
    }

    public void endPiloting() {
        this.entityData.set(DATA_PILOTED, false);
        clearFollowMotion();
    }

    public boolean isBarraging() {
        return this.entityData.get(DATA_BARRAGING);
    }

    public boolean isPiloted() {
        return this.entityData.get(DATA_PILOTED);
    }

    void endEngagement() {
        movement.endEngagement();
    }

    void moveTowardsAtSpeed(Vec3 target, double speed) {
        movement.moveTowardsAtSpeed(target, speed);
    }

    public void pursueAndPunch(LivingEntity target, boolean usePunch2) {
        strikes.pursueAndPunch(target, usePunch2);
    }

    void takeAttackStance() {
        strikes.takeAttackStance();
    }

    public boolean inCombatFrame() {
        return isEngaged() || synced(DATA_COMBAT_STANCE);
    }

    public void keepStance() {
        strikes.keepStance();
    }

    public StandMovementState movementState() {
        return movement.movementState();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger,
                                               EntityDimensions dimensions,
                                               float partialTick) {
        float yaw = Mth.rotLerp(partialTick, this.yRotO, this.getYRot());
        float radians = yaw * ((float) Math.PI / 180F);
        Vec3 forward = new Vec3(-Mth.sin(radians), 0, Mth.cos(radians));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);

        double clearance = passenger.getBbWidth() * 0.5;
        if (this.entityData.get(DATA_HOLD_POSE) == HOLD_FETCH) {
            return forward.scale(CRADLE_FORWARD + clearance)
                    .add(0, CRADLE_HEIGHT - passenger.getBbHeight() * CRADLE_SINK, 0);
        }
        double drop = passenger.getBbHeight() * 0.5;

        return forward.scale(GRAB_HAND_FORWARD + clearance)
                .add(right.scale(GRAB_HAND_SIDE))
                .add(0, GRAB_HAND_HEIGHT - drop, 0);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (this.level().noCollision(passenger, passenger.getBoundingBox())) {
            return passenger.position();
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    public boolean grab(LivingEntity target) {
        return holding.grab(target);
    }

    public boolean seize(LivingEntity target) {
        return holding.seize(target);
    }

    public boolean carry(LivingEntity target) {
        return holding.carry(target);
    }

    public boolean isCarrying() {
        return holding.isCarrying();
    }

    public boolean isCradling() {
        return holding.isCradling();
    }

    public boolean isHolding() {
        return holding.isHolding();
    }

    public LivingEntity heldEntity() {
        return holding.heldEntity();
    }

    public void releaseHeld(Vec3 velocity) {
        holding.releaseHeld(velocity);
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseHeld(Vec3.ZERO);
        super.remove(reason);
    }

    public void beginHeldBarrage() {
        barrage.beginHeldBarrage();
    }

    public void beginHeldBarrage(BarrageStyle style) {
        barrage.beginHeldBarrage(style);
    }

    public void endHeldBarrage() {
        barrage.endHeldBarrage();
    }

    public void endHeldBarrageQuietly() {
        barrage.endHeldBarrageQuietly();
    }

    public boolean isHeldBarrageRunning() {
        return barrage.isHeldBarrageRunning();
    }

    void setHeldBarrage(boolean held) {
        barrage.setHeldBarrage(held);
    }

    private void tickHeldBarrage() {
        barrage.tickHeldBarrage();
    }

    private void tickGrip() {
        holding.tickGrip();
    }

    private void clearFollowMotion() {
        movement.clearFollowMotion();
    }

    String strikeAnim(boolean usePunch2) {
        return strikes.strikeAnim(usePunch2);
    }

    public void punch(boolean usePunch2) {
        strikes.punch(usePunch2);
    }

    public void punch(boolean usePunch2, Runnable onContact) {
        strikes.punch(usePunch2, onContact);
    }

    void queueStrike(PendingStrike strike) {
        strikes.queueStrike(strike);
    }

    private void tickStrikeContact() {
        strikes.tickStrikeContact();
    }

    private void tickPendingStrike(Player owner) {
        strikes.tickPendingStrike(owner);
    }

    void releaseCommandedBarrage() {
        barrage.releaseCommandedBarrage();
    }

    enum PendingStrike {
        PUNCH,
        PUNCH2,
        BARRAGE
    }

    public void startGuarding() {
        guard.startGuarding();
    }

    public void setGuardStrain(float strain) {
        guard.setGuardStrain(strain);
    }

    public float guardStrain() {
        return guard.guardStrain();
    }

    public int guardBrokenTicks() {
        return guard.guardBrokenTicks();
    }

    public static int guardBrokenFlashTicks() {
        return StandGuard.guardBrokenFlashTicks();
    }

    public void stopGuarding() {
        guard.stopGuarding();
    }

    public void triggerGuardBroken() {
        guard.triggerGuardBroken();
    }

    public void recoverFromGuardBreak() {
        guard.recoverFromGuardBreak();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        animations.register(controllers);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    public static DoubleSupplier renderPartialTick = () -> 0D;

    int animationHeldUntil = -1;
    double animationHeldAt;
    int animationHeldTotal;

    public void holdAnimation(int ticks) {
        animations.holdAnimation(ticks);
    }

    @Override
    public double getTick(Object animatable) {
        if (this.tickCount < animationHeldUntil) {
            return animationHeldAt - renderPartialTick.getAsDouble();
        }
        return this.tickCount - animationHeldTotal;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.entityData.get(DATA_HELD_ITEM) : ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
    }
}
