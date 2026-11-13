package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.LinkedHashMap;

final class LayeredBuffers extends MultiBufferSource.BufferSource {

    LayeredBuffers() {
        super(new ByteBufferBuilder(1 << 14), new LinkedHashMap<>());
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        if (!fixedBuffers.containsKey(type)) {
            fixedBuffers.put(type, new ByteBufferBuilder(type.bufferSize()));
        }
        return super.getBuffer(type);
    }

    @Override
    public void endBatch() {
        endLastBatch();
        for (RenderType type : fixedBuffers.keySet()) {
            if (!shines(type)) {
                endBatch(type);
            }
        }
        for (RenderType type : fixedBuffers.keySet()) {
            if (shines(type)) {
                endBatch(type);
            }
        }
    }

    private static boolean shines(RenderType type) {
        return type.toString().contains("glint");
    }
}
