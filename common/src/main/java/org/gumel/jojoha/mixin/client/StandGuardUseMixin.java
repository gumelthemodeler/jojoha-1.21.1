package org.gumel.jojoha.mixin.client;

import net.minecraft.client.Minecraft;
import org.gumel.jojoha.client.ParryInput;
import org.gumel.jojoha.client.StandCombatInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class StandGuardUseMixin {
    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void jojoha$guardInsteadOfUse(CallbackInfo ci) {
        ParryInput.useStarted();
        if (StandCombatInput.claimsUse()) {
            ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("TAIL"))
    private void jojoha$parryWhenNothingElse(CallbackInfo ci) {
        ParryInput.nothingElseTookIt();
    }
}
