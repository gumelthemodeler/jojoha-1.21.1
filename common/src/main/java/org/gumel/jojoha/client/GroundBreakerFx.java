package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.network.packet.GroundBreakerFxPacket;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class GroundBreakerFx {
    private static final float DRAW_IN_TICKS = 7F;

    private static final float RING_WIDTH = 0.22F;
    private static final float RING_LIFT = 0.04F;

    private static final int RING_SEGMENTS = 128;

    private static final float[] RING_COLOUR = {1.0F, 0.84F, 0.2F};

    private static final float RING_ALPHA = 0.7F;
    private static final float RING_PULSE = 0.2F;
    private static final float PULSE_SPEED = 0.45F;

    private static final float FILL_ALPHA = 0.10F;

    private static final double SWELL_WIDTH = 3.2;

    private static final double RISE_PART = 0.4;

    private static final double LIFT_NEAR = 0.65;
    private static final double LIFT_FAR = 0.38;

    private static final float TILT_DEGREES = 10F;

    private static final double MIN_VISIBLE_LIFT = 0.03;

    private static final float INSET = 0.994F;

    private static final int SURFACE_SEARCH_UP = 2;
    private static final int SURFACE_SEARCH_DOWN = 4;

    private static final List<Mark> ACTIVE = new ArrayList<>();

    private GroundBreakerFx() {
    }

    public static void begin(int throwerId, double x, double y, double z, float radius, int phase,
                             int ticks) {
        Vec3 centre = new Vec3(x, y, z);

        if (phase == GroundBreakerFxPacket.SLAM) {
            Gore.quake(centre, radius, ticks);
            for (Mark mark : ACTIVE) {
                if (mark.throwerId == throwerId && mark.waveAge < 0) {
                    mark.slam(centre, radius, ticks);
                    return;
                }
            }
            Mark fresh = new Mark(throwerId, centre, radius, 0);
            fresh.slam(centre, radius, ticks);
            ACTIVE.add(fresh);
            return;
        }

        ACTIVE.removeIf(mark -> mark.throwerId == throwerId);
        ACTIVE.add(new Mark(throwerId, centre, radius, ticks));
    }

    public static void tick() {
        Iterator<Mark> it = ACTIVE.iterator();
        while (it.hasNext()) {
            if (it.next().advance()) {
                it.remove();
            }
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                              float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (ACTIVE.isEmpty() || level == null) {
            return;
        }

        Vec3 cam = camera.getPosition();
        for (Mark mark : ACTIVE) {
            mark.surveyIfNeeded(level);
            drawOutline(poseStack, buffers, cam, mark, partialTick);
            if (mark.waveAge >= 0) {
                drawSwell(poseStack, buffers, cam, level, minecraft, mark, partialTick);
            }
        }

        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
    }

    private static void drawOutline(PoseStack poseStack, MultiBufferSource buffers, Vec3 cam,
                                    Mark mark, float partialTick) {
        float age = mark.age + partialTick;
        float alpha = mark.outlineAlpha(partialTick);
        if (!mark.ringed || alpha <= 0.01F) {
            return;
        }

        float drawn = Mth.clamp(age / DRAW_IN_TICKS, 0F, 1F);
        int segments = Math.max(1, Math.round(RING_SEGMENTS * drawn));

        poseStack.pushPose();
        poseStack.translate(mark.centre.x - cam.x, 0 - cam.y, mark.centre.z - cam.z);
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer ribbon = buffers.getBuffer(RenderType.lightning());

        double inner = mark.radius - RING_WIDTH * 0.5;
        double outer = mark.radius + RING_WIDTH * 0.5;
        float r = RING_COLOUR[0], g = RING_COLOUR[1], b = RING_COLOUR[2];

        for (int i = 0; i < segments; i++) {
            double a0 = i / (double) RING_SEGMENTS * Math.PI * 2;
            double a1 = (i + 1) / (double) RING_SEGMENTS * Math.PI * 2;
            float y0 = (float) mark.surfaceAt(a0, mark.radius) + RING_LIFT;
            float y1 = (float) mark.surfaceAt(a1, mark.radius) + RING_LIFT;

            quad(ribbon, pose,
                    (float) (Math.cos(a0) * inner), y0, (float) (Math.sin(a0) * inner),
                    (float) (Math.cos(a0) * outer), y0, (float) (Math.sin(a0) * outer),
                    (float) (Math.cos(a1) * outer), y1, (float) (Math.sin(a1) * outer),
                    (float) (Math.cos(a1) * inner), y1, (float) (Math.sin(a1) * inner),
                    r, g, b, alpha);

            float cy = (float) mark.surfaceAt(0, 0) + RING_LIFT * 0.5F;
            float fill = alpha * FILL_ALPHA;
            quad(ribbon, pose,
                    0F, cy, 0F,
                    (float) (Math.cos(a0) * inner), y0, (float) (Math.sin(a0) * inner),
                    (float) (Math.cos(a1) * inner), y1, (float) (Math.sin(a1) * inner),
                    0F, cy, 0F,
                    r, g, b, fill);
        }

        poseStack.popPose();
    }

    private static void quad(VertexConsumer buffer, Matrix4f pose,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float r, float g, float b, float a) {
        buffer.addVertex(pose, ax, ay, az).setColor(r, g, b, a);
        buffer.addVertex(pose, bx, by, bz).setColor(r, g, b, a);
        buffer.addVertex(pose, cx, cy, cz).setColor(r, g, b, a);
        buffer.addVertex(pose, dx, dy, dz).setColor(r, g, b, a);
        buffer.addVertex(pose, dx, dy, dz).setColor(r, g, b, a);
        buffer.addVertex(pose, cx, cy, cz).setColor(r, g, b, a);
        buffer.addVertex(pose, bx, by, bz).setColor(r, g, b, a);
        buffer.addVertex(pose, ax, ay, az).setColor(r, g, b, a);
    }

    private static void drawSwell(PoseStack poseStack, MultiBufferSource buffers, Vec3 cam,
                                  ClientLevel level, Minecraft minecraft, Mark mark,
                                  float partialTick) {
        double front = mark.frontAt(partialTick);

        for (Column column : mark.columns) {
            double lift = liftAt(front, column.distance, mark.radius);
            if (lift < MIN_VISIBLE_LIFT) {
                continue;
            }

            BlockState state = column.state;
            BakedModel model = minecraft.getBlockRenderer().getBlockModel(state);

            int tint = minecraft.getBlockColors().getColor(state, level, column.pos, 0);
            float tr = tint == -1 ? 1F : ((tint >> 16) & 0xFF) / 255F;
            float tg = tint == -1 ? 1F : ((tint >> 8) & 0xFF) / 255F;
            float tb = tint == -1 ? 1F : (tint & 0xFF) / 255F;

            int light = LevelRenderer.getLightColor(level, column.pos.above());

            float peak = (float) (lift / liftFor(column.distance, mark.radius));
            float tilt = TILT_DEGREES * Mth.clamp(peak, 0F, 1F);

            poseStack.pushPose();
            poseStack.translate(column.pos.getX() - cam.x, column.pos.getY() - cam.y + lift,
                    column.pos.getZ() - cam.z);
            poseStack.translate(0.5F, 0F, 0.5F);
            if (column.distance > 0.3) {
                poseStack.mulPose(Axis.of(new Vector3f(column.tiltAxisX, 0F, column.tiltAxisZ))
                        .rotationDegrees(tilt));
            }
            poseStack.scale(INSET, 1F, INSET);
            poseStack.translate(-0.5F, 0F, -0.5F);

            minecraft.getBlockRenderer().getModelRenderer().renderModel(poseStack.last(),
                    buffers.getBuffer(ItemBlockRenderTypes.getRenderType(state, false)),
                    state, model, tr, tg, tb, light, OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
        }
    }

    static double liftAt(double front, double distance, double radius) {
        double u = (front - distance) / SWELL_WIDTH;
        if (u <= 0.0 || u >= 1.0) {
            return 0.0;
        }

        double shape = u < RISE_PART
                ? 0.5 * (1.0 - Math.cos((u / RISE_PART) * Math.PI))
                : 0.5 * (1.0 + Math.cos((u - RISE_PART) / (1.0 - RISE_PART) * Math.PI));
        return liftFor(distance, radius) * shape;
    }

    private static double liftFor(double distance, double radius) {
        double t = Mth.clamp(distance / Math.max(radius, 1.0E-3), 0.0, 1.0);
        return Mth.lerp(t, LIFT_NEAR, LIFT_FAR);
    }

    static double waveSettleTicks(int waveTicks, double radius) {
        return waveTicks * (radius + SWELL_WIDTH) / Math.max(radius, 1.0E-3);
    }

    private record Column(BlockPos pos, BlockState state, double distance, float tiltAxisX,
                          float tiltAxisZ) {
    }

    private static final class Mark {
        private final int throwerId;
        private Vec3 centre;
        private double radius;

        private int age;
        private final int telegraphTicks;
        private final boolean ringed;

        private int waveAge = -1;
        private int waveTicks;

        private List<Column> columns;
        private final Map<Long, Double> surfaces = new HashMap<>();

        private Mark(int throwerId, Vec3 centre, double radius, int telegraphTicks) {
            this.throwerId = throwerId;
            this.centre = centre;
            this.radius = radius;
            this.telegraphTicks = telegraphTicks;
            this.ringed = telegraphTicks > 0;
        }

        private void slam(Vec3 at, double wave, int ticks) {
            if (at.distanceToSqr(centre) > 0.01 || wave != radius) {
                columns = null;
                surfaces.clear();
            }
            this.centre = at;
            this.radius = wave;
            this.waveAge = 0;
            this.waveTicks = Math.max(1, ticks);
        }

        private boolean advance() {
            age++;
            if (waveAge >= 0) {
                waveAge++;
                return waveAge > waveSettleTicks(waveTicks, radius) + 2;
            }
            return age > telegraphTicks + 10;
        }

        private double frontAt(float partialTick) {
            return radius * ((waveAge + partialTick) / (double) waveTicks);
        }

        private float outlineAlpha(float partialTick) {
            float breath = RING_ALPHA + RING_PULSE * Mth.sin((age + partialTick) * PULSE_SPEED);
            if (waveAge < 0) {
                return breath;
            }
            float t = (waveAge + partialTick) / (float) waveTicks;
            return Mth.clamp(1.0F - t, 0F, 1F) * Math.min(1F, breath + 0.3F);
        }

        private void surveyIfNeeded(ClientLevel level) {
            if (columns != null) {
                return;
            }
            columns = new ArrayList<>();

            int reach = Mth.ceil(radius) + 1;
            int baseX = Mth.floor(centre.x);
            int baseZ = Mth.floor(centre.z);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    double cx = baseX + dx + 0.5 - centre.x;
                    double cz = baseZ + dz + 0.5 - centre.z;
                    double distance = Math.sqrt(cx * cx + cz * cz);
                    if (distance > radius + 0.5) {
                        continue;
                    }

                    BlockPos top = surface(level, baseX + dx, baseZ + dz);
                    if (top == null) {
                        continue;
                    }
                    BlockState state = level.getBlockState(top);
                    if (state.getRenderShape() != RenderShape.MODEL) {
                        continue;
                    }

                    float ax = distance > 1.0E-3 ? (float) (-cz / distance) : 0F;
                    float az = distance > 1.0E-3 ? (float) (cx / distance) : 0F;
                    columns.add(new Column(top, state, distance, -ax, -az));
                }
            }
        }

        private double surfaceAt(double angle, double distance) {
            int x = Mth.floor(centre.x + Math.cos(angle) * distance);
            int z = Mth.floor(centre.z + Math.sin(angle) * distance);
            long key = BlockPos.asLong(x, 0, z);

            Double cached = surfaces.get(key);
            if (cached != null) {
                return cached;
            }
            ClientLevel level = Minecraft.getInstance().level;
            BlockPos top = level == null ? null : surface(level, x, z);
            double y = top == null ? centre.y : top.getY() + 1.0;
            surfaces.put(key, y);
            return y;
        }

        private BlockPos surface(ClientLevel level, int x, int z) {
            int y0 = Mth.floor(centre.y - 0.1);
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int y = y0 + SURFACE_SEARCH_UP; y >= y0 - SURFACE_SEARCH_DOWN; y--) {
                at.set(x, y, z);
                BlockState state = level.getBlockState(at);
                if (!state.isCollisionShapeFullBlock(level, at)) {
                    continue;
                }
                BlockPos above = at.above();
                if (!level.getBlockState(above).isCollisionShapeFullBlock(level, above)) {
                    return at.immutable();
                }
            }
            return null;
        }
    }
}
