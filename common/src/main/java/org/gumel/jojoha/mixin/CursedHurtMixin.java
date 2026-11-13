package org.gumel.jojoha.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.body.Limbs;
import org.gumel.jojoha.fortune.Cursed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class CursedHurtMixin {
    @Unique
    private DamageSource jojoha$hurting;

    @Unique
    private float jojoha$before;

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void jojoha$bodyRefuses(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        if (Cursed.refusesBlow(source) || Limbs.refusesBlow(source)) {
            cir.setReturnValue(false);
            return;
        }
        jojoha$hurting = source;
        jojoha$before = self.getHealth();
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void jojoha$bodyStruck(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (jojoha$hurting == null) {
            return;
        }
        jojoha$hurting = null;
        if (cir.getReturnValueZ() && self.isAlive() && Limbs.severable(self)) {
            Limbs.struck(self, source, Math.max(jojoha$before - self.getHealth(), 0F));
        }
    }

    @ModifyVariable(method = "setHealth", at = @At("HEAD"), argsOnly = true)
    private float jojoha$cursedFloor(float health) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (jojoha$hurting == null) {
            return health;
        }
        return Cursed.survive(self, jojoha$hurting, self.getHealth(), health);
    }
}
