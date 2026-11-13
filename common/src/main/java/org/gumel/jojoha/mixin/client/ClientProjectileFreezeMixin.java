package org.gumel.jojoha.mixin.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.gumel.jojoha.client.TimeStopView;
import org.gumel.jojoha.stand.skill.FrozenMotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientProjectileFreezeMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void jojoha$freezeHeldProjectiles(Entity entity, CallbackInfo ci) {
        if (!TimeStopView.active() || !(entity instanceof Projectile)) {
            return;
        }

        if (TimeStopView.clientHolds(entity)) {
            FrozenMotion.settle(entity);
            FrozenMotion.hold(entity);
            ci.cancel();
        }
    }
}
