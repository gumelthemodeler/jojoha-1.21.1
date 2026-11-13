package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

public final class EmeraldGlow extends RenderType {
    public static final RenderType ADDITIVE = new EmeraldGlow(
            "jojoha_emerald_glow",
            () -> {
                RENDERTYPE_LIGHTNING_SHADER.setupRenderState();
                COLOR_WRITE.setupRenderState();
                NO_CULL.setupRenderState();
                LEQUAL_DEPTH_TEST.setupRenderState();
                LIGHTNING_TRANSPARENCY.setupRenderState();
            },
            () -> {
                LIGHTNING_TRANSPARENCY.clearRenderState();
                LEQUAL_DEPTH_TEST.clearRenderState();
                NO_CULL.clearRenderState();
                COLOR_WRITE.clearRenderState();
                RENDERTYPE_LIGHTNING_SHADER.clearRenderState();
            });

    private EmeraldGlow(String name, Runnable setup, Runnable clear) {
        super(name, DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1536, false, true, setup, clear);
    }
}
