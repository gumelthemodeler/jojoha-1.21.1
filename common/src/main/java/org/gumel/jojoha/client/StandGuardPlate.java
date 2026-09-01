package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.stand.StandEntity;
import org.joml.Matrix4f;

/**
 * The guard as a pane: one flat sheet standing in front of the Stand, cracking as it gives way.
 *
 * <p>A single quad, not a coat of paint on the model. The block-breaking overlay is the reference
 * and it is the right one - what a player reads there is a flat surface with damage on it, and the
 * damage means <em>this is about to go</em>. Wrapping the same art round the Stand's own geometry
 * says something different and worse: that the Stand is the thing cracking.
 *
 * <p>Held off until the guard is actually in trouble. A pane that appeared the moment you raised
 * the stance would be a permanent sheet of glass hanging off the player, and the whole point of the
 * cue is that its arrival means something. It fades in from {@link #SHOW_FROM} and is at full
 * strength as the break lands.
 *
 * <p>Built in world axes rather than in model space, which is why it is drawn from the renderer
 * rather than as a {@code GeoRenderLayer}. A layer receives the pose with the entity's rotation and
 * GeckoLib's own model transform already applied, so placing anything by hand inside one means
 * knowing which way that space happens to point. Called after {@code super.render} the pose is back
 * at the entity's origin in world orientation, and the quad's facing is simply the Stand's yaw -
 * which is already the direction the guard is turned, so the pane faces the threat for free.
 */
public final class StandGuardPlate {
    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/particle/" + name);
    }

    /** The fracture running through, and what it leaves. */
    private static final ResourceLocation CRACKING = texture("break.png");
    private static final ResourceLocation SHATTERED = texture("broken.png");

    /** How far in front of the Stand the pane stands, in blocks. */
    private static final float OFFSET = 0.5F;

    /** Where it is centred up the Stand, and how big it is - roughly what a body covers. */
    private static final float CENTRE_HEIGHT = 1.05F;
    private static final float HALF_WIDTH = 0.8F;
    private static final float HALF_HEIGHT = 0.95F;

    /**
     * How far into the guard's strain the pane starts showing, 0 to 1.
     *
     * <p>Past halfway, so it reads as a warning rather than as scenery. Everything before this is
     * the guard doing its job quietly.
     */
    private static final float SHOW_FROM = 0.45F;

    /** How solid it gets at its worst. Kept well short of opaque - it is a pane, not a wall. */
    private static final float MAX_ALPHA = 0.72F;

    /** How hard it flickers once it is close to going, and how fast. */
    private static final float PULSE_DEPTH = 0.22F;
    private static final float PULSE_SPEED = 0.9F;

    /** Cool for the cracks, hot for the break, so the two are told apart at a glance. */
    private static final float[] CRACK_TINT = {0.78F, 0.90F, 1.0F};
    private static final float[] BREAK_TINT = {1.0F, 0.96F, 0.86F};

    private StandGuardPlate() {
    }

    /**
     * Draws the pane, if there is one to draw.
     *
     * <p>Called with the pose at the Stand's origin and no rotation applied - see the class note.
     */
    public static void render(StandEntity stand, float partialTick, PoseStack poseStack,
                              MultiBufferSource bufferSource) {
        float viewAlpha = StandViewAlpha.of(stand);
        if (viewAlpha <= 0.01F) {
            return;
        }

        int brokenTicks = stand.guardBrokenTicks();
        if (brokenTicks > 0) {
            // Checked before the stance, because by the time this runs the guard is already down
            // and the last thing it did is what should be on screen.
            float remaining = brokenTicks / (float) StandEntity.guardBrokenFlashTicks();
            draw(stand, partialTick, poseStack, bufferSource, SHATTERED, BREAK_TINT,
                    remaining * viewAlpha);
            return;
        }

        if (!stand.isGuarding()) {
            return;
        }

        float strain = stand.guardStrain();
        if (strain < SHOW_FROM) {
            return;
        }

        // Fades in across whatever is left of the strain, so the pane arrives rather than appears.
        float into = (strain - SHOW_FROM) / (1F - SHOW_FROM);
        float wave = Mth.sin((stand.tickCount + partialTick) * PULSE_SPEED);
        float alpha = MAX_ALPHA * into * (1F + wave * PULSE_DEPTH * into);

        draw(stand, partialTick, poseStack, bufferSource, CRACKING, CRACK_TINT,
                Mth.clamp(alpha, 0F, 1F) * viewAlpha);
    }

    /**
     * One quad, square on to the way the Stand is facing.
     *
     * <p>Translucent rather than additive on purpose. Additive would make the cracks glow, which
     * turns them into an energy effect; the block overlay this is modelled on is a surface being
     * damaged, and plain blending is what keeps it reading that way. {@code entityTranslucent} is
     * also the no-cull variant, so the pane exists from both sides - a guard that vanished when you
     * looked at it from behind would be a strange kind of barrier.
     */
    private static void draw(StandEntity stand, float partialTick, PoseStack poseStack,
                             MultiBufferSource bufferSource, ResourceLocation sheet,
                             float[] tint, float alpha) {
        // Interpolated, or the pane snaps between tick positions while the Stand it belongs to
        // moves smoothly - which reads as the two being separate objects.
        float yaw = Mth.rotLerp(partialTick, stand.yRotO, stand.getYRot());
        double radians = Math.toRadians(yaw);

        // Minecraft's yaw zero points along +Z, so this is the Stand's own facing - which the guard
        // has already been turned onto. The pane inherits that aim without asking for it.
        Vec3 forward = new Vec3(-Math.sin(radians), 0, Math.cos(radians));
        Vec3 right = new Vec3(forward.z, 0, -forward.x);

        Vec3 centre = forward.scale(OFFSET).add(0, CENTRE_HEIGHT, 0);
        Vec3 across = right.scale(HALF_WIDTH);

        RenderType type = RenderType.entityTranslucent(sheet);
        VertexConsumer buffer = bufferSource.getBuffer(type);
        Matrix4f pose = poseStack.last().pose();

        // Wound so the front face points back along the Stand's facing, at whatever is being
        // guarded against. No-cull means the winding only decides which side the normal lights.
        corner(buffer, pose, centre, across, -1, +1, 0F, 0F, tint, alpha, forward);
        corner(buffer, pose, centre, across, -1, -1, 0F, 1F, tint, alpha, forward);
        corner(buffer, pose, centre, across, +1, -1, 1F, 1F, tint, alpha, forward);
        corner(buffer, pose, centre, across, +1, +1, 1F, 0F, tint, alpha, forward);
    }

    private static void corner(VertexConsumer buffer, Matrix4f pose, Vec3 centre, Vec3 across,
                               int side, int vertical, float u, float v, float[] tint, float alpha,
                               Vec3 normal) {
        double x = centre.x + across.x * side;
        double y = centre.y + HALF_HEIGHT * vertical;
        double z = centre.z + across.z * side;

        buffer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(tint[0], tint[1], tint[2], alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                // Fullbright: a guard about to break is exactly the thing you cannot afford to miss
                // because the fight happens to be in a cave.
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal((float) normal.x, (float) normal.y, (float) normal.z);
    }
}
