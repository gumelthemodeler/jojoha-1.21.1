package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.gumel.jojoha.fortune.Trickster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class TrickThrowMixin {
    @Inject(method = "onHit", at = @At("HEAD"))
    private void jojoha$trickThrow(HitResult result, CallbackInfo ci) {
        Trickster.landed((Projectile) (Object) this, result);
    }
}
