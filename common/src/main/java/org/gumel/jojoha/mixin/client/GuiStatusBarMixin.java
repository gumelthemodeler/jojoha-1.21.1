package org.gumel.jojoha.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.gumel.jojoha.client.BlindSense;
import org.gumel.jojoha.client.CentralBarOverlay;
import org.gumel.jojoha.client.ManifestCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Gui.class)
public abstract class GuiStatusBarMixin {
    private boolean jojoha$lifted;

    @Unique
    private boolean jojoha$itemNameLifted;

    @Unique
    private boolean jojoha$chatLifted;

    @Inject(method = "isExperienceBarVisible", at = @At("HEAD"), cancellable = true)
    private void jojoha$hideExperienceBar(CallbackInfoReturnable<Boolean> cir) {
        if (CentralBarOverlay.coversExperienceBar()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void jojoha$hideCrosshairDuringManifestation(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (ManifestCamera.active() || BlindSense.active()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void jojoha$hideExperienceLevel(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (CentralBarOverlay.coversExperienceBar()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPlayerHealth", at = @At("HEAD"))
    private void jojoha$liftStatus(GuiGraphics graphics, CallbackInfo ci) {
        jojoha$lift(graphics);
    }

    @Inject(method = "renderPlayerHealth", at = @At("RETURN"))
    private void jojoha$dropStatus(GuiGraphics graphics, CallbackInfo ci) {
        jojoha$drop(graphics);
    }

    @Inject(method = "renderVehicleHealth", at = @At("HEAD"))
    private void jojoha$liftVehicle(GuiGraphics graphics, CallbackInfo ci) {
        jojoha$lift(graphics);
    }

    @Inject(method = "renderVehicleHealth", at = @At("RETURN"))
    private void jojoha$dropVehicle(GuiGraphics graphics, CallbackInfo ci) {
        jojoha$drop(graphics);
    }

    @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At("HEAD"))
    private void jojoha$liftItemName(GuiGraphics graphics, CallbackInfo ci) {
        jojoha$itemNameLifted = jojoha$push(graphics);
    }

    @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At("RETURN"))
    private void jojoha$dropItemName(GuiGraphics graphics, CallbackInfo ci) {
        if (jojoha$itemNameLifted) {
            graphics.pose().popPose();
            jojoha$itemNameLifted = false;
        }
    }

    @Inject(method = "renderChat(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"))
    private void jojoha$liftChat(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        jojoha$chatLifted = jojoha$push(graphics);
    }

    @Inject(method = "renderChat(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("RETURN"))
    private void jojoha$dropChat(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (jojoha$chatLifted) {
            graphics.pose().popPose();
            jojoha$chatLifted = false;
        }
    }

    private boolean jojoha$push(GuiGraphics graphics) {
        int lift = CentralBarOverlay.statusLift();
        if (lift <= 0) {
            return false;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0F, -lift, 0F);
        return true;
    }

    private void jojoha$lift(GuiGraphics graphics) {
        int lift = CentralBarOverlay.statusLift();
        jojoha$lifted = lift > 0;

        if (jojoha$lifted) {
            graphics.pose().pushPose();
            graphics.pose().translate(0F, -lift, 0F);
        }
    }

    private void jojoha$drop(GuiGraphics graphics) {
        if (jojoha$lifted) {
            graphics.pose().popPose();
            jojoha$lifted = false;
        }
    }
}
