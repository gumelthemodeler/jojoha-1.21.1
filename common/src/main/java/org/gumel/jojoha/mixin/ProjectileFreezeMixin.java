package org.gumel.jojoha.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.gumel.jojoha.stand.skill.FrozenMotion;
import org.gumel.jojoha.stand.skill.TimeStopSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ProjectileFreezeMixin {
    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void jojoha$freezeHeldProjectiles(Entity entity, CallbackInfo ci) {
        if (entity instanceof Projectile projectile && TimeStopSystem.holdIfInside(projectile)) {
            FrozenMotion.hold(projectile);
            ci.cancel();
        }
    }
}
