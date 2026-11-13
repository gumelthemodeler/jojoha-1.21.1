package org.gumel.jojoha.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.client.BarrageFx;
import org.gumel.jojoha.client.CombatShake;
import org.gumel.jojoha.client.DreamWaking;
import org.gumel.jojoha.client.ManifestCamera;
import org.gumel.jojoha.client.MeteorImpactFx;
import org.gumel.jojoha.client.PossessionClient;
import org.gumel.jojoha.client.SkullFlashFx;
import org.gumel.jojoha.client.StandAwakeningRays;
import org.gumel.jojoha.client.TimeStopClient;
import org.gumel.jojoha.client.fx.ImpactDirector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Shadow
    protected abstract void setPosition(Vec3 pos);

    @ModifyVariable(method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean jojoha$standBackForTheManifestation(boolean detached) {
        return detached || ManifestCamera.detaches(
                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
    }

    @Inject(method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At("TAIL"))
    private void jojoha$frameTheManifestation(BlockGetter level, Entity entity, boolean detached,
                                              boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || entity != minecraft.player) {
            return;
        }
        ManifestCamera.Shot shot = ManifestCamera.frame(((Camera) (Object) this).getPosition(),this.yRot, this.xRot, partialTick);
        if (shot != null) {
            setRotation(shot.yaw(), shot.pitch());
            setPosition(shot.position());
        }
    }

    @ModifyExpressionValue(method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
    private float jojoha$turnWithTheMouseInsideTheBody(float yRot, @Local(argsOnly = true) Entity entity,
                                                       @Local(argsOnly = true) float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        return inside(minecraft, entity) ? minecraft.player.getViewYRot(partialTick) : yRot;
    }

    @ModifyExpressionValue(method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
    private float jojoha$lookUpAndDownInsideTheBody(float xRot, @Local(argsOnly = true) Entity entity,
                                                    @Local(argsOnly = true) float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        return inside(minecraft, entity) ? minecraft.player.getViewXRot(partialTick) : xRot;
    }

    private static boolean inside(Minecraft minecraft, Entity entity) {
        return minecraft.player != null && entity != minecraft.player
                && PossessionClient.possessing();
    }

    @Inject(method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V",
            at = @At("TAIL"))
    private void jojoha$shakeOnAwakening(BlockGetter level, Entity entity, boolean detached,
                                         boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || entity != minecraft.player) {
            return;
        }

        float clientTime = (float) minecraft.level.getGameTime() + partialTick;

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

        float[] crusher = SkullFlashFx.cameraShake();
        if (crusher != null) {
            yawShake += crusher[0];
            pitchShake += crusher[1];
        }

        float[] meteor = MeteorImpactFx.cameraShake();
        if (meteor != null) {
            yawShake += meteor[0];
            pitchShake += meteor[1];
        }

        float[] waking = DreamWaking.cameraShake(partialTick);
        if (waking != null) {
            yawShake += waking[0];
            pitchShake += waking[1];
        }

        float[] blows = CombatShake.cameraShake();
        if (blows != null) {
            yawShake += blows[0];
            pitchShake += blows[1];
        }

        float[] kick = ImpactDirector.cameraKick();
        if (kick != null) {
            yawShake += kick[0];
            pitchShake += kick[1];
        }

        float[] barrage = BarrageFx.cameraShake();
        if (barrage != null) {
            yawShake += barrage[0];
            pitchShake += barrage[1];
        }

        float[] charging = null;
        if (charging != null) {
            yawShake += charging[0];
            pitchShake += charging[1];
        }

        if (yawShake != 0F || pitchShake != 0F) {
            setRotation(this.yRot + yawShake, this.xRot + pitchShake);
        }
    }
}
