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

/**
 * Drops the world to black and white for a moment when a skull gives way.
 *
 * <p>This replaces a cut to black with a skull drawn on top of it. That version did what it said,
 * but it was the wrong tool: cutting the world away and putting a picture in its place is film
 * grammar, and this move is not a film. It is something the player is doing, in the world, while
 * still holding the controls. Taking the colour out of what they are already looking at punctuates
 * the hit without taking the hit away from them.
 *
 * <h2>Why a shader and not an overlay</h2>
 *
 * <p>Every overlay is additive by construction - it can put a wash, a tint or a fill in front of the
 * picture, and all three read as a sheet of colour rather than as the picture changing.
 * Desaturation cannot be faked that way at all: there is no colour you can draw over red that turns
 * it grey. It has to happen to the pixels, which means a pass.
 *
 * <p>The plumbing is {@link TimeStopPost}'s, deliberately - render into a scratch target, blit it
 * back over the main one. That is the whole of it.
 */
public final class ImpactFramePost {
    /**
     * How far apart the surviving greys are pushed at full strength, and the brightness they are
     * pushed around.
     *
     * <p>The first version of this used 2.35 around a pivot of 0.5 and came out as a dark tint
     * rather than a black and white one. Both numbers were wrong in the same direction: a lit
     * Minecraft frame averages around a third rather than a half, so pivoting at 0.5 put nearly
     * every pixel on the losing side of the push, and a contrast of 2.35 then drove them most of the
     * way to black. At a luma of 0.35 that is 0.15 out - which is exactly what a dark tint is.
     *
     * <p>Pivoting where the picture actually sits keeps the overall level where it was, and once the
     * level holds, the contrast barely has to do anything to read as hard.
     */
    private static final float CONTRAST = 1.22F;
    private static final float PIVOT = 0.38F;


    private static RenderTarget scratch;

    private ImpactFramePost() {
    }

    public static void render() {
        ShaderInstance shader = JojohaShaders.impact();
        if (shader == null) {
            return;
        }

        float strength = ImpactFrame.strength();
        if (strength <= 0.001F) {
            // Worth the early return: this is called every frame of every session and does nothing
            // in almost all of them.
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        RenderTarget scratchTarget = ensure(scratch, main.width, main.height);
        scratch = scratchTarget;

        shader.setSampler("DiffuseSampler", main.getColorTextureId());
        shader.safeGetUniform("Impact").set(strength, CONTRAST, PIVOT, 0F);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.viewport(0, 0, main.width, main.height);

        scratchTarget.bindWrite(false);
        RenderSystem.setShader(() -> shader);

        BufferBuilder builder = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        builder.addVertex(-1F, -1F, 0F);
        builder.addVertex(1F, -1F, 0F);
        builder.addVertex(1F, 1F, 0F);
        builder.addVertex(-1F, 1F, 0F);
        BufferUploader.drawWithShader(builder.buildOrThrow());

        shader.clear();
        scratchTarget.unbindWrite();

        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, scratchTarget.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, main.width,
                main.height, GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST);

        main.bindWrite(true);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /** A colour-only target the size of the screen, rebuilt when the window changes. */
    private static RenderTarget ensure(RenderTarget target, int width, int height) {
        if (target == null || target.width != width || target.height != height) {
            RenderTarget created = new TextureTarget(width, height, false, Minecraft.ON_OSX);
            created.setClearColor(0F, 0F, 0F, 0F);
            if (target != null) {
                target.destroyBuffers();
            }
            return created;
        }
        return target;
    }
}
