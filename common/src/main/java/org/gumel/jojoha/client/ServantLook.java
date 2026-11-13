package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public final class ServantLook extends RenderType {
    private static final ShaderStateShard SHADER = new ShaderStateShard(JojohaShaders::servant);

    private static final Function<ResourceLocation, RenderType> DRAINED = Util.memoize(texture -> create(
            "jojoha_servant", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, TRANSIENT_BUFFER_SIZE, true, true,
            CompositeState.builder()
                    .setShaderState(SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .createCompositeState(true)));

    private ServantLook(String name, Runnable setup, Runnable clear) {
        super(name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, TRANSIENT_BUFFER_SIZE, false, false,
                setup, clear);
    }

    public static RenderType drained(ResourceLocation texture) {
        if (JojohaShaders.servant() == null || ShaderPacks.active()) {
            return null;
        }
        return DRAINED.apply(texture);
    }
}
