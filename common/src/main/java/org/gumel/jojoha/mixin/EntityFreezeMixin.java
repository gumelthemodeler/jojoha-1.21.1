package org.gumel.jojoha.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.gumel.jojoha.stand.skill.FrozenMotion;
import org.gumel.jojoha.stand.skill.TimeStopSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class EntityFreezeMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void jojoha$freezeCaughtEntities(Entity entity, CallbackInfo ci) {
        if (entity instanceof Player || entity instanceof Projectile) {
            return;
        }

        if (TimeStopSystem.freezes(entity)) {
            FrozenMotion.hold(entity);
            ci.cancel();
        }
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
    private void jojoha$carryCaughtPassengers(Entity vehicle, Entity passenger, CallbackInfo ci) {
        if (passenger instanceof Player || passenger.isRemoved() || passenger.getVehicle() != vehicle) {
            return;
        }

        if (TimeStopSystem.freezes(passenger)) {
            FrozenMotion.carry(vehicle, passenger);
            ci.cancel();
        }
    }
}
