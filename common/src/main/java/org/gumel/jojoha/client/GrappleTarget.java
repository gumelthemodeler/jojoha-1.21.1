package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.StandTypes;

/**
 * The mark that says the vine would reach.
 *
 * <p>Aiming a grapple by eye is a guess about two things at once - whether there is anything solid
 * where you are looking, and whether it is inside the vine's reach - and getting either wrong costs
 * you the throw and the fall afterwards. Neither is a judgement the player can make from a
 * crosshair, so the game answers both before they commit.
 *
 * <h2>Why it traces rather than searching</h2>
 *
 * <p>An obvious targeting system scans for grappleable blocks nearby and marks all of them. That is
 * a worse answer: it fills the screen with marks in any built-up area, and none of them tell you
 * about the one place you are actually pointing. The vine goes exactly where it is thrown, so the
 * honest mark is on the block that particular throw would hit - one trace, one mark, and it moves
 * with the crosshair.
 *
 * <p>The trace is the same length as the hook's own range, so the mark appearing and the throw
 * connecting are the same question. It cannot promise something the vine will not do.
 */
public final class GrappleTarget {
    private static final ResourceLocation MARK =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                    "textures/particle/hermit_purple_block_target.png");

    /** How big the mark is drawn, and how far off the face it floats so it never z-fights. */
    private static final float SIZE = 0.85F;
    private static final double LIFT = 0.04;

    /** The breathing pulse, so the mark reads as live rather than as part of the world. */
    private static final float PULSE_RATE = 3.1F;
    private static final float ALPHA_MIN = 0.55F;
    private static final float ALPHA_MAX = 1.0F;

    /** Where the throw would land, or null if it would hit nothing in reach. */
    private static BlockHitResult aim;

    private GrappleTarget() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;

        aim = null;
        if (player == null || level == null || !holdsHermitPurple(player)) {
            return;
        }

        // Nothing to aim at while already hanging from one - the decision has been made.
        if (GrappleController.active() != null) {
            return;
        }

        aim = org.gumel.jojoha.stand.grapple.GrappleAim.find(player);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                              float partialTick) {
        BlockHitResult at = aim;
        if (at == null) {
            return;
        }

        Vec3 cameraPos = camera.getPosition();

        // Lifted along the face that was struck rather than a fixed axis, so a mark on a ceiling or
        // a wall stands off it the same way one on the floor does.
        Vec3 normal = Vec3.atLowerCornerOf(at.getDirection().getNormal());
        Vec3 spot = at.getLocation().add(normal.scale(LIFT)).subtract(cameraPos);

        float seconds = (System.currentTimeMillis() % 100000L) / 1000F;
        float alpha = Mth.lerp((Mth.sin(seconds * PULSE_RATE) + 1F) * 0.5F, ALPHA_MIN, ALPHA_MAX);

        poseStack.pushPose();
        poseStack.translate(spot.x, spot.y, spot.z);

        // Turned to the camera rather than laid on the block. A mark flat on the face is invisible
        // at a glancing angle, which is exactly the angle you throw a grapple at.
        poseStack.mulPose(camera.rotation());
        poseStack.scale(SIZE, SIZE, SIZE);

        VertexConsumer buffer = buffers.getBuffer(RenderType.textSeeThrough(MARK));
        org.joml.Matrix4f pose = poseStack.last().pose();
        quad(buffer, pose, -0.5F, -0.5F, 0F, 1F, alpha);
        quad(buffer, pose, 0.5F, -0.5F, 1F, 1F, alpha);
        quad(buffer, pose, 0.5F, 0.5F, 1F, 0F, alpha);
        quad(buffer, pose, -0.5F, 0.5F, 0F, 0F, alpha);

        poseStack.popPose();

        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
    }

    private static void quad(VertexConsumer buffer, org.joml.Matrix4f pose,
                             float x, float y, float u, float v, float alpha) {
        buffer.addVertex(pose, x, y, 0F)
                .setColor(1F, 1F, 1F, alpha)
                .setUv(u, v)
                .setLight(net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
    }

    /**
     * Whether the vine is actually out.
     *
     * <p>Having Hermit Purple is not the same as having it summoned, and the first version tested
     * only the former - so the mark stayed painted on the world with the Stand put away, offering
     * a throw that nothing could make. {@code standSummoned} is the flag that answers the question
     * being asked.
     */
    private static boolean holdsHermitPurple(LocalPlayer player) {
        var data = org.gumel.jojoha.data.ClientPlayerDataCache.data;
        return data != null && data.standSummoned && data.stand.isPresent()
                && StandTypes.HERMIT_PURPLE_ID.equals(data.stand.standId());
    }
}
