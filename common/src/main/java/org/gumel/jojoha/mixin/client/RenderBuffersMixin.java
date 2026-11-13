package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import org.gumel.jojoha.client.RedGlint;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderBuffers.class)
public abstract class RenderBuffersMixin {

    @Shadow @Final
    private MultiBufferSource.BufferSource bufferSource;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void jojoha$addRedGlintBuffers(int size, CallbackInfo ci) {
        BufferSourceAccessor accessor = (BufferSourceAccessor) (Object) bufferSource;
        for (RenderType type : new RenderType[]{RedGlint.ITEM, RedGlint.DIRECT}) {
            accessor.jojoha$fixedBuffers().put(type, new ByteBufferBuilder(type.bufferSize()));
        }
    }
}
