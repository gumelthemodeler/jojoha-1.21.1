package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import org.gumel.jojoha.client.GoldGlint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives the gold glint types a buffer of their own, which is the difference between drawing and not.
 *
 * <h2>What goes wrong without this</h2>
 *
 * <p>A render type with no buffer of its own is written into the shared one, and {@code endBatch()}
 * drains the shared buffer <em>first</em> - {@code endLastBatch()} runs before it iterates the fixed
 * buffers at all. So the glint was being drawn before the item had written any depth, and since a
 * glint tests for equal depth and writes only colour, every one of its pixels was rejected. The
 * effect was not a wrong colour but no glint whatsoever, which is a good reminder that "nothing
 * appears" usually means the geometry went somewhere, not that it was never built.
 *
 * <p>Vanilla's own glint types are in this map for exactly this reason. Ours are appended, so they
 * come last in the sequence and therefore draw after the item sheets - which is what makes the
 * equal-depth test pass.
 */
@Mixin(RenderBuffers.class)
public abstract class RenderBuffersMixin {

    @Shadow @org.spongepowered.asm.mixin.Final
    private MultiBufferSource.BufferSource bufferSource;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void jojoha$addGoldGlintBuffers(int size, CallbackInfo ci) {
        BufferSourceAccessor accessor = (BufferSourceAccessor) (Object) bufferSource;
        for (RenderType type : new RenderType[]{GoldGlint.ITEM, GoldGlint.DIRECT}) {
            accessor.jojoha$fixedBuffers().put(type, new ByteBufferBuilder(type.bufferSize()));
        }
    }
}
