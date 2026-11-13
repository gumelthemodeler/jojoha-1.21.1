package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.pillar.PixelDraw;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class FlameFx {
    public static final int WHITE = 0;
    public static final int YELLOW = 1;
    public static final int ORANGE = 2;
    public static final int RED = 3;

    public static final ResourceLocation SHEET =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/magician_flame.png");
    public static final ResourceLocation BLAZE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/magician_blaze.png");

    private static final int FRAME_COUNT = 8;
    private static final int PINNED_FRAMES = 6;
    private static final int STAGE_COUNT = 4;
    private static final float CELL_U = 1F / FRAME_COUNT;
    private static final float CELL_V = 1F / 5F;
    private static final float SWATCH_V = 4.5F * CELL_V;
    private static final float PIXEL = 1F / 16F;
    private static final int MAX_MOTES = 3600;
    private static final int MAX_SPARKS = 1400;
    private static final double RANGE = 96.0;
    private static final double TAPER = 0.94;
    private static final double SPARK_DRAG = 0.92;
    private static final double SPARK_LIFT = 0.012;
    private static final int UNTAGGED = 0;
    private static final int BODY = 0xD0FFFFFF;
    private static final int BODY_DYING = 0x90FFFFFF;
    private static final float GLOW = 0.85F;
    private static final float GLOW_DYING = 0.55F;

    private static final class Mote {
        double x;
        double y;
        double z;
        double xo;
        double yo;
        double zo;
        double vx;
        double vy;
        double vz;
        float half;
        int age;
        int life;
        int tag;
        int fixed = -1;
    }

    private static final class Spark {
        double x;
        double y;
        double z;
        double xo;
        double yo;
        double zo;
        double vx;
        double vy;
        double vz;
        int pixels;
        int age;
        int life;
    }

    private static final List<Mote> MOTES = new ArrayList<>();
    private static final List<Spark> SPARKS = new ArrayList<>();
    private static final RandomSource RANDOM = RandomSource.create();

    private FlameFx() {
    }

    public static void clear() {
        MOTES.clear();
        SPARKS.clear();
    }

    private static Mote add(Vec3 at, Vec3 velocity, float half, int life) {
        if (MOTES.size() >= MAX_MOTES) {
            MOTES.remove(0);
        }
        Mote mote = new Mote();
        mote.x = mote.xo = at.x;
        mote.y = mote.yo = at.y;
        mote.z = mote.zo = at.z;
        mote.vx = velocity.x;
        mote.vy = velocity.y;
        mote.vz = velocity.z;
        mote.half = half;
        mote.life = Math.max(1, life);
        MOTES.add(mote);
        return mote;
    }

    public static void mote(Vec3 at, Vec3 velocity, float half, int life) {
        add(at, velocity, half, life);
    }

    public static void lick(Vec3 at, double spread) {
        Vec3 where = at.add((RANDOM.nextDouble() - 0.5) * 2.0 * spread, (RANDOM.nextDouble() - 0.5) * spread,
                (RANDOM.nextDouble() - 0.5) * 2.0 * spread);
        add(where, new Vec3(0.0, 0.05 + RANDOM.nextDouble() * 0.06, 0.0), 0.38F + RANDOM.nextFloat() * 0.18F,
                10 + RANDOM.nextInt(7));
    }

    public static void burst(Vec3 at, int count, double speed) {
        for (int i = 0; i < count; i++) {
            add(at, way().scale(speed * (0.5 + RANDOM.nextDouble() * 0.5)), 0.4F + RANDOM.nextFloat() * 0.22F,
                    9 + RANDOM.nextInt(7));
        }
        sparks(at, count, speed * 1.6);
    }

    private static Vec3 way() {
        Vec3 way = new Vec3(RANDOM.nextDouble() - 0.5, RANDOM.nextDouble() - 0.3, RANDOM.nextDouble() - 0.5);
        return way.lengthSqr() < 1.0E-6 ? new Vec3(0, 1, 0) : way.normalize();
    }

    public static void spark(Vec3 at, Vec3 velocity, int life) {
        if (SPARKS.size() >= MAX_SPARKS) {
            SPARKS.remove(0);
        }
        Spark spark = new Spark();
        spark.x = spark.xo = at.x;
        spark.y = spark.yo = at.y;
        spark.z = spark.zo = at.z;
        spark.vx = velocity.x;
        spark.vy = velocity.y;
        spark.vz = velocity.z;
        spark.pixels = RANDOM.nextInt(3) == 0 ? 2 : 1;
        spark.life = Math.max(1, life);
        SPARKS.add(spark);
    }

    public static void sparks(Vec3 at, int count, double speed) {
        for (int i = 0; i < count; i++) {
            spark(at, way().scale(speed * (0.4 + RANDOM.nextDouble() * 0.6)), 12 + RANDOM.nextInt(14));
        }
    }

    public static void pinned(int tag, Vec3 at, Vec3 velocity, float half, int life, int heat) {
        Mote mote = add(at, velocity, half, life);
        mote.tag = tag;
        mote.fixed = heat;
        mote.age = RANDOM.nextInt(PINNED_FRAMES);
        mote.life += mote.age;
    }

    public static void drop(int tag) {
        MOTES.removeIf(mote -> mote.tag == tag);
    }

    public static void tick() {
        for (Iterator<Mote> it = MOTES.iterator(); it.hasNext(); ) {
            Mote mote = it.next();
            if (++mote.age >= mote.life) {
                it.remove();
                continue;
            }
            mote.xo = mote.x;
            mote.yo = mote.y;
            mote.zo = mote.z;
            mote.x += mote.vx;
            mote.y += mote.vy;
            mote.z += mote.vz;
            if (mote.tag == UNTAGGED) {
                mote.vx *= TAPER;
                mote.vz *= TAPER;
            }
        }
        for (Iterator<Spark> it = SPARKS.iterator(); it.hasNext(); ) {
            Spark spark = it.next();
            if (++spark.age >= spark.life) {
                it.remove();
                continue;
            }
            spark.xo = spark.x;
            spark.yo = spark.y;
            spark.zo = spark.z;
            spark.x += spark.vx;
            spark.y += spark.vy;
            spark.z += spark.vz;
            spark.vx *= SPARK_DRAG;
            spark.vy = spark.vy * SPARK_DRAG + SPARK_LIFT;
            spark.vz *= SPARK_DRAG;
        }
    }

    private static int stage(Mote mote) {
        if (mote.fixed >= 0) {
            return Math.min(STAGE_COUNT - 1, mote.fixed);
        }
        int part = mote.age * 20 / mote.life;
        return part < 3 ? 0 : part < 10 ? 1 : part < 16 ? 2 : 3;
    }

    public static int dim(float strength) {
        int level = Math.max(0, Math.min(255, Math.round(255F * strength)));
        return 0xFF000000 | (level << 16) | (level << 8) | level;
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        if (MOTES.isEmpty() && SPARKS.isEmpty()) {
            return;
        }
        Vector3f leftF = camera.getLeftVector();
        Vector3f upF = camera.getUpVector();
        Vec3 right = new Vec3(-leftF.x(), -leftF.y(), -leftF.z());
        Vec3 up = new Vec3(upF.x(), upF.y(), upF.z());
        pass(poseStack, buffers, camera, partialTick, right, up, RenderType.entityTranslucentEmissive(SHEET), false);
        pass(poseStack, buffers, camera, partialTick, right, up, RenderType.eyes(SHEET), true);
    }

    private static void pass(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick,
                             Vec3 right, Vec3 up, RenderType type, boolean glow) {
        Vec3 eye = camera.getPosition();
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer out = buffers.getBuffer(type);
        for (Mote mote : MOTES) {
            Vec3 at = new Vec3(mote.xo + (mote.x - mote.xo) * partialTick - eye.x,
                    mote.yo + (mote.y - mote.yo) * partialTick - eye.y,
                    mote.zo + (mote.z - mote.zo) * partialTick - eye.z);
            if (at.lengthSqr() > RANGE * RANGE) {
                continue;
            }
            int stage = stage(mote);
            int frame;
            float half = mote.half;
            if (mote.fixed >= 0) {
                frame = mote.age % PINNED_FRAMES;
            } else {
                frame = Math.min(FRAME_COUNT - 1, mote.age * FRAME_COUNT / mote.life);
                float progress = mote.age / (float) mote.life;
                half *= (1F - progress * progress * 0.6F) * (mote.age == 0 ? 0.7F : 1F);
            }
            boolean dying = mote.fixed < 0 && stage == STAGE_COUNT - 1;
            int argb = glow ? dim(dying ? GLOW_DYING : GLOW) : dying ? BODY_DYING : BODY;
            float u0 = frame * CELL_U;
            float v0 = stage * CELL_V;
            quad(out, pose, at, right.scale(half), up.scale(half), u0, u0 + CELL_U, v0, v0 + CELL_V, argb, glow);
        }
        for (Spark spark : SPARKS) {
            Vec3 at = new Vec3(spark.xo + (spark.x - spark.xo) * partialTick - eye.x,
                    spark.yo + (spark.y - spark.yo) * partialTick - eye.y,
                    spark.zo + (spark.z - spark.zo) * partialTick - eye.z);
            if (at.lengthSqr() > RANGE * RANGE) {
                continue;
            }
            int heat = Math.min(STAGE_COUNT - 1, spark.age * STAGE_COUNT / spark.life);
            float half = spark.pixels * PIXEL * 0.5F;
            float u = (heat + 0.5F) * CELL_U;
            quad(out, pose, at, right.scale(half), up.scale(half), u, u, SWATCH_V, SWATCH_V,
                    glow ? dim(GLOW) : 0xFFFFFFFF, glow);
        }
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(type);
        }
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose, Vec3 at, Vec3 r, Vec3 u, float u0, float u1,
                             float v0, float v1, int argb, boolean both) {
        Vec3 topLeft = at.subtract(r).add(u);
        Vec3 bottomLeft = at.subtract(r).subtract(u);
        Vec3 bottomRight = at.add(r).subtract(u);
        Vec3 topRight = at.add(r).add(u);
        PixelDraw.quad(out, pose, topLeft, bottomLeft, bottomRight, topRight, u0, u1, v0, v1, argb,
                LightTexture.FULL_BRIGHT);
        if (both) {
            PixelDraw.quad(out, pose, topRight, bottomRight, bottomLeft, topLeft, u1, u0, v0, v1, argb,
                    LightTexture.FULL_BRIGHT);
        }
    }
}
