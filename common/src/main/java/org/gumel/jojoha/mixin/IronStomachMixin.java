package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import org.gumel.jojoha.trait.TraitEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class IronStomachMixin {
    @Inject(method = "addEatEffect", at = @At("HEAD"), cancellable = true)
    private void jojoha$ironStomach(FoodProperties food, CallbackInfo ci) {
        if (TraitEffects.ironStomach((LivingEntity) (Object) this, food)) {
            ci.cancel();
        }
    }
}
