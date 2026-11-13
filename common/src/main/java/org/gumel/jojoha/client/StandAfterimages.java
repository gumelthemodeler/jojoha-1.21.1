package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.network.packet.StandAfterimagePacket;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StandAfterimages {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private static final int GHOST_LIFETIME_TICKS = 9;
    private static final int MAX_GHOSTS = 10;
    private static final float PEAK_ALPHA = 0.55F;

    private static final double GHOST_SPACING = 0.5;
    private static final double GHOST_CLEARANCE = 0.9;

    private static final int PART_COUNT = 6;

    private static final Map<UUID, Trail> TRAILS = new HashMap<>();

    private StandAfterimages() {
    }

    public static void begin(StandAfterimagePacket packet) {
        TRAILS.computeIfAbsent(packet.playerId(), id -> new Trail())
                .restart(packet.color(), packet.durationTicks());
    }

    public static void clear() {
        TRAILS.clear();
    }

    public static void tick() {
        TRAILS.values().forEach(Trail::age);
        TRAILS.entrySet().removeIf(entry -> entry.getValue().isFinished());
    }

    public static void renderTrail(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model,
                                   PoseStack poseStack, MultiBufferSource bufferSource,
                                   float partialTick, long gameTime) {
        Trail trail = TRAILS.get(player.getUUID());
        if (trail == null) {
            return;
        }

        trail.captureOnce(player, model, gameTime);

        trail.draw(player, model, poseStack, bufferSource, partialTick);
    }

    private static final class Trail {
        private final Deque<Ghost> ghosts = new ArrayDeque<>();
        private int color = 0xFFFFFF;
        private int ticksLeft;
        private long lastCaptureTick = Long.MIN_VALUE;

        private void restart(int color, int durationTicks) {
            this.color = color;
            this.ticksLeft = durationTicks;
        }

        private void age() {
            if (ticksLeft > 0) {
                ticksLeft--;
            }
            ghosts.removeIf(ghost -> ++ghost.age > GHOST_LIFETIME_TICKS);
        }

        private boolean isFinished() {
            return ticksLeft <= 0 && ghosts.isEmpty();
        }

        private void captureOnce(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model, long gameTime) {
            if (ticksLeft <= 0 || gameTime == lastCaptureTick) {
                return;
            }
            Ghost last = ghosts.peekFirst();
            if (last != null && last.distanceTo(player) < GHOST_SPACING) {
                return;
            }

            lastCaptureTick = gameTime;
            ghosts.addFirst(new Ghost(player, model, gameTime));
            while (ghosts.size() > MAX_GHOSTS) {
                ghosts.removeLast();
            }
        }

        private void draw(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model,
                          PoseStack poseStack, MultiBufferSource bufferSource, float partialTick) {
            if (ghosts.isEmpty()) {
                return;
            }

            Ghost live = new Ghost(player, model);

            double nowX = Mth.lerp(partialTick, player.xo, player.getX());
            double nowY = Mth.lerp(partialTick, player.yo, player.getY());
            double nowZ = Mth.lerp(partialTick, player.zo, player.getZ());
            float scale = player.getScale();

            for (Ghost ghost : ghosts) {
                float fade = 1F - (ghost.age / (float) GHOST_LIFETIME_TICKS);
                if (fade <= 0F) {
                    continue;
                }
                if (ghost.distanceTo(nowX, nowY, nowZ) < GHOST_CLEARANCE) {
                    continue;
                }

                ghost.applyTo(model);

                poseStack.pushPose();
                poseStack.translate(ghost.x - nowX, ghost.y - nowY, ghost.z - nowZ);
                poseStack.scale(scale, scale, scale);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - ghost.bodyYaw));
                poseStack.scale(-1F, -1F, 1F);
                poseStack.scale(0.9375F, 0.9375F, 0.9375F);
                poseStack.translate(0F, -1.501F, 0F);
                PlayerMoveAnimation.root(poseStack, player, ghost.time);

                model.renderToBuffer(poseStack,
                        bufferSource.getBuffer(RenderType.entityTranslucent(WHITE)),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        tint(color, fade * PEAK_ALPHA));

                poseStack.popPose();
            }

            live.applyTo(model);
        }
    }

    private static final class Ghost {
        private final double x;
        private final double y;
        private final double z;
        private final float bodyYaw;
        private final float time;
        private final float[] rotations = new float[PART_COUNT * 3];
        private final float[] offsets = new float[PART_COUNT * 3];

        private int age;

        private Ghost(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model) {
            this(player, model, 0L);
        }

        private Ghost(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model, long gameTime) {
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.bodyYaw = player.yBodyRot;
            this.time = gameTime;

            ModelPart[] parts = partsOf(model);
            for (int i = 0; i < parts.length; i++) {
                rotations[i * 3] = parts[i].xRot;
                rotations[i * 3 + 1] = parts[i].yRot;
                rotations[i * 3 + 2] = parts[i].zRot;
                offsets[i * 3] = parts[i].x;
                offsets[i * 3 + 1] = parts[i].y;
                offsets[i * 3 + 2] = parts[i].z;
            }
        }

        private double distanceTo(AbstractClientPlayer player) {
            return distanceTo(player.getX(), player.getY(), player.getZ());
        }

        private double distanceTo(double atX, double atY, double atZ) {
            double dx = x - atX, dy = y - atY, dz = z - atZ;
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        private void applyTo(PlayerModel<AbstractClientPlayer> model) {
            ModelPart[] parts = partsOf(model);
            for (int i = 0; i < parts.length; i++) {
                parts[i].xRot = rotations[i * 3];
                parts[i].yRot = rotations[i * 3 + 1];
                parts[i].zRot = rotations[i * 3 + 2];
                parts[i].x = offsets[i * 3];
                parts[i].y = offsets[i * 3 + 1];
                parts[i].z = offsets[i * 3 + 2];
            }

            model.hat.copyFrom(model.head);
            model.jacket.copyFrom(model.body);
            model.rightSleeve.copyFrom(model.rightArm);
            model.leftSleeve.copyFrom(model.leftArm);
            model.rightPants.copyFrom(model.rightLeg);
            model.leftPants.copyFrom(model.leftLeg);
        }
    }

    private static ModelPart[] partsOf(PlayerModel<AbstractClientPlayer> model) {
        return new ModelPart[] {
                model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg
        };
    }

    private static int tint(int rgb, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255F), 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
