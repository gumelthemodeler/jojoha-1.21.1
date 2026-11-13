package org.gumel.jojoha.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.client.BankedTint;
import org.gumel.jojoha.client.DrainPale;
import org.gumel.jojoha.client.FleshBudFx;
import org.gumel.jojoha.client.FrozenEntityFx;
import org.gumel.jojoha.client.HeavenWorldFx;
import org.gumel.jojoha.client.PlayerMoveAnimation;
import org.gumel.jojoha.client.StandAwakeningRays;
import org.gumel.jojoha.client.TangledVinesLayer;
import org.gumel.jojoha.client.TargetShudder;
import org.gumel.jojoha.client.ThrownTumble;
import org.gumel.jojoha.client.WallWalkPose;
import org.gumel.jojoha.client.fx.HitStop;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.StandEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Shadow
    @Final
    protected List<RenderLayer<?, ?>> layers;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "<init>", at = @At("TAIL"))
    private void jojoha$addTangledVines(EntityRendererProvider.Context context,
                                        EntityModel<?> model, float shadowRadius,
                                        CallbackInfo ci) {
        this.layers.add(new TangledVinesLayer(
                (RenderLayerParent) (Object) this));
    }

    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;isPassenger()Z"))
    private boolean jojoha$notSittingInAStandsHand(LivingEntity entity) {
        return entity.isPassenger() && !(entity.getVehicle() instanceof StandEntity);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;setupRotations(Lnet/minecraft/world/entity/LivingEntity;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V"))
    private void jojoha$tumbleWhenThrown(LivingEntity entity, float entityYaw, float partialTick,
                                         PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        ThrownTumble.apply(entity, poseStack, partialTick);
        WallWalkPose.apply(entity, poseStack);
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V"))
    private void jojoha$moveRoot(LivingEntity entity, float entityYaw, float partialTick,
                                 PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (entity instanceof Player player && player.level() != null) {
            PlayerMoveAnimation.root(poseStack, player,
                    (float) player.level().getGameTime() + partialTick);
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void jojoha$frozenIn(LivingEntity entity, float entityYaw, float partialTick,
                                 PoseStack poseStack, MultiBufferSource buffers, int light,
                                 CallbackInfo ci) {
        jojoha$pushed = FrozenEntityFx.active() || HitStop.active();
        jojoha$pushed |= TargetShudder.active();
        if (!jojoha$pushed) {
            return;
        }

        poseStack.pushPose();

        Vec3 held = HitStop.offset(entity, partialTick);
        if (held != null) {
            poseStack.translate(held.x, held.y, held.z);
        }

        Vec3 shudder = TargetShudder.offset(entity, partialTick);
        if (shudder != null) {
            poseStack.translate(shudder.x, shudder.y, shudder.z);
        }

        if (!FrozenEntityFx.active()) {
            return;
        }

        if (jojoha$isCaster(entity)) {
            return;
        }

        float[] shake = FrozenEntityFx.shake(entity);
        if (shake != null) {
            poseStack.translate(shake[0], shake[1], shake[2]);
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"))
    private void jojoha$frozenOut(LivingEntity entity, float entityYaw, float partialTick,
                                  PoseStack poseStack, MultiBufferSource buffers, int light,
                                  CallbackInfo ci) {
        FleshBudFx.render(entity, poseStack, buffers, light, partialTick);

        if (!jojoha$pushed) {
            return;
        }

        jojoha$pushed = false;
        poseStack.popPose();
    }

    @Unique
    private boolean jojoha$pushed;

    @ModifyArg(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            index = 4)
    private int jojoha$bankedTint(int colour, @Local(argsOnly = true) LivingEntity entity) {
        return BankedTint.tint(entity.getId(), colour);
    }

    @Inject(method = "getOverlayCoords(Lnet/minecraft/world/entity/LivingEntity;F)I",
            at = @At("HEAD"), cancellable = true)
    private static void jojoha$hitStopFlash(LivingEntity entity, float whiteOverlayProgress,
                                            CallbackInfoReturnable<Integer> cir) {
        if (HitStop.flashing(entity)) {
            cir.setReturnValue(OverlayTexture.pack(
                    OverlayTexture.u(1F),
                    OverlayTexture.v(false)));
            return;
        }
        float risen = HeavenWorldFx.whiteness(entity);
        if (risen > 0F) {
            cir.setReturnValue(OverlayTexture.pack(
                    OverlayTexture.u(risen),
                    OverlayTexture.v(false)));
        }
    }

    private static boolean jojoha$isCaster(LivingEntity entity) {
        if (entity instanceof StandEntity) {
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && entity.getUUID().equals(minecraft.player.getUUID())
                && ClientPlayerDataCache.data.timeStopHeldTicks > 0;
    }

    @Inject(method = "getWhiteOverlayProgress(Lnet/minecraft/world/entity/LivingEntity;F)F",
            at = @At("HEAD"), cancellable = true)
    private void jojoha$standAwakeningFlash(LivingEntity entity, float partialTick,
                                            CallbackInfoReturnable<Float> cir) {
        if (!(entity instanceof Player player)) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float flash = StandAwakeningRays.whiteFlash(player, (float) minecraft.level.getGameTime() + partialTick);
        if (flash > 0F) {
            cir.setReturnValue(flash);
        }
    }

    @Inject(method = "getWhiteOverlayProgress(Lnet/minecraft/world/entity/LivingEntity;F)F",
            at = @At("RETURN"), cancellable = true)
    private void jojoha$drainedPale(LivingEntity entity, float partialTick,
                                    CallbackInfoReturnable<Float> cir) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float pale = DrainPale.paleness(entity.getId(),
                (float) minecraft.level.getGameTime() + partialTick);
        if (pale > cir.getReturnValueF()) {
            cir.setReturnValue(pale);
        }
    }
}
