package org.gumel.jojoha.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import org.gumel.jojoha.fortune.Kindred;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvoidEntityGoal.class)
public abstract class KindredAvoidMixin {
    @Shadow
    @Final
    protected PathfinderMob mob;

    @Shadow
    protected LivingEntity toAvoid;

    @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
    private void jojoha$kindred(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && Kindred.welcome(mob, toAvoid)) {
            toAvoid = null;
            cir.setReturnValue(false);
        }
    }
}
