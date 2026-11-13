package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

public final class RedGlint extends RenderType {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/misc/enchanted_glint_item_red.png");

    private static final TextureStateShard SHEET = new TextureStateShard(TEXTURE, true, false);

    public static final RenderType ITEM = new RedGlint(
            "jojoha_red_glint",
            () -> {
                RENDERTYPE_GLINT_SHADER.setupRenderState();
                SHEET.setupRenderState();
                COLOR_WRITE.setupRenderState();
                NO_CULL.setupRenderState();
                EQUAL_DEPTH_TEST.setupRenderState();
                GLINT_TRANSPARENCY.setupRenderState();
                GLINT_TEXTURING.setupRenderState();
            },
            () -> {
                GLINT_TEXTURING.clearRenderState();
                GLINT_TRANSPARENCY.clearRenderState();
                EQUAL_DEPTH_TEST.clearRenderState();
                NO_CULL.clearRenderState();
                COLOR_WRITE.clearRenderState();
                SHEET.clearRenderState();
                RENDERTYPE_GLINT_SHADER.clearRenderState();
            });

    public static final RenderType DIRECT = new RedGlint(
            "jojoha_red_glint_direct",
            () -> {
                RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER.setupRenderState();
                SHEET.setupRenderState();
                COLOR_WRITE.setupRenderState();
                NO_CULL.setupRenderState();
                EQUAL_DEPTH_TEST.setupRenderState();
                GLINT_TRANSPARENCY.setupRenderState();
                ENTITY_GLINT_TEXTURING.setupRenderState();
            },
            () -> {
                ENTITY_GLINT_TEXTURING.clearRenderState();
                GLINT_TRANSPARENCY.clearRenderState();
                EQUAL_DEPTH_TEST.clearRenderState();
                NO_CULL.clearRenderState();
                COLOR_WRITE.clearRenderState();
                SHEET.clearRenderState();
                RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER.clearRenderState();
            });

    private RedGlint(String name, Runnable setup, Runnable clear) {
        super(name, DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, false, false,
                setup, clear);
    }
}
