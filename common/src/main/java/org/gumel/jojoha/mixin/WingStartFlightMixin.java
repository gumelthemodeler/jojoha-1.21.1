package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.ultimate.WingFlight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class WingStartFlightMixin {
    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void jojoha$wingsTakeOff(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (!self.isFallFlying() && WingFlight.canGlide(self) && WingFlight.out(self)) {
            self.startFallFlying();
            cir.setReturnValue(true);
        }
    }
}
