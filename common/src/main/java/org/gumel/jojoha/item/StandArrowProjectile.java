package org.gumel.jojoha.item;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.gumel.jojoha.registry.ModItems;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.stand.mob.MobStands;
import org.jetbrains.annotations.Nullable;

public class StandArrowProjectile extends AbstractArrow {
    private static final EntityDataAccessor<ItemStack> SHOWN =
            SynchedEntityData.defineId(StandArrowProjectile.class, EntityDataSerializers.ITEM_STACK);

    public StandArrowProjectile(EntityType<? extends StandArrowProjectile> type, Level level) {
        super(type, level);
    }

    public StandArrowProjectile(Level level, LivingEntity owner, ItemStack arrow, @Nullable ItemStack weapon) {
        super(ModRegistries.STAND_ARROW_PROJECTILE.get(), owner, level, arrow, weapon);
        show();
    }

    public StandArrowProjectile(Level level, double x, double y, double z, ItemStack arrow) {
        super(ModRegistries.STAND_ARROW_PROJECTILE.get(), x, y, z, level, arrow, null);
        show();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHOWN, ItemStack.EMPTY);
    }

    @Override
    protected void setPickupItemStack(ItemStack stack) {
        super.setPickupItemStack(stack);
        show();
    }

    private void show() {
        ItemStack origin = getPickupItemStackOrigin();
        entityData.set(SHOWN, origin.isEmpty() ? ItemStack.EMPTY : origin.copyWithCount(1));
    }

    public ItemStack shownItem() {
        ItemStack shown = entityData.get(SHOWN);
        return shown.isEmpty() ? getDefaultPickupItem() : shown;
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(ModItems.STAND_ARROW.get());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (level() instanceof ServerLevel level && result.getEntity() instanceof LivingEntity struck
                && getPickupItemStackOrigin().getItem() instanceof StandArrowItem arrow) {
            ArrowStrike outcome = ArrowStrike.NOTHING;
            if (struck instanceof ServerPlayer person) {
                outcome = StandArrowRitual.struck(person, arrow);
            } else if (struck instanceof Mob mob) {
                outcome = MobStands.struck(mob, arrow, level);
            }
            if (outcome != ArrowStrike.NOTHING) {
                discard();
                return;
            }
        }

        ItemStack kept = getPickupItemStackOrigin().copy();
        super.onHitEntity(result);
        if (isRemoved() && !level().isClientSide() && pickup == Pickup.ALLOWED && !kept.isEmpty()) {
            spawnAtLocation(kept, 0.1F);
        }
    }
}
