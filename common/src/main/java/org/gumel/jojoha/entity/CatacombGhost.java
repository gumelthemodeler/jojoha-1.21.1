package org.gumel.jojoha.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.gumel.jojoha.registry.ModRegistries;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class CatacombGhost extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> SHADE =
            SynchedEntityData.defineId(CatacombGhost.class, EntityDataSerializers.INT);

    public static final int[] SHADES = {0xE8F2FF, 0x9FD4FF, 0x7FEBD6, 0xB6F59A, 0xC6A6FF, 0xFFB0C8};

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenLoop("attack");
    private static final String CONTROLLER = "ghost";
    private static final int TRANSITION_TICKS = 5;

    private static final int HOME_REACH = 10;
    private static final int LUNGE_LOOP = 30;
    private static final int LUNGE_PEAK = 15;
    private static final int LUNGE_WINDOW = 2;
    private static final double CHARGE_SPEED = 0.45;
    private static final double DRIFT_SPEED = 0.25;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private BlockPos home;
    private int aggroTick = -1;
    private int lastLunge = -1;

    public CatacombGhost(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new GhostMoveControl(this);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHADE, 0);
    }

    public int shade() {
        return SHADES[Mth.clamp(entityData.get(SHADE), 0, SHADES.length - 1)];
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        SpawnGroupData data) {
        entityData.set(SHADE, random.nextInt(SHADES.length));
        home = blockPosition();
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new ChargeGoal());
        goalSelector.addGoal(8, new DriftGoal());
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        if (fogged(movement)) {
            movement = new Vec3(0.0, movement.y, 0.0);
            setDeltaMovement(getDeltaMovement().multiply(-0.3, 1.0, -0.3));
        }
        super.move(type, movement);
        checkInsideBlocks();
    }

    private boolean fogged(Vec3 movement) {
        AABB now = getBoundingBox();
        AABB next = now.expandTowards(movement.x, 0.0, movement.z);
        CollisionContext context = CollisionContext.of(this);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(Math.min(now.minX, next.minX), now.minY, Math.min(now.minZ, next.minZ)),
                BlockPos.containing(Math.max(now.maxX, next.maxX), now.maxY, Math.max(now.maxZ, next.maxZ)))) {
            BlockState state = level().getBlockState(pos);
            if (!state.is(ModRegistries.CATACOMB_FOG.get())) {
                continue;
            }
            for (AABB sheet : state.getCollisionShape(level(), pos, context).toAabbs()) {
                AABB placed = sheet.move(pos);
                if (next.intersects(placed) && !now.intersects(placed)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void tick() {
        noPhysics = true;
        super.tick();
        noPhysics = false;
        setNoGravity(true);
    }

    private boolean lunging() {
        if (aggroTick < 0) {
            return false;
        }
        int elapsed = tickCount - aggroTick - TRANSITION_TICKS;
        if (elapsed < 0) {
            return false;
        }
        int loop = elapsed / LUNGE_LOOP;
        return loop != lastLunge && Math.abs(elapsed % LUNGE_LOOP - LUNGE_PEAK) <= LUNGE_WINDOW;
    }

    private void lunged() {
        lastLunge = (tickCount - aggroTick - TRANSITION_TICKS) / LUNGE_LOOP;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state,
                                   BlockPos pos) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    private final class ChargeGoal extends Goal {

        ChargeGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            setAggressive(true);
            aggroTick = tickCount;
            lastLunge = -1;
        }

        @Override
        public void stop() {
            setAggressive(false);
            aggroTick = -1;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            Vec3 aim = target.getEyePosition().add(0.0, -0.6, 0.0);
            getLookControl().setLookAt(target);
            if (getBoundingBox().inflate(0.2).intersects(target.getBoundingBox())) {
                if (lunging()) {
                    doHurtTarget(target);
                    lunged();
                }
                return;
            }
            moveControl.setWantedPosition(aim.x, aim.y, aim.z, CHARGE_SPEED);
        }
    }

    private final class DriftGoal extends Goal {

        DriftGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !moveControl.hasWanted() && random.nextInt(reducedTickDelay(7)) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            BlockPos origin = home != null ? home : blockPosition();
            for (int tries = 0; tries < 3; tries++) {
                BlockPos to = origin.offset(random.nextInt(HOME_REACH * 2 + 1) - HOME_REACH,
                        random.nextInt(5) - 2, random.nextInt(HOME_REACH * 2 + 1) - HOME_REACH);
                if (level().isEmptyBlock(to)) {
                    moveControl.setWantedPosition(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5, DRIFT_SPEED);
                    if (getTarget() == null) {
                        getLookControl().setLookAt(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5, 180.0F, 20.0F);
                    }
                    break;
                }
            }
        }
    }

    private static final class GhostMoveControl extends MoveControl {
        private final CatacombGhost ghost;

        GhostMoveControl(CatacombGhost ghost) {
            super(ghost);
            this.ghost = ghost;
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO) {
                return;
            }
            Vec3 to = new Vec3(wantedX - ghost.getX(), wantedY - ghost.getY(), wantedZ - ghost.getZ());
            double length = to.length();
            if (length < ghost.getBoundingBox().getSize()) {
                operation = Operation.WAIT;
                ghost.setDeltaMovement(ghost.getDeltaMovement().scale(0.5));
                return;
            }
            ghost.setDeltaMovement(ghost.getDeltaMovement().add(to.scale(speedModifier * 0.05 / length)));
            if (ghost.getTarget() == null) {
                Vec3 motion = ghost.getDeltaMovement();
                ghost.setYRot(-((float) Mth.atan2(motion.x, motion.z)) * Mth.RAD_TO_DEG);
            } else {
                double dx = ghost.getTarget().getX() - ghost.getX();
                double dz = ghost.getTarget().getZ() - ghost.getZ();
                ghost.setYRot(-((float) Mth.atan2(dx, dz)) * Mth.RAD_TO_DEG);
            }
            ghost.yBodyRot = ghost.getYRot();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Shade", entityData.get(SHADE));
        if (home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SHADE, tag.getInt("Shade"));
        home = NbtUtils.readBlockPos(tag, "Home").orElse(null);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SOUL_ESCAPE.value();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.55F;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, TRANSITION_TICKS, this::animate));
    }

    private PlayState animate(AnimationState<CatacombGhost> state) {
        return state.setAndContinue(isAggressive() ? ATTACK : IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
