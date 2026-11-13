package org.gumel.jojoha.item;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.registry.ModRegistries;

public class FallingMask extends Entity {
    private static final double GRAVITY = 0.04;

    private static final float SPIN_DEGREES_PER_TICK = 14F;

    private static final int MAX_LIFETIME = 200;

    private static final int SHARDS = 40;
    private static final double SHARD_SPREAD = 0.22;

    private static final EntityDataAccessor<Boolean> DATA_AJA =
            SynchedEntityData.defineId(FallingMask.class, EntityDataSerializers.BOOLEAN);

    private boolean broken;

    public FallingMask(EntityType<? extends FallingMask> type, Level level) {
        super(type, level);
    }

    public FallingMask(Level level, double x, double y, double z, Vec3 push, boolean aja) {
        this(ModRegistries.FALLING_MASK.get(), level);
        this.entityData.set(DATA_AJA, aja);
        this.setPos(x, y, z);
        this.setDeltaMovement(push);

        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount > MAX_LIFETIME) {
            this.discard();
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x * 0.98, (motion.y - GRAVITY) * 0.98, motion.z * 0.98);
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (!this.level().isClientSide && this.onGround() && !this.broken) {
            this.broken = true;
            shatter((ServerLevel) this.level());
            this.discard();
        }
    }

    public boolean aja() {
        return this.entityData.get(DATA_AJA);
    }

    public float spinDegrees(float partialTick) {
        return (this.tickCount + partialTick) * SPIN_DEGREES_PER_TICK;
    }

    private void shatter(ServerLevel level) {
        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                this.getX(), this.getY() + 0.1, this.getZ(), SHARDS,
                SHARD_SPREAD, SHARD_SPREAD, SHARD_SPREAD, 0.06);

        level.playSound(null, this.blockPosition(), SoundEvents.STONE_BREAK,
                SoundSource.PLAYERS, 1.0F, 0.65F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_AJA, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_AJA, tag.getBoolean("Aja"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Aja", aja());
    }
}
