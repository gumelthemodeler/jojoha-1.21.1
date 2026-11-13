package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.pillar.PillarMen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PillarUnseenMixin {
    @Inject(method = "canBeSeenAsEnemy", at = @At("HEAD"), cancellable = true)
    private void jojoha$unseen(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide() && PillarMen.unseen(player)) {
            cir.setReturnValue(false);
        }
    }
}
