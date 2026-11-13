package org.gumel.jojoha.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import org.gumel.jojoha.client.TimeStopView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientAnimateFreezeMixin {
    @Inject(method = "animateTick", at = @At("HEAD"), cancellable = true)
    private void jojoha$stillWorldDoesNotFlicker(int x, int y, int z, CallbackInfo ci) {
        if (TimeStopView.holdsPosition(x, y, z)) {
            ci.cancel();
        }
    }
}
