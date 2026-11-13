package org.gumel.jojoha.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.gumel.jojoha.entity.ai.FollowMasterGoal;
import org.gumel.jojoha.entity.ai.MasterHurtByTargetGoal;
import org.gumel.jojoha.entity.ai.MasterHurtTargetGoal;
import org.gumel.jojoha.vampire.VampireFx;
import org.gumel.jojoha.vampire.VampireMinions;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class RisenZombie extends Zombie implements GeoEntity {
    private static final EntityDataAccessor<Integer> EMERGING =
            SynchedEntityData.defineId(RisenZombie.class, EntityDataSerializers.INT);

    public static final int EMERGE_TICKS = 30;

    private static final int MASTER_CHECK_EVERY = 20;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private long crumbleAt = -1L;

    @Nullable
    private UUID master;

    private boolean raisedByPlayer;

    @Nullable
    private String tier;

    private boolean sunproof;

    public RisenZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public void raisedBy(Entity raiser, int lifetime, boolean emerge) {
        this.master = raiser.getUUID();
        this.raisedByPlayer = raiser instanceof ServerPlayer;
        this.crumbleAt = level().getGameTime() + lifetime;
        this.sunproof = raisedByPlayer;
        if (emerge) {
            entityData.set(EMERGING, EMERGE_TICKS);
        }
    }

    public void rememberTier(VampireMinions.Tier tier) {
        this.tier = tier.name();
    }

    @Nullable
    public UUID master() {
        return master;
    }

    @Nullable
    public LivingEntity masterEntity() {
        if (master == null || !(level() instanceof ServerLevel level)) {
            return null;
        }
        return level.getEntity(master) instanceof LivingEntity raiser ? raiser : null;
    }

    public boolean sharesMasterWith(Entity other) {
        return master != null && other instanceof RisenZombie sibling
                && master.equals(sibling.master);
    }

    public float emergence(float partialTick) {
        int left = entityData.get(EMERGING);
        if (left <= 0) {
            return 1F;
        }
        return 1F - Math.max(0F, left - partialTick) / EMERGE_TICKS;
    }

    public boolean isEmerging() {
        return entityData.get(EMERGING) > 0;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnData) {
        setLeftHanded(random.nextFloat() < 0.05F);
        return spawnData;
    }

    @Override
    public void setBaby(boolean baby) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof Player player && masterEntity() instanceof VampireEntity sire) {
            sire.provokedBy(player);
        }
        return hurt;
    }

    @Override
    protected void addBehaviourGoals() {
        goalSelector.addGoal(2, new ZombieAttackGoal(this, 1.0, false));
        goalSelector.addGoal(6, new FollowMasterGoal(this, 1.15));
        goalSelector.addGoal(7,
                new WaterAvoidingRandomStrollGoal(this, 1.0));

        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new MasterHurtByTargetGoal(this));
        targetSelector.addGoal(3, new MasterHurtTargetGoal(this));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(
                this, Player.class, true, target -> master == null));
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(
                this, AbstractVillager.class, false, target -> master == null));
        targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(
                this, IronGolem.class, true, target -> master == null));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && master != null
                && (master.equals(target.getUUID()) || sharesMasterWith(target))) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity killed) {
        return true;
    }

    @Override
    protected boolean isSunSensitive() {
        return !sunproof;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isEmerging();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(EMERGING, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }

        int emerging = entityData.get(EMERGING);
        if (emerging > 0) {
            entityData.set(EMERGING, emerging - 1);
            churn(level);
        }

        if (crumbleAt >= 0 && level.getGameTime() >= crumbleAt) {
            crumble(level);
            return;
        }

        if (master != null && tickCount % MASTER_CHECK_EVERY == 0) {
            keepFaith(level);
        }
    }

    private void churn(ServerLevel level) {
        BlockPos below = blockPosition().below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground),
                    getX(), getY() + 0.1, getZ(), 6, 0.35, 0.05, 0.35, 0.12);
        }
        if (tickCount % 6 == 0) {
            level.playSound(null, blockPosition(), ground.getSoundType().getBreakSound(),
                    SoundSource.HOSTILE, 0.6F, 0.7F);
        }
    }

    private void keepFaith(ServerLevel level) {
        Entity raiser = level.getEntity(master);
        if (!raisedByPlayer) {
            if (raiser == null || !raiser.isAlive()) {
                crumble(level);
            }
            return;
        }

        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(master);
        if (owner != null && owner.level() == level && tier != null && crumbleAt >= 0) {
            VampireMinions.readopt(owner, this, VampireMinions.Tier.valueOf(tier),
                    (int) Math.max(1L, crumbleAt - level.getGameTime()));
        }
    }

    public void crumble(ServerLevel level) {
        double mid = getY() + getBbHeight() * 0.5;
        level.sendParticles(ParticleTypes.ASH, getX(), mid, getZ(), 30, 0.3, 0.6, 0.3, 0.02);
        level.sendParticles(ParticleTypes.SMOKE, getX(), mid, getZ(), 12, 0.25, 0.5, 0.25, 0.01);
        VampireFx.blood(level, getX(), mid, getZ(), 8, 0.3, 0.04);
        level.playSound(null, blockPosition(), SoundEvents.ZOMBIE_DEATH, SoundSource.HOSTILE,
                0.8F, 0.6F);
        level.playSound(null, blockPosition(), SoundEvents.SAND_BREAK, SoundSource.HOSTILE,
                1.0F, 0.6F);
        VampireMinions.forget(this);
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putLong("CrumbleAt", crumbleAt);
        tag.putBoolean("RaisedByPlayer", raisedByPlayer);
        tag.putBoolean("Sunproof", sunproof);
        if (master != null) {
            tag.putUUID("Master", master);
        }
        if (tier != null) {
            tag.putString("Tier", tier);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        crumbleAt = tag.contains("CrumbleAt") ? tag.getLong("CrumbleAt") : -1L;
        raisedByPlayer = tag.getBoolean("RaisedByPlayer");
        sunproof = tag.getBoolean("Sunproof");
        master = tag.hasUUID("Master") ? tag.getUUID("Master") : null;
        tier = tag.contains("Tier") ? tag.getString("Tier") : null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
