package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.stand.StandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class HeldPlayerMixin {
    @Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
    private void jojoha$stayInTheHand(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (self.getVehicle() instanceof StandEntity stand && stand.heldEntity() == self) {
            cir.setReturnValue(false);
        }
    }
}
