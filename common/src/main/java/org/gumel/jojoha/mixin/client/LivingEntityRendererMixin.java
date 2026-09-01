package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.gumel.jojoha.client.FrozenEntityFx;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.stand.StandEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.client.StandAwakeningRays;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Washes an awakening player out to solid white for a moment.
 *
 * <p>Rides vanilla's existing white-overlay channel rather than inventing a glow: this is the same
 * value a creeper drives to flash white before detonating, so the whiteness is applied by the
 * regular entity shader over every part of the model at once - skin, armour and all - with no
 * extra render pass.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    /**
     * Stops something held by a Stand from sitting down.
     *
     * <p>A grabbed mob is a passenger of the Stand, which is what makes the hold smooth - the
     * vehicle positions it and nothing fights over the result. The cost is that vanilla reads
     * "passenger" as "riding something", and the renderer hands that straight to the model as
     * {@code EntityModel.riding}, which bends the legs into a saddle pose. Correct for a horse, and
     * wrong for a zombie being held off the ground by the throat.
     *
     * <p>There is no lever for this in 1.21.1 - the flag is assigned directly from
     * {@code isPassenger()} with nothing consulted in between, so redirecting that one call is the
     * whole of the fix. Narrow on purpose: it answers differently only for something riding a Stand,
     * so every real vehicle in the game still poses its riders exactly as before.
     */
    @Redirect(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;isPassenger()Z"))
    private boolean jojoha$notSittingInAStandsHand(LivingEntity entity) {
        return entity.isPassenger() && !(entity.getVehicle() instanceof StandEntity);
    }

    /**
     * Shakes anything held by a time stop, and keeps the caster and their Stand out of the
     * inversion.
     *
     * <p>The exclusion has to flush the batch. Entity geometry is buffered by render type and only
     * handed over once, at the end of the frame, so a uniform set while one entity is "being drawn"
     * would in fact apply to every entity sharing that render type. Forcing the batch out on the way
     * in and again on the way out is what makes the window around this one entity real.
     */
    @Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"))
    private void jojoha$frozenIn(LivingEntity entity, float entityYaw, float partialTick,
                                 PoseStack poseStack, MultiBufferSource buffers, int light,
                                 CallbackInfo ci) {
        if (!FrozenEntityFx.active()) {
            return;
        }

        poseStack.pushPose();

        // Only the tremble is decided here now. Colour used to be too, which meant flushing the
        // vertex batch either side of every living entity so that a uniform set for one body did
        // not leak onto every other body sharing its render type. TimeStopPost draws the exceptions
        // into a mask instead and asks the question once, in screen space.
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
        if (!FrozenEntityFx.active()) {
            return;
        }

        poseStack.popPose();
    }

    /** The player holding the stop, and the Stand that threw it - neither is caught by it. */
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
}
