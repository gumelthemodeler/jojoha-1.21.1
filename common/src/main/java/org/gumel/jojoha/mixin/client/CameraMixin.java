package org.gumel.jojoha.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.gumel.jojoha.client.StandAwakeningRays;
import org.gumel.jojoha.client.TimeStopCharge;
import org.gumel.jojoha.client.TimeStopClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shakes the camera as a Stand awakens.
 *
 * <p>Applied at the end of {@code setup}, on top of whatever rotation vanilla just resolved, so it
 * layers over normal look control rather than fighting it - the player keeps full control of where
 * they're facing while the world rattles around them.
 *
 * <p>Only the awakening player's own camera moves. Everyone else sees the rays and the flash but
 * keeps a steady view, since shaking a bystander's screen for something happening to someone else
 * is disorienting rather than dramatic.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At("TAIL"))
    private void jojoha$shakeOnAwakening(BlockGetter level, Entity entity, boolean detached,
                                         boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || entity != minecraft.player) {
            return;
        }

        float clientTime = (float) minecraft.level.getGameTime() + partialTick;

        // Independent sources, summed rather than one winning. They are different events and can
        // overlap - a stop cast during an awakening, or the wind-up tremble running into the jolt of
        // the cast itself - and picking one would silently drop the others.
        float yawShake = 0F;
        float pitchShake = 0F;

        float[] awakening = StandAwakeningRays.cameraShake(minecraft.player, clientTime);
        if (awakening != null) {
            yawShake += awakening[0];
            pitchShake += awakening[1];
        }

        float[] timeStop = TimeStopClient.cameraShake(clientTime);
        if (timeStop != null) {
            yawShake += timeStop[0];
            pitchShake += timeStop[1];
        }

        float[] crusher = org.gumel.jojoha.client.SkullFlashFx.cameraShake();
        if (crusher != null) {
            yawShake += crusher[0];
            pitchShake += crusher[1];
        }

        float[] charging = TimeStopCharge.cameraShake();
        if (charging != null) {
            yawShake += charging[0];
            pitchShake += charging[1];
        }

        if (yawShake != 0F || pitchShake != 0F) {
            setRotation(this.yRot + yawShake, this.xRot + pitchShake);
        }
    }
}
