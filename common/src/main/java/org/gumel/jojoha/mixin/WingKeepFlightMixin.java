package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.ultimate.WingFlight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class WingKeepFlightMixin {
    @Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
    private void jojoha$wingsKeepFlying(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() && self instanceof Player player && player.isFallFlying()
                && WingFlight.canGlide(player) && WingFlight.out(player)) {
            ci.cancel();
        }
    }
}
