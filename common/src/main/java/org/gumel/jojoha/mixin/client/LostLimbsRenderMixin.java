package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.gumel.jojoha.body.Limbs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LostLimbsRenderMixin {
    @Shadow
    protected EntityModel<?> model;

    @Unique
    private int jojoha$hid;

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V",
                    shift = At.Shift.AFTER))
    private void jojoha$hideLostLimbs(LivingEntity entity, float entityYaw, float partialTick,
                                      PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!(entity instanceof Mob) || !(model instanceof HumanoidModel<?> humanoid)) {
            return;
        }
        int lost = Limbs.of(entity);
        jojoha$hid = lost;
        if (lost != 0) {
            jojoha$show(humanoid, lost, false);
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void jojoha$restoreLimbs(LivingEntity entity, float entityYaw, float partialTick,
                                     PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (jojoha$hid != 0 && model instanceof HumanoidModel<?> humanoid) {
            jojoha$show(humanoid, jojoha$hid, true);
        }
        jojoha$hid = 0;
    }

    @Unique
    private static void jojoha$show(HumanoidModel<?> model, int limbs, boolean shown) {
        if ((limbs & Limbs.RIGHT_ARM) != 0) {
            model.rightArm.visible = shown;
        }
        if ((limbs & Limbs.LEFT_ARM) != 0) {
            model.leftArm.visible = shown;
        }
        if ((limbs & Limbs.RIGHT_LEG) != 0) {
            model.rightLeg.visible = shown;
        }
        if ((limbs & Limbs.LEFT_LEG) != 0) {
            model.leftLeg.visible = shown;
        }
    }
}
