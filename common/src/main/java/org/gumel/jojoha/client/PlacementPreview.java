package org.gumel.jojoha.client;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandPlacement;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandMovementProfile;
import org.gumel.jojoha.stand.StandType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class PlacementPreview {
    private static final float YAW = -125F;
    private static final float PITCH = 24F;
    private static final float PITCH_LOW = 4F;
    private static final float PITCH_HIGH = 80F;
    private static final float TURN_PER_PIXEL = 0.9F;
    private static final float ZOOM_LOW = 0.6F;
    private static final float ZOOM_HIGH = 2F;
    private static final float ZOOM_STEP = 1.12F;
    private static final float FLOOR_DOWN = 0.7F;
    private static final float DEPTH = 200F;

    private static final int GRID = 5;
    private static final float LINE = 0.018F;
    private static final float GRID_ALPHA = 0.55F;
    private static final float AXIS_ALPHA = 0.85F;
    private static final int BACKGROUND = 0xFF000000;

    private static float yaw = YAW;
    private static float pitch = PITCH;
    private static float zoom = 1F;
    private static boolean turning;
    private static boolean spinning;

    private PlacementPreview() {
    }

    public static void grab(boolean spin) {
        turning = true;
        spinning = spin;
    }

    public static boolean spinning() {
        return turning && spinning;
    }

    public static float spin(float turn, double dx) {
        return Mth.wrapDegrees(turn - (float) dx * TURN_PER_PIXEL);
    }

    public static boolean turning() {
        return turning;
    }

    public static void drop() {
        turning = false;
        spinning = false;
    }

    public static void turn(double dx, double dy) {
        yaw = Mth.wrapDegrees(yaw + (float) dx * TURN_PER_PIXEL);
        pitch = Mth.clamp(pitch + (float) dy * TURN_PER_PIXEL, PITCH_LOW, PITCH_HIGH);
    }

    public static void zoom(double scroll) {
        zoom = Mth.clamp(zoom * (float) Math.pow(ZOOM_STEP, scroll), ZOOM_LOW, ZOOM_HIGH);
    }

    public static void reset() {
        yaw = YAW;
        pitch = PITCH;
        zoom = 1F;
    }

    public static void render(GuiGraphics guiGraphics, JojohaPlayerData data, StandType type, StandPlacement placed,
                              int x, int y, int width, int height, float pixelsPerBlock) {
        render(guiGraphics, data, type, placed, data.stand.skin(), x, y, width, height, pixelsPerBlock);
    }

    public static void render(GuiGraphics guiGraphics, JojohaPlayerData data, StandType type, StandPlacement placed,
                              int skin, int x, int y, int width, int height, float pixelsPerBlock) {
        guiGraphics.fill(x, y, x + width, y + height, BACKGROUND);
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }

        guiGraphics.flush();
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        float size = pixelsPerBlock * zoom;
        Quaternionf view = new Quaternionf()
                .rotateZ((float) Math.PI)
                .rotateX(-pitch * Mth.DEG_TO_RAD)
                .rotateY(yaw * Mth.DEG_TO_RAD);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(x + width / 2F, y + height * FLOOR_DOWN, DEPTH);
        pose.mulPose(new Matrix4f().scaling(size, size, -size));
        pose.mulPose(view);

        floor(guiGraphics, pose.last().pose());
        guiGraphics.flush();

        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        dispatcher.overrideCameraOrientation(new Quaternionf()
                .rotateX(-pitch * Mth.DEG_TO_RAD)
                .rotateY(yaw * Mth.DEG_TO_RAD)
                .conjugate()
                .rotateY((float) Math.PI));
        dispatcher.setRenderShadow(false);

        float[] heldPlayer = StandPortrait.hold(player, 0F);
        RenderSystem.runAsFancy(() -> dispatcher.render(player, 0.0, 0.0, 0.0, 0F, 1F, pose,
                guiGraphics.bufferSource(), LightTexture.FULL_BRIGHT));
        StandPortrait.release(player, heldPlayer);

        StandEntity stand = type.form().hasBody() ? StandPortrait.prepare(data, skin) : null;
        if (stand != null) {
            boolean bound = type.form().isBound();
            StandMovementProfile profile = StandMovementProfile.forStand(type);
            double across = bound ? 0.0 : profile.sideOffset() + placed.side();
            double forward = bound ? 0.0 : placed.ahead() - profile.followDistance();
            double up = bound ? 0.0 : profile.verticalOffset() + placed.height();
            float[] heldStand = StandPortrait.hold(stand, bound ? 0F : placed.turn());
            RenderSystem.runAsFancy(() -> dispatcher.render(stand, -across, up, forward, 0F, 1F, pose,
                    guiGraphics.bufferSource(), LightTexture.FULL_BRIGHT));
            StandPortrait.release(stand, heldStand);
        }

        guiGraphics.flush();
        dispatcher.setRenderShadow(true);
        pose.popPose();
        Lighting.setupFor3DItems();
        RenderSystem.clear(GlConst.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    private static void floor(GuiGraphics guiGraphics, Matrix4f pose) {
        VertexConsumer out = guiGraphics.bufferSource().getBuffer(RenderType.gui());
        for (int i = -GRID; i <= GRID; i++) {
            float alpha = i == 0 ? AXIS_ALPHA : GRID_ALPHA;
            for (int step = -GRID; step < GRID; step++) {
                float near = fade(step, alpha);
                float far = fade(step + 1, alpha);
                strip(out, pose, i - LINE, step, i + LINE, step + 1, near, far, true);
                strip(out, pose, step, i - LINE, step + 1, i + LINE, near, far, false);
            }
        }
    }

    private static float fade(int at, float alpha) {
        float edge = 1F - Math.abs(at) / (float) GRID;
        return alpha * edge * edge;
    }

    private static void strip(VertexConsumer out, Matrix4f pose, float x0, float z0, float x1, float z1,
                              float from, float to, boolean alongZ) {
        float a00 = from, a01 = alongZ ? to : from, a11 = to, a10 = alongZ ? from : to;
        corner(out, pose, x0, z0, a00);
        corner(out, pose, x0, z1, a01);
        corner(out, pose, x1, z1, a11);
        corner(out, pose, x1, z0, a10);
        corner(out, pose, x1, z0, a10);
        corner(out, pose, x1, z1, a11);
        corner(out, pose, x0, z1, a01);
        corner(out, pose, x0, z0, a00);
    }

    private static void corner(VertexConsumer out, Matrix4f pose, float x, float z, float alpha) {
        out.addVertex(pose, x, 0F, z).setColor(1F, 1F, 1F, alpha);
    }
}
