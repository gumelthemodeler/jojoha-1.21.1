package org.gumel.jojoha.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.labyrinth.CatacombSeal;
import org.gumel.jojoha.labyrinth.DungeonSpawnerBlockEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class VampireRat extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> COMMUNING =
            SynchedEntityData.defineId(VampireRat.class, EntityDataSerializers.INT);

    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation COMMUNE = RawAnimation.begin().thenPlay("commune");
    private static final String CONTROLLER = "rat";
    private static final int TRANSITION_TICKS = 3;
    private static final double WALKING_THRESHOLD = 0.02;

    public static final int COMMUNE_TICKS = 26 + TRANSITION_TICKS;
    private static final int SEEK_RADIUS = 24;
    private static final int SEEK_EVERY = 40;
    private static final double REACH = 2.2;
    private static final int GIVE_UP_TICKS = 1200;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private BlockPos spawner;
    private int seeking;

    public VampireRat(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COMMUNING, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new RekindleGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void aim(BlockPos spawner) {
        this.spawner = spawner;
    }

    public boolean communing() {
        return entityData.get(COMMUNING) > 0;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        int commune = entityData.get(COMMUNING);
        if (commune > 0 && hurtTime > 0) {
            entityData.set(COMMUNING, 0);
            return;
        }
        if (commune > 0) {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (spawner != null) {
                getLookControl().setLookAt(Vec3.atCenterOf(spawner));
            }
            if (commune % 6 == 0) {
                Vec3 at = position();
                server.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.4, at.z, 3, 0.2, 0.2, 0.2, 0.01);
            }
            entityData.set(COMMUNING, commune - 1);
            if (commune - 1 == 0) {
                finish(server);
            }
            return;
        }
        if (spawner != null && !DungeonSpawnerBlockEntity.revivable(server.getBlockState(spawner), server, spawner)) {
            spawner = null;
        }
        if (spawner == null && tickCount % SEEK_EVERY == 0) {
            spawner = CatacombSeal.spentNear(server, blockPosition(), SEEK_RADIUS);
        }
        if (spawner != null && ++seeking > GIVE_UP_TICKS) {
            spawner = null;
            seeking = 0;
        }
    }

    private void finish(ServerLevel server) {
        if (spawner != null) {
            BlockState state = server.getBlockState(spawner);
            if (server.getBlockEntity(spawner) instanceof DungeonSpawnerBlockEntity block) {
                block.revive(server, spawner, state);
            }
        }
        vanish(server);
    }

    private void vanish(ServerLevel server) {
        Vec3 at = position();
        server.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.3, at.z, 12, 0.25, 0.2, 0.25, 0.02);
        server.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.3, at.z, 4, 0.2, 0.2, 0.2, 0.03);
        server.playSound(null, blockPosition(), SoundEvents.BAT_TAKEOFF, SoundSource.HOSTILE, 0.8F, 0.7F);
        discard();
    }

    private final class RekindleGoal extends Goal {

        RekindleGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return spawner != null;
        }

        @Override
        public boolean canContinueToUse() {
            return spawner != null && !communing();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (spawner == null) {
                return;
            }
            Vec3 target = Vec3.atCenterOf(spawner);
            getLookControl().setLookAt(target);
            if (position().distanceTo(target) <= REACH) {
                getNavigation().stop();
                entityData.set(COMMUNING, COMMUNE_TICKS);
                playSound(SoundEvents.SILVERFISH_AMBIENT, 1.0F, 0.6F);
                return;
            }
            if (tickCount % 10 == 0 || getNavigation().isDone()) {
                getNavigation().moveTo(target.x, spawner.getY(), target.z, 1.2);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (spawner != null) {
            tag.put("Spawner", NbtUtils.writeBlockPos(spawner));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        spawner = NbtUtils.readBlockPos(tag, "Spawner").orElse(null);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.3F;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER, TRANSITION_TICKS, this::animate));
    }

    private PlayState animate(AnimationState<VampireRat> state) {
        if (communing()) {
            return state.setAndContinue(COMMUNE);
        }
        if (state.getLimbSwingAmount() > WALKING_THRESHOLD) {
            return state.setAndContinue(WALK);
        }
        state.getController().forceAnimationReset();
        return PlayState.STOP;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
