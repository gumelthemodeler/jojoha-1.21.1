package org.gumel.jojoha.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.body.Limbs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class LostLimbsArmorMixin {
    @Unique
    private int jojoha$lost;

    @Inject(method = "renderArmorPiece*", at = @At("HEAD"))
    private void jojoha$wearer(CallbackInfo ci, @Local(argsOnly = true) LivingEntity entity) {
        jojoha$lost = Limbs.of(entity);
    }

    @Inject(method = "setPartVisibility", at = @At("TAIL"))
    private void jojoha$noSleeveForLostLimbs(HumanoidModel<?> model, EquipmentSlot slot, CallbackInfo ci) {
        if ((jojoha$lost & Limbs.RIGHT_ARM) != 0) {
            model.rightArm.visible = false;
        }
        if ((jojoha$lost & Limbs.LEFT_ARM) != 0) {
            model.leftArm.visible = false;
        }
        if ((jojoha$lost & Limbs.RIGHT_LEG) != 0) {
            model.rightLeg.visible = false;
        }
        if ((jojoha$lost & Limbs.LEFT_LEG) != 0) {
            model.leftLeg.visible = false;
        }
    }
}
