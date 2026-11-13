package org.gumel.jojoha.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.level.ServerLevelAccessor;
import org.gumel.jojoha.stand.mob.MobStandHolder;
import org.gumel.jojoha.stand.mob.MobStands;
import org.gumel.jojoha.stand.mob.StandFightGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Mob.class)
public abstract class MobStandMixin implements MobStandHolder {
    @Shadow
    @Final
    protected GoalSelector goalSelector;
    @Unique
    private boolean jojoha$fightsWithStand;
    @Unique
    private String jojoha$standId;
    @Unique
    private int jojoha$standSkin;
    @Unique
    private UUID jojoha$standEntity;
    @Unique
    private int jojoha$calmTicks;

    @Override
    public String jojoha$standId() {
        return jojoha$standId;
    }

    @Override
    public int jojoha$standSkin() {
        return jojoha$standSkin;
    }

    @Override
    public void jojoha$setStand(String standId, int skin) {
        this.jojoha$standId = standId;
        this.jojoha$standSkin = skin;
    }

    @Override
    public UUID jojoha$standEntity() {
        return jojoha$standEntity;
    }

    @Override
    public void jojoha$setStandEntity(UUID standEntity) {
        this.jojoha$standEntity = standEntity;
    }

    @Override
    public int jojoha$calmTicks() {
        return jojoha$calmTicks;
    }

    @Override
    public void jojoha$setCalmTicks(int ticks) {
        this.jojoha$calmTicks = ticks;
    }

    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void jojoha$saveStand(CompoundTag tag, CallbackInfo ci) {
        if (jojoha$standId != null) {
            tag.putString("JojohaStand", jojoha$standId);
            tag.putInt("JojohaStandSkin", jojoha$standSkin);
        }
    }

    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void jojoha$loadStand(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("JojohaStand")) {
            jojoha$standId = tag.getString("JojohaStand");
            jojoha$standSkin = tag.getInt("JojohaStandSkin");
        }
    }

    @Inject(method = "aiStep()V", at = @At("HEAD"))
    private void jojoha$tickStand(CallbackInfo ci) {
        if (jojoha$standId != null) {
            if (!jojoha$fightsWithStand && (Object) this instanceof PathfinderMob pathfinder
                    && !pathfinder.level().isClientSide()) {
                jojoha$fightsWithStand = true;
                goalSelector.addGoal(1, new StandFightGoal(pathfinder));
            }
            MobStands.tick((Mob) (Object) this);
        }
    }

    @Inject(method = "finalizeSpawn(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;"
            + "Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;)"
            + "Lnet/minecraft/world/entity/SpawnGroupData;", at = @At("RETURN"))
    private void jojoha$rollStand(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                  SpawnGroupData data, CallbackInfoReturnable<SpawnGroupData> cir) {
        MobStands.onSpawn((Mob) (Object) this, spawnType);
    }
}
