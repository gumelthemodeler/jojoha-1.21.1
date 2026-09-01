package org.gumel.jojoha.mixin.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.SequencedMap;

/** Reaches the buffer source's fixed-buffer map so a render type can be given one. */
@Mixin(MultiBufferSource.BufferSource.class)
public interface BufferSourceAccessor {

    @Accessor("fixedBuffers")
    SequencedMap<RenderType, ByteBufferBuilder> jojoha$fixedBuffers();
}
