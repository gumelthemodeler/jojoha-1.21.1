package org.gumel.jojoha.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import org.gumel.jojoha.block.RespawnTotems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class RespawnTotemMixin {
    @Inject(method = "findRespawnPositionAndUseSpawnBlock", at = @At("HEAD"), cancellable = true)
    private void jojoha$riseAtTheTotem(boolean conqueredEnd, DimensionTransition.PostDimensionTransition after,
                                        CallbackInfoReturnable<DimensionTransition> cir) {
        DimensionTransition risen = RespawnTotems.respawn((ServerPlayer) (Object) this, conqueredEnd, after);
        if (risen != null) {
            cir.setReturnValue(risen);
        }
    }
}
