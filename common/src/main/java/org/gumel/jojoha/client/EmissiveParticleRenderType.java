package org.gumel.jojoha.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

/**
 * An additively-blended particle pass, so Stand particles actually emit light instead of merely
 * being drawn at full brightness.
 *
 * <p>Fullbright alone (which is all {@code getLightColor() == 240} buys) stops a particle being
 * darkened by the world, but it still composites <em>over</em> whatever is behind it. Additive
 * blending makes overlapping motes sum toward white the way real light does, which is the whole
 * difference between a flat sprite and something that looks lit from within.
 *
 * <p>Identical to vanilla's {@code PARTICLE_SHEET_TRANSLUCENT} setup other than the blend
 * function - same atlas, same shader, same depth handling - so it slots into the engine's normal
 * batching without special treatment.
 *
 * @see org.gumel.jojoha.mixin.client.ParticleEngineMixin for how the engine is taught to draw it
 */
public final class EmissiveParticleRenderType implements ParticleRenderType {
    public static final ParticleRenderType INSTANCE = new EmissiveParticleRenderType();

    private EmissiveParticleRenderType() {
    }

    @Override
    public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
        RenderSystem.enableBlend();
        // SRC_ALPHA / ONE: the destination is added to rather than replaced, so stacked particles
        // build brightness instead of averaging out.
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getParticleShader);

        return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
    }

    @Override
    public String toString() {
        return "jojoha:emissive";
    }
}
