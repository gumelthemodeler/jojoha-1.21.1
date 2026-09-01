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

/**
 * The trail of coloured silhouettes a Stand leaves behind its user when it throws them.
 *
 * <p>Each ghost is a full copy of the pose at one past tick, not just a position. Limb rotations
 * are captured along with the location, because a trail of identically-posed bodies reads as a row
 * of statues - it is the limbs frozen mid-stride at different points that makes it look like one
 * person moving fast.
 *
 * <p>Drawn as flat colour rather than as tinted skin: the point is the Stand's presence, and a
 * solid silhouette is what the effect is in the source material.
 */
public final class StandAfterimages {
    /** Vanilla's own white texture - every UV samples pure white, so the tint is the only colour. */
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    /** How long one ghost hangs around after being laid down. */
    private static final int GHOST_LIFETIME_TICKS = 9;
    /** Cap per player - a runaway trail would be both ugly and expensive. */
    private static final int MAX_GHOSTS = 10;
    /** Peak opacity of the freshest ghost. Kept low; these overlap each other and the player. */
    private static final float PEAK_ALPHA = 0.55F;

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

    /** Ages out ghosts and finished trails. Call once per client tick. */
    public static void tick() {
        TRAILS.values().forEach(Trail::age);
        TRAILS.entrySet().removeIf(entry -> entry.getValue().isFinished());
    }

    /**
     * Lays down this tick's ghost and draws every ghost already recorded.
     *
     * <p>Capture and draw share a call because both need the posed model, and it only holds this
     * player's pose during their own render - by the end of the frame those same parts have been
     * reused for whoever was drawn next.
     */
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

        /**
         * One ghost per tick, however many frames get drawn within it.
         *
         * <p>Without the guard the density of the trail would follow the framerate - the same dash
         * would smear at 240fps and dot at 30.
         */
        private void captureOnce(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model, long gameTime) {
            if (ticksLeft <= 0 || gameTime == lastCaptureTick) {
                return;
            }

            lastCaptureTick = gameTime;
            ghosts.addFirst(new Ghost(player, model));
            while (ghosts.size() > MAX_GHOSTS) {
                ghosts.removeLast();
            }
        }

        private void draw(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model,
                          PoseStack poseStack, MultiBufferSource bufferSource, float partialTick) {
            if (ghosts.isEmpty()) {
                return;
            }

            // The live pose has to be put back exactly: one model instance draws every player in
            // the world, so anything left behind smears onto the next one.
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

                ghost.applyTo(model);

                poseStack.pushPose();
                // Replicates LivingEntityRenderer's own chain, since this draws outside it: entity
                // scale, body yaw, the vertical flip, the player model's 0.9375 shrink, then the
                // drop to the model origin.
                poseStack.translate(ghost.x - nowX, ghost.y - nowY, ghost.z - nowZ);
                poseStack.scale(scale, scale, scale);
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - ghost.bodyYaw));
                poseStack.scale(-1F, -1F, 1F);
                poseStack.scale(0.9375F, 0.9375F, 0.9375F);
                poseStack.translate(0F, -1.501F, 0F);

                model.renderToBuffer(poseStack,
                        bufferSource.getBuffer(RenderType.entityTranslucent(WHITE)),
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        tint(color, fade * PEAK_ALPHA));

                poseStack.popPose();
            }

            live.applyTo(model);
        }
    }

    /** One frozen frame of a player: where they were, which way they faced, how they were posed. */
    private static final class Ghost {
        private final double x;
        private final double y;
        private final double z;
        private final float bodyYaw;
        private final float[] rotations = new float[PART_COUNT * 3];

        private int age;

        private Ghost(AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> model) {
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.bodyYaw = player.yBodyRot;

            ModelPart[] parts = partsOf(model);
            for (int i = 0; i < parts.length; i++) {
                rotations[i * 3] = parts[i].xRot;
                rotations[i * 3 + 1] = parts[i].yRot;
                rotations[i * 3 + 2] = parts[i].zRot;
            }
        }

        private void applyTo(PlayerModel<AbstractClientPlayer> model) {
            ModelPart[] parts = partsOf(model);
            for (int i = 0; i < parts.length; i++) {
                parts[i].xRot = rotations[i * 3];
                parts[i].yRot = rotations[i * 3 + 1];
                parts[i].zRot = rotations[i * 3 + 2];
            }

            // The cosmetic layers ride whatever their body part is doing, so they have to follow.
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
