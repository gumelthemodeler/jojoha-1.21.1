package org.gumel.jojoha.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class StandKnockbackMixin {

    @ModifyExpressionValue(method = "hurt",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/damagesource/DamageSource;getSourcePosition()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 jojoha$pushedFromTheStand(Vec3 from, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (from == null || self.level().isClientSide() || !(source.getEntity() instanceof Player owner)
                || source.getDirectEntity() != owner) {
            return from;
        }
        StandEntity stand = StandEntity.findFor(owner);
        if (stand == null || stand.getStandType().form().isBound() || stand.getTrustTier().isPartialManifestation()) {
            return from;
        }
        Vec3 at = stand.position();
        if (stand.isPiloted() || at.distanceToSqr(self.position()) < from.distanceToSqr(self.position())) {
            return at;
        }
        return from;
    }
}
