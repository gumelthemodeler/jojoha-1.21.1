package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.body.Limbs;
import org.gumel.jojoha.combat.Bleed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.effect.RegenerationMobEffect")
public abstract class LimbMendRegenMixin {
    @Inject(method = "applyEffectTick", at = @At("HEAD"))
    private void jojoha$mend(LivingEntity entity, int amplifier, CallbackInfoReturnable<Boolean> cir) {
        Limbs.mended(entity, 1F);
        Bleed.ease(entity);
    }
}
