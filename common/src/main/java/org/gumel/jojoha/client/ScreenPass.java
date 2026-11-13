package org.gumel.jojoha.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL30;

public final class ScreenPass {
    private RenderTarget scratch;

    public void draw(ShaderInstance shader) {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        scratch = ensure(scratch, main.width, main.height);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.viewport(0, 0, main.width, main.height);

        scratch.bindWrite(false);
        RenderSystem.setShader(() -> shader);

        BufferBuilder builder = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(-1F, -1F, 0F);
        builder.addVertex(1F, -1F, 0F);
        builder.addVertex(1F, 1F, 0F);
        builder.addVertex(-1F, 1F, 0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());

        shader.clear();
        scratch.unbindWrite();

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, scratch.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);

        main.bindWrite(true);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static RenderTarget ensure(RenderTarget target, int width, int height) {
        if (target == null) {
            RenderTarget created = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            created.setFilterMode(GL30.GL_NEAREST);
            return created;
        }

        if (target.width != width || target.height != height) {
            target.resize(width, height, Minecraft.ON_OSX);
        }
        return target;
    }
}
