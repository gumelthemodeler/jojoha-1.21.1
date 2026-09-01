package org.gumel.jojoha.mixin.client;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.gumel.jojoha.client.EmissiveParticleRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Teaches the particle engine to draw {@link EmissiveParticleRenderType}.
 *
 * <p>This is necessary because {@code RENDER_ORDER} is a hardcoded five-entry {@code ImmutableList}
 * and the render loop only ever walks it. A particle returning a render type outside that list is
 * still ticked, aged and removed perfectly normally - it simply never gets drawn, which looks
 * exactly like a broken texture path and is a genuinely nasty thing to debug. Appending the type
 * here is the only way a custom pass gets rendered at all.
 *
 * <p>It is inserted <em>before</em> the translucent pass rather than at the end, and that ordering
 * is deliberate: the engine only calls {@code disableBlend()} when it finishes, never
 * {@code defaultBlendFunc()}, so whichever pass ran last leaves its blend function behind
 * globally. Letting vanilla's translucent pass run afterwards means it restores the default
 * function, and nothing downstream inherits additive blending.
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow
    @Final
    @Mutable
    private static List<ParticleRenderType> RENDER_ORDER;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void jojoha$registerEmissivePass(CallbackInfo ci) {
        List<ParticleRenderType> order = new ArrayList<>(RENDER_ORDER);
        int translucent = order.indexOf(ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT);
        order.add(translucent >= 0 ? translucent : order.size(), EmissiveParticleRenderType.INSTANCE);
        RENDER_ORDER = ImmutableList.copyOf(order);
    }
}
