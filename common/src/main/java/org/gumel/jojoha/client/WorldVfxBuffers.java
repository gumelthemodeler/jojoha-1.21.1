package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;

public final class WorldVfxBuffers extends MultiBufferSource.BufferSource {
    private static final int SHARED_BYTES = 256 * 1024;
    private static final String OPAQUE = "no_transparency";

    private final SequencedMap<RenderType, ByteBufferBuilder> perType;
    private final Map<RenderType, Boolean> solid = new HashMap<>();
    private boolean holding;

    private WorldVfxBuffers(SequencedMap<RenderType, ByteBufferBuilder> perType) {
        super(new ByteBufferBuilder(SHARED_BYTES), perType);
        this.perType = perType;
    }

    public static WorldVfxBuffers create() {
        return new WorldVfxBuffers(new LinkedHashMap<>());
    }

    public void hold(boolean holding) {
        this.holding = holding;
    }

    public boolean solid(RenderType type) {
        return this.solid.computeIfAbsent(type, key -> key.toString().contains(OPAQUE));
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        if (!this.perType.containsKey(type)) {
            this.perType.put(type, new ByteBufferBuilder(type.bufferSize()));
        }
        return super.getBuffer(type);
    }

    @Override
    public void endBatch(RenderType type) {
        if (this.holding && !solid(type)) {
            return;
        }
        super.endBatch(type);
    }
}
