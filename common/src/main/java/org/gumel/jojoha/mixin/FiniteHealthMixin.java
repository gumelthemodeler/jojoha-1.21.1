package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class FiniteHealthMixin {
    @ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true)
    private float jojoha$finiteHealth(float health) {
        if (!Float.isNaN(health)) {
            return health;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        float now = self.getHealth();
        return Float.isNaN(now) ? self.getMaxHealth() : now;
    }

    @ModifyVariable(method = "setAbsorptionAmount", at = @At("HEAD"), argsOnly = true)
    private float jojoha$finiteAbsorption(float amount) {
        return Float.isNaN(amount) ? 0F : amount;
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, index = 2)
    private float jojoha$finiteDamage(float amount) {
        return Float.isNaN(amount) ? 0F : Math.min(amount, Float.MAX_VALUE);
    }
}
