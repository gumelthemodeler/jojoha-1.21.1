package org.gumel.jojoha.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.gumel.jojoha.client.StandTurn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class ScrollTurnMixin {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void jojoha$turnPlacement(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (window == Minecraft.getInstance().getWindow().getWindow() && StandTurn.scrolled(yOffset)) {
            ci.cancel();
        }
    }
}
