package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;

/**
 * Hermit Purple's vine, drawn between two points.
 *
 * <p>Lifted out of the grapple renderer once the lasso needed the same thing. Two copies of a curve
 * solver and a segment walker would have been two places to fix the next time one of them was wrong,
 * and this particular code has already been wrong three separate ways - backwards segments, no
 * segments at all, and every segment at a sixteenth scale. Once was enough.
 *
 * <h2>What it does</h2>
 *
 * <ol>
 *   <li>Fits a curve between the ends whose <em>arc length is the vine's length</em>, by
 *       binary-searching the sag of a quadratic bezier. This is what makes slack read as slack: the
 *       vine always draws its whole length, so taut is straight and loose hangs, with neither being
 *       a special case.</li>
 *   <li>Walks that curve by cumulative arc length, dropping one whole segment every segment-length
 *       and pointing it along the local chord. Nothing is stretched, so the thorns stay thorns.</li>
 *   <li>Lights each segment where it hangs, so the vine darkens through shade.</li>
 * </ol>
 *
 * <p>Everything is in the pose's own space, with the start at the origin. Callers translate to
 * wherever the vine begins and hand over the far end relative to that.
 */
public final class ThornRope {

    /**
     * The parts of the vine that are lit, painted rather than calculated.
     *
     * <p>Same idea as the Stand's, and the same file naming, but drawn by hand here because the vine
     * is a plain model part and never goes near GeckoLib - so there is no layer system to hang it
     * on. What it buys is the same: only the pixels the artwork marks glow, so the thorns light up
     * and the stem between them does not.
     */

    /**
     * How many times the lit pass is laid down.
     *
     * <p>One pass is a translucent draw, so it arrives already faded against whatever is behind it -
     * fine at night, invisible at noon against a bright sky. Repeating it builds the alpha up toward
     * solid, which is what makes the thorns hold their own in daylight.
     *
     * <p>Three, because the return diminishes fast: the first pass does most of the work, the second
     * closes most of the remaining gap, and past the third there is nothing left to gain. It caps at
     * the mask's own colour either way - stacking cannot make a dark mask bright, it can only stop a
     * bright one being washed out.
     */
    public static final int GLOW_PASSES = 3;

    /**
     * How long one segment is, in blocks.
     *
     * <p>The only place the sixteenth belongs. {@code ModelPart} already divides by sixteen on every
     * cube vertex and again on the part offset, so scaling the pose as well draws the vine at a
     * sixteenth of its size - which it once did.
     */
    private static final double LINK = HermitGrappleModels.SEGMENT_HEIGHT / 16.0;

    /** How finely the curve is sampled before segments are walked along it. */
    private static final int SAMPLES = 48;

    /** How many halvings the sag search gets, and how deep it may go. */
    private static final int SAG_STEPS = 16;
    private static final double SAG_MAX = 12.0;

    /** Slack under this is not worth curving for - a vine this taut is a straight line. */
    private static final double SLACK_EPSILON = 0.06;

    /** A cap, so a vine at full stretch cannot spend hundreds of draws on one frame. */
    private static final int MAX_LINKS = 96;

    /**
     * The least block light a vine is ever drawn at.
     *
     * <p>Kept alongside the emissive pass rather than replaced by it, and much lower than it was.
     * They do different jobs: the mask lights the thorns, and this stops the stem between them going
     * completely black in a cave. Left at nine on top of a real glow the vine came out lit twice.
     */
    private static final int GLOW_BLOCK_LIGHT = 4;

    /** Placements from the last walk, so the glow pass can repeat it without walking again. */
    private static final Vec3[] PLACED_AT = new Vec3[MAX_LINKS];
    private static final Vec3[] PLACED_ALONG = new Vec3[MAX_LINKS];
    private static final int[] PLACED_LIGHT = new int[MAX_LINKS];

    /** How far above a surface a resting segment sits, so it lies on the floor rather than in it. */
    private static final double REST_LIFT = 0.12;

    private static final Vec3[] CURVE = new Vec3[SAMPLES + 1];

    private static ModelPart segment;

    private ThornRope() {
    }

    /**
     * Draws a vine from the pose origin to {@code end}.
     *
     * @param end        the far end, relative to the pose origin
     * @param worldStart where the pose origin is in the world, for lighting
     * @param length     how long the vine is; anything over the straight-line gap becomes sag
     */
    /**
     * The original vine, for a caller with no Stand to ask about.
     *
     * <p>Kept so the skin only has to be worked out where somebody actually knows the answer.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffers, Level level,
                            Vec3 end, Vec3 worldStart, double length) {
        draw(poseStack, buffers, level, end, worldStart, length, 0);
    }

    /** The same vine in a given Stand's colours - see HermitSkins. */
    public static void draw(PoseStack poseStack, MultiBufferSource buffers, Level level,
                            Vec3 end, Vec3 worldStart, double length, int skin) {
        double span = end.length();
        if (span < 1.0E-4) {
            return;
        }

        ModelPart model = model();
        if (model == null) {
            return;
        }

        build(end, span, Math.max(span, length));
        rest(level, worldStart);

        // Where every segment goes, worked out before anything is drawn.
        //
        // Separated from the drawing because there are two passes over the same segments and the
        // buffer source will not let them interleave: asking it for a second render type ends the
        // batch the first one is writing into, so a loop that switched buffers per segment would
        // hand the second half of the vine to a builder that had already been closed. Recording the
        // walk once and replaying it twice sidesteps that, and costs one array.
        double remaining = 0;
        int placed = 0;

        for (int i = 1; i <= SAMPLES && placed < MAX_LINKS; i++) {
            Vec3 from = CURVE[i - 1];
            Vec3 to = CURVE[i];

            double step = from.distanceTo(to);
            if (step < 1.0E-6) {
                continue;
            }
            Vec3 heading = to.subtract(from).scale(1.0 / step);

            double travelled = 0;
            while (travelled + remaining <= step + 1.0E-9 && placed < MAX_LINKS) {
                travelled += remaining;
                Vec3 at = from.add(heading.scale(travelled));

                PLACED_AT[placed] = at;
                PLACED_ALONG[placed] = heading;
                PLACED_LIGHT[placed] = glow(LevelRenderer.getLightColor(level,
                        BlockPos.containing(worldStart.add(at))));

                remaining = LINK;
                placed++;
            }
            remaining -= step - travelled;
        }

        // The vine as it is lit by the world.
        pass(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(HermitSkins.rope(skin))),
                model, placed, false);

        // And the thorns, at full brightness, wherever the mask says. Second because it goes on top
        // of what is already there, and repeated so it does not wash out in daylight - see
        // GLOW_PASSES. One buffer for all of them: it is the same render type every time, so
        // nothing is switching underneath the loop.
        VertexConsumer lit = buffers.getBuffer(
                RenderType.entityTranslucentEmissive(HermitSkins.ropeGlow(skin)));
        for (int i = 0; i < GLOW_PASSES; i++) {
            pass(poseStack, lit, model, placed, true);
        }
    }

    /** Draws every recorded segment once into one buffer. */
    private static void pass(PoseStack poseStack, VertexConsumer consumer, ModelPart model,
                             int placed, boolean emissive) {
        for (int i = 0; i < placed; i++) {
            poseStack.pushPose();
            poseStack.translate(PLACED_AT[i].x, PLACED_AT[i].y, PLACED_AT[i].z);
            aim(poseStack, PLACED_ALONG[i]);
            model.render(poseStack, consumer,
                    emissive ? net.minecraft.client.renderer.LightTexture.FULL_BRIGHT
                            : PLACED_LIGHT[i],
                    OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    /**
     * Lifts any part of the curve that has sagged into the ground back onto it.
     *
     * <p>The sag is worked out from length alone, which knows nothing about the world - so a vine
     * with slack in it hangs in a smooth curve straight through whatever floor happens to be under
     * it. Perfectly correct as a catenary, and it reads as the rope being a hologram.
     *
     * <p>Each sample is tested against the block it is inside, and pushed up to the top of that
     * block's own collision shape if it is below it. Using the shape rather than the block position
     * is what makes it settle correctly on slabs, stairs and paths instead of floating a full block
     * over them.
     *
     * <p>The ends are left alone. Those are attachment points - a hand and a mob, or a hand and a
     * hook - and moving either would detach the vine from the thing it is tied to, which is a worse
     * artefact than the one being fixed.
     *
     * <p>What this does not do is lengthen the curve to make up for the shortcut it just took. The
     * proper answer is to snake the resting part sideways until the arc length is right again, and
     * it matters for a chain with metres of slack pooling on the floor. Here the slack is measured
     * in tenths of a block, so the shortfall is not visible and the extra pass is not worth it.
     */
    private static void rest(Level level, Vec3 worldStart) {
        for (int i = 1; i < SAMPLES; i++) {
            Vec3 local = CURVE[i];
            Vec3 world = worldStart.add(local);

            BlockPos pos = BlockPos.containing(world);
            net.minecraft.world.phys.shapes.VoxelShape shape =
                    level.getBlockState(pos).getCollisionShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }

            double surface = pos.getY() + shape.max(net.minecraft.core.Direction.Axis.Y) + REST_LIFT;
            if (world.y < surface) {
                CURVE[i] = new Vec3(local.x, local.y + (surface - world.y), local.z);
            }
        }
    }

    /** The first chord of the last curve built, for anything that wants to face along the vine. */
    public static Vec3 firstChord() {
        return CURVE[1] == null ? new Vec3(0, 1, 0) : CURVE[1];
    }

    /**
     * Fits the vine's whole length between the two ends.
     *
     * <p>Binary search rather than algebra, because the arc length of a bezier has no closed form
     * worth having and sixteen halvings settle it to well under a pixel. The control point is
     * dropped straight down rather than displaced sideways, because gravity is the only thing
     * putting slack in a rope and it only pulls one way.
     */
    private static void build(Vec3 end, double span, double length) {
        if (length - span <= SLACK_EPSILON) {
            for (int i = 0; i <= SAMPLES; i++) {
                CURVE[i] = end.scale(i / (double) SAMPLES);
            }
            return;
        }

        double low = 0.0;
        double high = Math.min(SAG_MAX, (length - span) * 2.0 + 1.0);
        for (int step = 0; step < SAG_STEPS; step++) {
            double mid = (low + high) * 0.5;
            if (sample(end, mid) > length) {
                high = mid;
            } else {
                low = mid;
            }
        }
        sample(end, (low + high) * 0.5);
    }

    /** Writes the curve for one sag and returns how long it came out. */
    private static double sample(Vec3 end, double sag) {
        Vec3 control = end.scale(0.5).subtract(0, sag, 0);

        double length = 0;
        for (int i = 0; i <= SAMPLES; i++) {
            double t = i / (double) SAMPLES;
            CURVE[i] = control.scale(2 * (1 - t) * t).add(end.scale(t * t));
            if (i > 0) {
                length += CURVE[i].distanceTo(CURVE[i - 1]);
            }
        }
        return length;
    }

    /**
     * Points the model's own {@code +Y} along a direction.
     *
     * <p>Worked through rather than guessed at. The pose applies the last rotation pushed first, so
     * a yaw of {@code phi} followed by a pitch of {@code p} sends {@code +Y} to
     * {@code (sin p sin phi, cos p, sin p cos phi)} - which gives the pitch as
     * {@code atan2(flat, dy)} and the yaw as {@code atan2(dx, dz)}, both positive. An earlier
     * version negated one and took the pitch against {@code -dy}, and aimed four directions out of
     * five at their exact opposite.
     */
    public static void aim(PoseStack poseStack, Vec3 direction) {
        double flat = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                (float) (Mth.atan2(direction.x, direction.z) * (180.0 / Math.PI))));
        poseStack.mulPose(Axis.XP.rotationDegrees(
                (float) (Mth.atan2(flat, direction.y) * (180.0 / Math.PI))));
    }

    /**
     * Raises the block half of a packed light to the glow floor, leaving the sky half alone.
     *
     * <p>Public because the hook wants the same treatment, and the two should not drift apart.
     */
    public static int glow(int packed) {
        return net.minecraft.client.renderer.LightTexture.pack(
                Math.max(net.minecraft.client.renderer.LightTexture.block(packed), GLOW_BLOCK_LIGHT),
                net.minecraft.client.renderer.LightTexture.sky(packed));
    }

    /** Baked on first use - there is no renderer context to hand it in from a mixin. */
    private static ModelPart model() {
        if (segment == null) {
            segment = Minecraft.getInstance().getEntityModels().bakeLayer(HermitGrappleModels.ROPE);
        }
        return segment;
    }
}
