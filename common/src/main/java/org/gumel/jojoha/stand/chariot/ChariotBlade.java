package org.gumel.jojoha.stand.chariot;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.StandEntity;

public class ChariotBlade extends ThrowableProjectile {
    public static final float DAMAGE = 6.0F;
    public static final float SPEED = 1.8F;
    public static final int LIFETIME = 30;
    public static final int LOST_TICKS = 300;
    private static final double GRAVITY = 0.01;
    private static final double SPENT_GRAVITY = 0.05;
    private static final double PICKUP_RANGE = 1.6;
    private static final double BOUNCE = 0.15;
    private static final int GLINT_EVERY = 10;

    private static final EntityDataAccessor<Integer> DATA_SKIN =
            SynchedEntityData.defineId(ChariotBlade.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_STUCK =
            SynchedEntityData.defineId(ChariotBlade.class, EntityDataSerializers.BOOLEAN);

    private float damage = DAMAGE;
    private boolean spent;

    public ChariotBlade(EntityType<? extends ChariotBlade> type, Level level) {
        super(type, level);
    }

    public static ChariotBlade fire(ServerPlayer owner, StandEntity stand, Vec3 from, Vec3 heading, float damage) {
        ChariotBlade blade = new ChariotBlade(ModRegistries.CHARIOT_BLADE.get(), owner.level());
        blade.setOwner(owner);
        blade.damage = damage;
        blade.entityData.set(DATA_SKIN, stand.getSkin());
        blade.setPos(from.x, from.y, from.z);
        blade.shoot(heading.x, heading.y, heading.z, SPEED, 0F);
        owner.level().addFreshEntity(blade);
        return blade;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SKIN, 0);
        builder.define(DATA_STUCK, false);
    }

    public boolean stuck() {
        return this.entityData.get(DATA_STUCK);
    }

    public int skin() {
        return this.entityData.get(DATA_SKIN);
    }

    @Override
    protected double getDefaultGravity() {
        return spent ? SPENT_GRAVITY : GRAVITY;
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel level && !looked(level)) {
            return;
        }
        if (stuck()) {
            if (level() instanceof ServerLevel level && tickCount % GLINT_EVERY == 0) {
                level.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.2, getZ(), 1, 0.05, 0.05, 0.05, 0.0);
            }
            return;
        }
        super.tick();
        if (level() instanceof ServerLevel level && !spent) {
            level.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 1, 0.02, 0.02, 0.02, 0.0);
            if (tickCount >= LIFETIME) {
                spent = true;
            }
        }
    }

    private boolean looked(ServerLevel level) {
        if (!(getOwner() instanceof ServerPlayer owner) || owner.isRemoved() || !Chariot.holds(owner, this)) {
            discard();
            return false;
        }
        if (tickCount >= LOST_TICKS) {
            comeHome(level, owner, false);
            return false;
        }
        if ((spent || stuck()) && owner.isAlive() && owner.level() == level
                && owner.distanceToSqr(this) < PICKUP_RANGE * PICKUP_RANGE) {
            comeHome(level, owner, true);
            return false;
        }
        return true;
    }

    private void comeHome(ServerLevel level, ServerPlayer owner, boolean picked) {
        discard();
        if (picked) {
            level.playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 1.2F);
        }
        level.playSound(null, owner.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    private void stick(Vec3 at) {
        setDeltaMovement(Vec3.ZERO);
        setPos(at.x, at.y, at.z);
        setNoGravity(true);
        spent = true;
        this.entityData.set(DATA_STUCK, true);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (spent || stuck()) {
            return false;
        }
        Entity owner = getOwner();
        if (target instanceof StandEntity other && owner != null) {
            return other.getOwner() != owner && StandHits.hittable(owner, other);
        }
        if (owner != null && target == owner) {
            return false;
        }
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(level() instanceof ServerLevel level) || !(result.getEntity() instanceof LivingEntity hit)) {
            return;
        }
        if (getOwner() instanceof ServerPlayer owner) {
            if (Chariot.strike(owner, hit, damage)) {
                Bleed.apply(hit);
            }
        } else {
            HitWindow.open(hit);
            hit.hurt(damageSources().thrown(this, getOwner()),
                    DamageScale.stand(getOwner(), hit, damage));
        }
        Impacts.landed(level, hit, getDeltaMovement(),
                ImpactSpec.HEAVY.silent(), Impacts.STEEL);
        level.playSound(null, hit.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 1.3F);
        spent = true;
        setDeltaMovement(getDeltaMovement().scale(-BOUNCE).add(0.0, BOUNCE, 0.0));
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = result.getLocation();
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 6, 0.08, 0.08, 0.08, 0.15);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.08, 0.08, 0.08, 0.2);
        level.playSound(null, result.getBlockPos(), SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 1.0F, 1.2F);
        stick(at);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("Skin", skin());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        entityData.set(DATA_SKIN, tag.getInt("Skin"));
    }
}
