package org.gumel.jojoha.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;

public final class TimeStopMask {
    private static RenderTarget mask;
    private static ByteBufferBuilder bytes;
    private static MultiBufferSource.BufferSource buffers;
    private static boolean capturing;

    private TimeStopMask() {
    }

    public static boolean capturing() {
        return capturing;
    }

    public static boolean redirects(RenderTarget target) {
        return capturing && target != mask;
    }

    public static void bindMask() {
        if (mask != null) {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, mask.frameBufferId);
            RenderSystem.viewport(0, 0, mask.width, mask.height);
        }
    }

    public static int colourTexture() {
        return mask == null ? 0 : mask.getColorTextureId();
    }

    public static int depthTexture() {
        return mask == null ? 0 : mask.getDepthTextureId();
    }

    public static boolean capture(Matrix4f modelView, Matrix4f projection, Camera camera, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        RenderTarget main = minecraft.getMainRenderTarget();
        if (level == null) {
            return false;
        }

        ensure(main.width, main.height);

        mask.setClearColor(0F, 0F, 0F, 0F);
        mask.clear(Minecraft.ON_OSX);

        List<Entity> free = freeBodies(minecraft, level);
        if (free.isEmpty()) {
            main.bindWrite(true);
            return false;
        }

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        Vec3 cam = camera.getPosition();
        boolean hideGui = minecraft.options.hideGui;

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.set(modelView);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);

        mask.bindWrite(true);
        capturing = true;
        try {
            dispatcher.setRenderShadow(false);
            minecraft.options.hideGui = true;

            PoseStack poseStack = new PoseStack();
            for (Entity body : free) {
                double x = Mth.lerp(partialTick, body.xOld, body.getX()) - cam.x;
                double y = Mth.lerp(partialTick, body.yOld, body.getY()) - cam.y;
                double z = Mth.lerp(partialTick, body.zOld, body.getZ()) - cam.z;
                float yaw = Mth.lerp(partialTick, body.yRotO, body.getYRot());
                dispatcher.render(body, x, y, z, yaw, partialTick, poseStack, buffers,
                        dispatcher.getPackedLightCoords(body, partialTick));
            }
            buffers.endBatch();
        } finally {
            capturing = false;
            minecraft.options.hideGui = hideGui;
            dispatcher.setRenderShadow(minecraft.options.entityShadows().get());

            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.restoreProjectionMatrix();
            main.bindWrite(true);
        }
        return true;
    }

    private static List<Entity> freeBodies(Minecraft minecraft, ClientLevel level) {
        List<Entity> free = new ArrayList<>();
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Player) && !(entity instanceof StandEntity)) {
                continue;
            }
            if (firstPerson && entity == minecraft.getCameraEntity()) {
                continue;
            }
            if (entity.isInvisible() && !(entity instanceof StandEntity)) {
                continue;
            }
            if (TimeStopView.movesFreely(entity)) {
                free.add(entity);
            }
        }
        return free;
    }

    private static void ensure(int width, int height) {
        if (mask == null) {
            mask = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            mask.setFilterMode(GL30.GL_NEAREST);
        } else if (mask.width != width || mask.height != height) {
            mask.resize(width, height, Minecraft.ON_OSX);
        }
        if (buffers == null) {
            bytes = new ByteBufferBuilder(256 * 1024);
            SequencedMap<RenderType, ByteBufferBuilder> fixed =
                    new LinkedHashMap<>();
            for (RenderType glint : List.of(
                    RenderType.armorEntityGlint(),
                    RenderType.glintTranslucent(),
                    RenderType.glint(),
                    RenderType.entityGlint(),
                    RenderType.entityGlintDirect())) {
                fixed.put(glint, new ByteBufferBuilder(glint.bufferSize()));
            }
            buffers = MultiBufferSource.immediateWithBuffers(fixed, bytes);
        }
    }
}
