package org.gumel.jojoha.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.client.PlayerStabAnimation;
import org.gumel.jojoha.client.StunPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelStunMixin {
    @Unique
    private boolean jojoha$posedForStun;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void jojoha$clearStunPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                      float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (jojoha$posedForStun) {
            jojoha$posedForStun = false;
            PlayerStabAnimation.resetParts((HumanoidModel<?>) (Object) this);
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void jojoha$applyStunPose(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                      float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        float now = (float) minecraft.level.getGameTime() + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        if (StunPose.apply((HumanoidModel<?>) (Object) this, entity, now)) {
            jojoha$posedForStun = true;
        }
    }
}
