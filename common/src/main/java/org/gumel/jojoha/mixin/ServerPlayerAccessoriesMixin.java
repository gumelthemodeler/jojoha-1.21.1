package org.gumel.jojoha.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import org.gumel.jojoha.accessory.Accessories;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerAccessoriesMixin {
    @Inject(method = "restoreFrom", at = @At("TAIL"))
    private void jojoha$keepAccessories(ServerPlayer old, boolean alive, CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (alive || self.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || old.isSpectator()) {
            Accessories.copy(old, self);
        }
    }
}
