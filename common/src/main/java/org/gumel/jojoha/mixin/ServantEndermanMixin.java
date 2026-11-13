package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.stand.heaven.Servitude;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderMan.class)
public abstract class ServantEndermanMixin {
    @Inject(method = "isLookingAtMe", at = @At("HEAD"), cancellable = true)
    private void jojoha$masterMayLook(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (Servitude.servesAgainst((EnderMan) (Object) this, player)) {
            cir.setReturnValue(false);
        }
    }
}
