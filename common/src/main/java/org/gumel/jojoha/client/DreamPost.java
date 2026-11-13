package org.gumel.jojoha.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

public final class DreamPost {
    private static final float BACKDROP_DISTANCE = 10F;
    private static final float BACKDROP_REACH = 4000F;

    private static boolean late;

    private DreamPost() {
    }

    public static boolean deferred() {
        return ShaderPacks.active();
    }

    public static void skyDepth() {
        if (late) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        } else {
            RenderSystem.disableDepthTest();
        }
    }

    public static void render(Matrix4f modelView, Matrix4f projection, Camera camera, float partialTick) {
        if (!deferred() || !DreamSpirit.dreaming()) {
            return;
        }
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(projection), VertexSorting.DISTANCE_TO_ORIGIN);

        late = true;
        GL11.glDepthRange(1.0, 1.0);
        try {
            backdrop();
            DreamSky.render(modelView, projection, partialTick);
            DreamSpirit.renderSky(modelView, partialTick);
        } finally {
            GL11.glDepthRange(0.0, 1.0);
            late = false;
        }

        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.mul(modelView);
        RenderSystem.applyModelViewMatrix();
        DreamWater.render(new PoseStack(), camera, partialTick);
        stack.popMatrix();
        RenderSystem.applyModelViewMatrix();

        RenderSystem.restoreProjectionMatrix();
    }

    private static void backdrop() {
        float[] fog = RenderSystem.getShaderFogColor();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(-BACKDROP_REACH, -BACKDROP_REACH, -BACKDROP_DISTANCE).setColor(fog[0], fog[1], fog[2], 1F);
        buffer.addVertex(BACKDROP_REACH, -BACKDROP_REACH, -BACKDROP_DISTANCE).setColor(fog[0], fog[1], fog[2], 1F);
        buffer.addVertex(BACKDROP_REACH, BACKDROP_REACH, -BACKDROP_DISTANCE).setColor(fog[0], fog[1], fog[2], 1F);
        buffer.addVertex(-BACKDROP_REACH, BACKDROP_REACH, -BACKDROP_DISTANCE).setColor(fog[0], fog[1], fog[2], 1F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }
}
