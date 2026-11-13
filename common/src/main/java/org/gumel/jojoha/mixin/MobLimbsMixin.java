package org.gumel.jojoha.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.gumel.jojoha.body.LimbHolder;
import org.gumel.jojoha.body.Limbs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobLimbsMixin extends LivingEntity implements LimbHolder {
    @Unique
    private static final EntityDataAccessor<Byte> jojoha$LIMBS =
            SynchedEntityData.defineId(Mob.class, EntityDataSerializers.BYTE);

    @Unique
    private static final String jojoha$SAVE_KEY = "jojoha_limbs";

    protected MobLimbsMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public int jojoha$limbs() {
        return this.getEntityData().get(jojoha$LIMBS);
    }

    @Override
    public void jojoha$setLimbs(int limbs) {
        this.getEntityData().set(jojoha$LIMBS, (byte) (limbs & Limbs.ALL));
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void jojoha$defineLimbs(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(jojoha$LIMBS, (byte) 0);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void jojoha$saveLimbs(CompoundTag tag, CallbackInfo ci) {
        int limbs = jojoha$limbs();
        if (limbs != 0) {
            tag.putByte(jojoha$SAVE_KEY, (byte) limbs);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void jojoha$loadLimbs(CompoundTag tag, CallbackInfo ci) {
        jojoha$setLimbs(tag.getByte(jojoha$SAVE_KEY));
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void jojoha$armless(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (Limbs.armless(this)) {
            cir.setReturnValue(false);
        }
    }
}
