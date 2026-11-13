package org.gumel.jojoha.mixin.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.client.HeavenWorldFx;
import org.gumel.jojoha.client.ServantLook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class ServantLookMixin {
    @Inject(method = "getRenderType", at = @At("RETURN"), cancellable = true)
    @SuppressWarnings("unchecked")
    private void jojoha$drainServant(LivingEntity entity, boolean bodyVisible, boolean translucent, boolean glowing,
                                     CallbackInfoReturnable<RenderType> cir) {
        if (cir.getReturnValue() == null || !HeavenWorldFx.serving(entity)) {
            return;
        }
        RenderType drained = ServantLook.drained(((EntityRenderer<LivingEntity>) (Object) this).getTextureLocation(entity));
        if (drained != null) {
            cir.setReturnValue(drained);
        }
    }
}
