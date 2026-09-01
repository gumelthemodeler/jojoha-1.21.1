package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the pilot's own body while they are away flying their Stand.
 *
 * <h2>Why this has to exist</h2>
 *
 * <p>Vanilla will not draw it. The entity loop in {@code LevelRenderer.renderLevel} ends with a
 * test that reads, in effect, "if this is the local player, only draw it when it is the camera
 * entity" - so the moment the camera is moved onto anything else, the player's body stops being
 * rendered anywhere. Nothing else is affected, which is why a piloting user could still see their
 * Stand's particles hanging in the air at the spot they were standing, with nobody standing there.
 *
 * <p>NeoForge patches that test to allow exactly this case; Fabric does not. Rather than depend on
 * which loader is underneath, the body is drawn here and a guard keeps it from being drawn twice -
 * see {@link #markDrawnByLevel()}.
 */
public final class PilotBody {
    /**
     * Set when the level renderer drew the local player itself this frame.
     *
     * <p>Read and cleared once per frame by {@link #render}. On a loader that patches the test
     * above, this is set every frame and nothing here draws anything; on one that does not, it is
     * never set and this class does the work. No loader check, no platform split - the answer is
     * observed rather than assumed, so it stays right if either side changes.
     */
    private static boolean drawnByLevel;

    private PilotBody() {
    }

    public static void markDrawnByLevel() {
        drawnByLevel = true;
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                              float partialTick) {
        boolean already = drawnByLevel;
        drawnByLevel = false;

        if (already || !StandSkillInput.isPiloting()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.getCameraEntity() == player || player.isSpectator()) {
            return;
        }

        // Interpolated the way the level renderer interpolates every other entity, and offset
        // against the camera rather than the world origin, because that is the space the pose stack
        // is already in by the time this runs.
        Vec3 eye = camera.getPosition();
        double x = Mth.lerp(partialTick, player.xo, player.getX()) - eye.x;
        double y = Mth.lerp(partialTick, player.yo, player.getY()) - eye.y;
        double z = Mth.lerp(partialTick, player.zo, player.getZ()) - eye.z;
        float yaw = Mth.rotLerp(partialTick, player.yRotO, player.getYRot());

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();

        poseStack.pushPose();
        dispatcher.render(player, x, y, z, yaw, partialTick, poseStack, buffers,
                dispatcher.getPackedLightCoords(player, partialTick));
        poseStack.popPose();
    }
}
