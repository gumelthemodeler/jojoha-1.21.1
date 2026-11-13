package org.gumel.jojoha.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.combat.HitWindow;
import org.gumel.jojoha.combat.StatPerkEffects;
import org.gumel.jojoha.fortune.Fortunes;
import org.gumel.jojoha.item.Provisions;
import org.gumel.jojoha.trait.TraitEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class StatPerkDamageMixin {
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void jojoha$slipAway(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() && StatPerkEffects.slippedAway(self, source)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void jojoha$defence(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() && Defence.refuses(self, source, amount)) {
            cir.setReturnValue(false);
        }
    }

    @Unique
    private float jojoha$healthBefore;

    @Inject(method = "hurt", at = @At("HEAD"))
    private void jojoha$healthBefore(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        jojoha$healthBefore = self.getHealth();
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void jojoha$struck(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && (Object) this instanceof ServerPlayer player) {
            HitWindow.landed(player, amount);
            Defence.struck(player, source, jojoha$healthBefore - player.getHealth());
        }
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, index = 2)
    private float jojoha$statPerks(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return amount;
        }
        return HitWindow.allowed(self, DamageScale.withinCeiling(amount,
                Fortunes.incoming(self, source,
                        TraitEffects.incoming(self, source,
                                StatPerkEffects.incoming(self, source,
                                        Provisions.incoming(self, source,
                                                Defence.scale(self, source, amount)))))));
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void jojoha$lastStand(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!self.level().isClientSide() && StatPerkEffects.lastStand(self, source)) {
            cir.setReturnValue(true);
        }
    }
}
