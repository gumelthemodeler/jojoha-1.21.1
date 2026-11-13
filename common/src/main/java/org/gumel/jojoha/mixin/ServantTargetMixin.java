package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.gumel.jojoha.stand.heaven.Servitude;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Mob.class)
public abstract class ServantTargetMixin {
    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity jojoha$neverTheMaster(LivingEntity target) {
        return target != null && Servitude.servesAgainst((Mob) (Object) this, target) ? null : target;
    }
}
