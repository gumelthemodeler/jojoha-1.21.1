package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.body.Limbs;
import org.gumel.jojoha.combat.Bleed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.effect.HealOrHarmMobEffect")
public abstract class LimbMendHealMixin {
    @Redirect(method = {"applyEffectTick", "applyInstantenousEffect"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V"))
    private void jojoha$mend(LivingEntity entity, float health) {
        entity.heal(health);
        Limbs.mended(entity, health);
        Bleed.ease(entity);
    }
}
