package org.gumel.jojoha.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.client.HeavenSky;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class HeavenSkyMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void jojoha$heavenSky(Vec3 at, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(HeavenSky.sky(cir.getReturnValue(), partialTick));
    }
}
