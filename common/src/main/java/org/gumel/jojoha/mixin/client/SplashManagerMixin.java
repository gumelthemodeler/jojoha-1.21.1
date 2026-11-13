package org.gumel.jojoha.mixin.client;

import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.gumel.jojoha.client.JojoSplashes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SplashManager.class)
public abstract class SplashManagerMixin {
    @Inject(method = "apply(Ljava/util/List;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("TAIL"))
    private void jojoha$loadJojoSplashes(List<String> splashes, ResourceManager resources, ProfilerFiller profiler,
                                         CallbackInfo ci) {
        JojoSplashes.load(resources);
    }

    @Inject(method = "getSplash", at = @At("RETURN"), cancellable = true)
    private void jojoha$mixInJojoSplashes(CallbackInfoReturnable<SplashRenderer> cir) {
        cir.setReturnValue(JojoSplashes.pick(cir.getReturnValue()));
    }
}
