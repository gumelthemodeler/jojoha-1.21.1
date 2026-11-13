package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public final class ThornRope {
    public static final int GLOW_PASSES = 3;

    private static final double LINK = HermitGrappleModels.SEGMENT_HEIGHT / 16.0;

    private static final int SAMPLES = 48;

    private static final int SAG_STEPS = 16;
    private static final double SAG_MAX = 12.0;

    private static final double SLACK_EPSILON = 0.06;

    private static final int MAX_LINKS = 96;

    private static final int GLOW_BLOCK_LIGHT = 4;

    private static final Vec3[] PLACED_AT = new Vec3[MAX_LINKS];
    private static final Vec3[] PLACED_ALONG = new Vec3[MAX_LINKS];
    private static final int[] PLACED_LIGHT = new int[MAX_LINKS];

    private static final double REST_LIFT = 0.12;

    private static final Vec3[] CURVE = new Vec3[SAMPLES + 1];

    private static ModelPart segment;

    private ThornRope() {
    }

    public static void draw(PoseStack poseStack, MultiBufferSource buffers, Level level,
                            Vec3 end, Vec3 worldStart, double length) {
        draw(poseStack, buffers, level, end, worldStart, length, 0);
    }

    public static void draw(PoseStack poseStack, MultiBufferSource buffers, Level level,
                            Vec3 end, Vec3 worldStart, double length, int skin) {
        double span = end.length();
        if (span < 1.0E-4) {
            return;
        }

        if (model() == null) {
            return;
        }

        build(end, span, Math.max(span, length));
        rest(level, worldStart);
        walk(poseStack, buffers, level, worldStart, skin, LINK);
    }

    public static void coil(PoseStack poseStack, MultiBufferSource buffers, Level level, Vec3 centre,
                            double radius, double rise, double turns, Vec3 worldStart, int skin) {
        if (model() == null || radius < 1.0E-4 || turns < 0.05) {
            return;
        }

        double sweep = turns * 2 * Math.PI;
        for (int i = 0; i <= SAMPLES; i++) {
            double t = i / (double) SAMPLES;
            double angle = t * sweep;
            CURVE[i] = centre.add(Math.cos(angle) * radius, (t - 0.5) * rise, Math.sin(angle) * radius);
        }
        walk(poseStack, buffers, level, worldStart, skin, LINK * COIL_SPACING);
    }

    private static final double COIL_SPACING = 0.45;

    public static void path(PoseStack poseStack, MultiBufferSource buffers, Level level,
                            List<Vec3> points, Vec3 worldStart, int skin) {
        if (model() == null || points.size() < 2) {
            return;
        }
        double[] along = new double[points.size()];
        for (int i = 1; i < points.size(); i++) {
            along[i] = along[i - 1] + points.get(i).distanceTo(points.get(i - 1));
        }
        double total = along[along.length - 1];
        if (total < 1.0E-4) {
            return;
        }
        int seg = 1;
        for (int i = 0; i <= SAMPLES; i++) {
            double want = total * i / SAMPLES;
            while (seg < points.size() - 1 && along[seg] < want) {
                seg++;
            }
            double span = along[seg] - along[seg - 1];
            double t = span < 1.0E-9 ? 0.0 : Mth.clamp((want - along[seg - 1]) / span, 0.0, 1.0);
            CURVE[i] = points.get(seg - 1).lerp(points.get(seg), t);
        }
        walk(poseStack, buffers, level, worldStart, skin, LINK);
    }

    private static void walk(PoseStack poseStack, MultiBufferSource buffers, Level level,
                             Vec3 worldStart, int skin, double spacing) {
        ModelPart model = model();

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

                remaining = spacing;
                placed++;
            }
            remaining -= step - travelled;
        }

        pass(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(HermitSkins.rope(skin))),
                model, placed, false);

        VertexConsumer lit = buffers.getBuffer(
                RenderType.entityTranslucentEmissive(HermitSkins.ropeGlow(skin)));
        for (int i = 0; i < GLOW_PASSES; i++) {
            pass(poseStack, lit, model, placed, true);
        }
    }

    private static void pass(PoseStack poseStack, VertexConsumer consumer, ModelPart model,
                             int placed, boolean emissive) {
        for (int i = 0; i < placed; i++) {
            poseStack.pushPose();
            poseStack.translate(PLACED_AT[i].x, PLACED_AT[i].y, PLACED_AT[i].z);
            aim(poseStack, PLACED_ALONG[i]);
            model.render(poseStack, consumer,
                    emissive ? LightTexture.FULL_BRIGHT
                            : PLACED_LIGHT[i],
                    OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    private static void rest(Level level, Vec3 worldStart) {
        for (int i = 1; i < SAMPLES; i++) {
            Vec3 local = CURVE[i];
            Vec3 world = worldStart.add(local);

            BlockPos pos = BlockPos.containing(world);
            VoxelShape shape =
                    level.getBlockState(pos).getCollisionShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }

            double surface = pos.getY() + shape.max(Direction.Axis.Y) + REST_LIFT;
            if (world.y < surface) {
                CURVE[i] = new Vec3(local.x, local.y + (surface - world.y), local.z);
            }
        }
    }

    public static Vec3 firstChord() {
        return CURVE[1] == null ? new Vec3(0, 1, 0) : CURVE[1];
    }

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

    public static void aim(PoseStack poseStack, Vec3 direction) {
        double flat = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                (float) (Mth.atan2(direction.x, direction.z) * (180.0 / Math.PI))));
        poseStack.mulPose(Axis.XP.rotationDegrees(
                (float) (Mth.atan2(flat, direction.y) * (180.0 / Math.PI))));
    }

    public static int glow(int packed) {
        return LightTexture.pack(
                Math.max(LightTexture.block(packed), GLOW_BLOCK_LIGHT),
                LightTexture.sky(packed));
    }

    private static ModelPart model() {
        if (segment == null) {
            segment = Minecraft.getInstance().getEntityModels().bakeLayer(HermitGrappleModels.ROPE);
        }
        return segment;
    }
}
