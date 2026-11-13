package org.gumel.jojoha.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.client.MenuIdlePose;
import org.gumel.jojoha.client.ParryFeel;
import org.gumel.jojoha.client.PlayerMoveAnimation;
import org.gumel.jojoha.client.PlayerStabAnimation;
import org.gumel.jojoha.client.fp.FirstPersonArms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Unique
    private boolean jojoha$posedForStab;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void jojoha$clearStabPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                      float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (jojoha$posedForStab) {
            jojoha$posedForStab = false;
            PlayerStabAnimation.resetParts((PlayerModel<?>) (Object) this);
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void jojoha$applyStabAnimation(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                           float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof Player player)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        if (MenuIdlePose.apply((PlayerModel<?>) (Object) this)) {
            jojoha$posedForStab = true;
            return;
        }

        if (FirstPersonArms.drawing()) {
            jojoha$posedForStab = true;
            PlayerStabAnimation.resetParts((PlayerModel<?>) (Object) this);
            return;
        }

        float clientTime = (float) minecraft.level.getGameTime()
                + minecraft.getTimer().getGameTimeDeltaPartialTick(false);

        if (PlayerStabAnimation.apply((PlayerModel<?>) (Object) this, player, clientTime)) {
            jojoha$posedForStab = true;
            return;
        }

        if (PlayerStabAnimation.inHandPass()) {
            return;
        }

        if (PlayerMoveAnimation.apply((PlayerModel<?>) (Object) this, player, clientTime)) {
            jojoha$posedForStab = true;
            return;
        }

    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void jojoha$parryKick(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                  float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(entity instanceof Player player) || minecraft.level == null
                || FirstPersonArms.drawing() || PlayerStabAnimation.inHandPass() || MenuIdlePose.drawing()) {
            return;
        }
        float clientTime = (float) minecraft.level.getGameTime()
                + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        if (ParryFeel.apply((PlayerModel<?>) (Object) this, player, clientTime)) {
            jojoha$posedForStab = true;
        }
    }
}
