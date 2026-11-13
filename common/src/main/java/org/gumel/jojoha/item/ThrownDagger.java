package org.gumel.jojoha.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModRegistries;

public class ThrownDagger extends AbstractArrow {
    private static final EntityDataAccessor<ItemStack> DATA_DAGGER =
            SynchedEntityData.defineId(ThrownDagger.class, EntityDataSerializers.ITEM_STACK);

    private static final float DEFAULT_DAMAGE = 4.0F;

    public ThrownDagger(EntityType<? extends ThrownDagger> type, Level level) {
        super(type, level);
    }

    public ThrownDagger(Level level, LivingEntity thrower, ItemStack thrown, float damage) {
        this(ModRegistries.THROWN_DAGGER.get(), level, thrower, thrown, damage);
    }

    protected ThrownDagger(EntityType<? extends ThrownDagger> type, Level level, LivingEntity thrower,
                           ItemStack thrown, float damage) {
        super(type, thrower, level, thrown.copy(), thrown.copy());
        this.setBaseDamage(damage);

        this.pickup = Pickup.ALLOWED;
        this.entityData.set(DATA_DAGGER, thrown.copy());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DAGGER, ItemStack.EMPTY);
    }

    public ItemStack displayStack() {
        ItemStack synced = this.entityData.get(DATA_DAGGER);
        return synced.isEmpty() ? this.getPickupItemStackOrigin() : synced;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_DAGGER, this.getPickupItemStackOrigin().copy());
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.DAGGER_IRON.get());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        int before = result.getEntity() instanceof LivingEntity hit ? hit.getArrowCount() : 0;

        super.onHitEntity(result);

        if (result.getEntity() instanceof LivingEntity hit) {
            hit.setArrowCount(before);
        }

        this.playSound(SoundEvents.TRIDENT_HIT, 1.0F, 1.0F);
    }

    @Override
    protected boolean tryPickup(Player player) {
        return this.ownedBy(player) && super.tryPickup(player);
    }

    @Override
    public ItemStack getWeaponItem() {
        return this.getPickupItemStackOrigin();
    }

    public static float defaultDamage() {
        return DEFAULT_DAMAGE;
    }
}
