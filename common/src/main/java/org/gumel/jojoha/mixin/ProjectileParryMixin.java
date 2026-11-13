package org.gumel.jojoha.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import org.gumel.jojoha.accessory.Relics;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.pillar.PillarMen;
import org.gumel.jojoha.stand.chariot.Chariot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ProjectileParryMixin {
    @Inject(method = "deflection", at = @At("HEAD"), cancellable = true)
    private void jojoha$parryProjectile(Projectile projectile, CallbackInfoReturnable<ProjectileDeflection> cir) {
        if (!((Object) this instanceof ServerPlayer defender) || projectile.getOwner() == defender) {
            return;
        }
        ProjectileDeflection parried = Defence.parryProjectile(defender, projectile);
        if (parried == null) {
            parried = Chariot.deflectProjectile(defender, projectile);
        }
        if (parried == null) {
            parried = Relics.windShell(defender, projectile);
        }
        if (parried == null) {
            parried = PillarMen.cloakDeflect(defender, projectile);
        }
        if (parried != null) {
            cir.setReturnValue(parried);
        }
    }
}
