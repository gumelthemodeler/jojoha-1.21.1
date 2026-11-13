package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class MeteorStreaks {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/meteor_streak.png");
    private static final RenderType TYPE = RenderType.eyes(TEXTURE);

    private static final float LENGTH = 12F;
    private static final float WIDTH = 1.1F;
    private static final float BIG_LENGTH = 22F;
    private static final float BIG_WIDTH = 2.2F;
    private static final float SHARD_LENGTH = 5F;
    private static final float SHARD_WIDTH = 0.6F;
    private static final int SHARDS = 3;
    private static final int SHARD_LIFE = 12;
    private static final float SHARD_SPREAD = 0.25F;
    private static final float SHARD_SPEED = 0.75F;
    private static final float FADE_IN = 3F;
    private static final float BURN_OUT = 0.3F;
    private static final int FRAME_TICKS = 2;
    private static final int MOST = 64;

    private static final List<Streak> ACTIVE = new ArrayList<>();

    private MeteorStreaks() {
    }

    public static void begin(double x, double y, double z, float vx, float vy, float vz, int life, boolean big) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || ACTIVE.size() >= MOST) {
            return;
        }
        ACTIVE.add(new Streak(new Vec3(x, y, z), new Vec3(vx, vy, vz), level.getGameTime(), Math.max(1, life),
                big ? BIG_LENGTH : LENGTH, big ? BIG_WIDTH : WIDTH, big));
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            ACTIVE.clear();
            return;
        }
        if (ACTIVE.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        List<Streak> shards = new ArrayList<>();
        Vec3 eye = camera.getPosition();
        VertexConsumer consumer = buffers.getBuffer(TYPE);
        poseStack.pushPose();
        poseStack.translate(-eye.x, -eye.y, -eye.z);
        Iterator<Streak> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Streak streak = it.next();
            float age = (now - streak.born) + partialTick;
            if (age > streak.life || age < 0F) {
                if (streak.big && age >= 0F) {
                    shatter(streak, level.random, shards);
                }
                it.remove();
                continue;
            }
            draw(poseStack, consumer, eye, streak, age);
        }
        poseStack.popPose();
        ACTIVE.addAll(shards);
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(TYPE);
        }
    }

    private static void shatter(Streak streak, RandomSource random, List<Streak> into) {
        Vec3 head = streak.from.add(streak.velocity.scale(streak.life));
        for (int i = 0; i < SHARDS; i++) {
            Vec3 kick = new Vec3(random.nextGaussian(), random.nextGaussian() * 0.5, random.nextGaussian())
                    .scale(SHARD_SPREAD * streak.velocity.length());
            Vec3 velocity = streak.velocity.scale(SHARD_SPEED).add(kick);
            into.add(new Streak(head, velocity, streak.born + streak.life, SHARD_LIFE + random.nextInt(6),
                    SHARD_LENGTH, SHARD_WIDTH, false));
        }
    }

    private static void draw(PoseStack poseStack, VertexConsumer consumer, Vec3 eye, Streak streak, float age) {
        double speed = streak.velocity.length();
        if (speed < 1.0E-4) {
            return;
        }
        Vec3 along = streak.velocity.scale(1.0 / speed);
        Vec3 head = streak.from.add(streak.velocity.scale(age));
        float travelled = (float) (speed * age);
        float shown = Math.min(streak.length, travelled);
        if (shown < 0.05F) {
            return;
        }
        float left = 1F - age / streak.life;
        float brightness = Mth.clamp(age / FADE_IN, 0F, 1F) * Mth.clamp(left / BURN_OUT, 0F, 1F);
        if (brightness <= 0.01F) {
            return;
        }
        Vec3 tail = head.subtract(along.scale(shown));
        Vec3 toEye = eye.subtract(head);
        Vec3 side = along.cross(toEye);
        side = side.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 1.0, 0.0) : side.normalize();
        side = side.scale(streak.width * 0.5);
        float u0 = 1F - shown / streak.length;
        int frame = ((int) (age / FRAME_TICKS)) & 1;
        float v0 = frame * 0.5F;
        float v1 = v0 + 0.5F;
        int shade = Math.round(brightness * 255F);
        PoseStack.Pose pose = poseStack.last();
        vertex(consumer, pose, tail.subtract(side), u0, v1, shade);
        vertex(consumer, pose, head.subtract(side), 1F, v1, shade);
        vertex(consumer, pose, head.add(side), 1F, v0, shade);
        vertex(consumer, pose, tail.add(side), u0, v0, shade);
        vertex(consumer, pose, tail.add(side), u0, v0, shade);
        vertex(consumer, pose, head.add(side), 1F, v0, shade);
        vertex(consumer, pose, head.subtract(side), 1F, v1, shade);
        vertex(consumer, pose, tail.subtract(side), u0, v1, shade);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 at, float u, float v, int shade) {
        consumer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor(shade, shade, shade, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0F, 1F, 0F);
    }

    private static final class Streak {
        private final Vec3 from;
        private final Vec3 velocity;
        private final long born;
        private final int life;
        private final float length;
        private final float width;
        private final boolean big;

        private Streak(Vec3 from, Vec3 velocity, long born, int life, float length, float width, boolean big) {
            this.from = from;
            this.velocity = velocity;
            this.born = born;
            this.life = life;
            this.length = length;
            this.width = width;
            this.big = big;
        }
    }
}
