package org.gumel.jojoha.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.accessory.Accessories;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerAccessoriesMixin extends LivingEntity implements Accessories.Holder {
    @Unique
    private static final EntityDataAccessor<ItemStack>[] jojoha$WORN = jojoha$defineWorn();

    @Unique
    private SimpleContainer jojoha$accessories;

    protected PlayerAccessoriesMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<ItemStack>[] jojoha$defineWorn() {
        EntityDataAccessor<ItemStack>[] worn = new EntityDataAccessor[Accessories.COUNT];
        for (int i = 0; i < worn.length; i++) {
            worn[i] = SynchedEntityData.defineId(Player.class, EntityDataSerializers.ITEM_STACK);
        }
        return worn;
    }

    @Override
    public SimpleContainer jojoha$accessories() {
        if (jojoha$accessories == null) {
            jojoha$accessories = new SimpleContainer(Accessories.COUNT);
        }
        return jojoha$accessories;
    }

    @Override
    public ItemStack jojoha$shown(int index) {
        return this.getEntityData().get(jojoha$WORN[index]);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void jojoha$defineAccessories(SynchedEntityData.Builder builder, CallbackInfo ci) {
        for (EntityDataAccessor<ItemStack> worn : jojoha$WORN) {
            builder.define(worn, ItemStack.EMPTY);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void jojoha$showAccessories(CallbackInfo ci) {
        if (this.level().isClientSide) {
            return;
        }
        SimpleContainer worn = jojoha$accessories();
        for (int i = 0; i < Accessories.COUNT; i++) {
            ItemStack stack = worn.getItem(i);
            if (!ItemStack.matches(this.getEntityData().get(jojoha$WORN[i]), stack)) {
                this.getEntityData().set(jojoha$WORN[i], stack.copy());
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void jojoha$saveAccessories(CompoundTag tag, CallbackInfo ci) {
        CompoundTag accessories = new CompoundTag();
        ContainerHelper.saveAllItems(accessories, jojoha$accessories().getItems(), this.registryAccess());
        tag.put(Accessories.SAVE_KEY, accessories);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void jojoha$loadAccessories(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(Accessories.SAVE_KEY, 10)) {
            SimpleContainer worn = jojoha$accessories();
            worn.clearContent();
            ContainerHelper.loadAllItems(tag.getCompound(Accessories.SAVE_KEY), worn.getItems(), this.registryAccess());
        }
    }

    @Inject(method = "dropEquipment", at = @At("TAIL"))
    private void jojoha$dropAccessories(CallbackInfo ci) {
        if (this.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        SimpleContainer worn = jojoha$accessories();
        for (int i = 0; i < Accessories.COUNT; i++) {
            ItemStack stack = worn.removeItemNoUpdate(i);
            if (!stack.isEmpty()) {
                ((Player) (Object) this).drop(stack, true, false);
            }
        }
    }
}
