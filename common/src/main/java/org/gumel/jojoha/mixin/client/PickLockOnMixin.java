package org.gumel.jojoha.mixin.client;

import net.minecraft.client.Minecraft;
import org.gumel.jojoha.client.LockOn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class PickLockOnMixin {
    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void jojoha$lockOnInstead(CallbackInfo ci) {
        if (LockOn.claimsPick()) {
            ci.cancel();
        }
    }
}
