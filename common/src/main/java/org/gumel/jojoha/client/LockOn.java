package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.client.compat.ShoulderSurfingCamera;
import org.gumel.jojoha.data.ClientPlayerDataCache;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandTypes;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class LockOn {
    private static final ResourceLocation MARK =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/particle/hermit_purple_block_target.png");

    private static final double RANGE = 24.0;
    private static final double KEEP_RANGE = 32.0;
    private static final double CONE_DEGREES = 40.0;
    private static final double DEGREES_PER_BLOCK = 0.75;
    private static final int UNSEEN_TICKS = 40;
    private static final float FOLLOW_RATE = 14F;
    private static final float MAX_FRAME_SECONDS = 0.1F;
    private static final double AIM_HEIGHT = 0.6;
    private static final float MARK_SIZE = 0.6F;
    private static final float MARK_SPIN = 40F;

    private static LivingEntity target;
    private static int unseen;
    private static long lastFrame;
    private static boolean wanted;

    private LockOn() {
    }

    public static boolean active() {
        return target != null;
    }

    public static boolean claimsPick() {
        if (!ShoulderSurfingCamera.installed() || Minecraft.getInstance().screen != null || !standOut()) {
            return false;
        }
        wanted = true;
        return true;
    }

    public static void tick() {
        if (!ShoulderSurfingCamera.installed()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean pressed = wanted;
        wanted = false;
        if (player == null || !standOut()) {
            release();
            return;
        }
        if (pressed && minecraft.screen == null) {
            if (target != null) {
                release();
            } else {
                target = pick(minecraft, player);
                unseen = 0;
            }
        }
        if (target != null && !keep(player, target)) {
            release();
        }
    }

    private static boolean standOut() {
        JojohaPlayerData data = ClientPlayerDataCache.data;
        return data != null && data.stand.isPresent() && data.standSummoned && !data.standPiloting;
    }

    private static void release() {
        target = null;
        unseen = 0;
        lastFrame = 0L;
    }

    private static LivingEntity pick(Minecraft minecraft, LocalPlayer player) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 from = camera.getPosition();
        Vector3f facing = camera.getLookVector();
        Vec3 look = new Vec3(facing.x(), facing.y(), facing.z()).normalize();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RANGE), entity -> lockable(player, entity))) {
            Vec3 toward = aimPoint(candidate, 1F).subtract(from);
            double distance = toward.length();
            if (distance < 1.0E-4 || player.distanceTo(candidate) > RANGE) {
                continue;
            }
            double angle = Math.toDegrees(Math.acos(Mth.clamp(look.dot(toward.scale(1.0 / distance)), -1.0, 1.0)));
            if (angle > CONE_DEGREES || !player.hasLineOfSight(candidate)) {
                continue;
            }
            double score = angle + distance * DEGREES_PER_BLOCK;
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean lockable(LocalPlayer player, LivingEntity entity) {
        if (entity == player || !entity.isAlive() || entity.isSpectator() || entity.isInvisibleTo(player)
                || entity instanceof StandEntity || entity instanceof ArmorStand
                || player.isAlliedTo(entity) || entity.isPassengerOfSameVehicle(player)) {
            return false;
        }
        return !(entity instanceof OwnableEntity pet && player.getUUID().equals(pet.getOwnerUUID()));
    }

    private static boolean keep(LocalPlayer player, LivingEntity locked) {
        if (!locked.isAlive() || locked.isRemoved() || locked.level() != player.level()
                || player.distanceTo(locked) > KEEP_RANGE || !player.isAlive()) {
            return false;
        }
        unseen = player.hasLineOfSight(locked) ? 0 : unseen + 1;
        return unseen <= UNSEEN_TICKS;
    }

    private static Vec3 aimPoint(LivingEntity entity, float partialTick) {
        return entity.getPosition(partialTick).add(0, entity.getBbHeight() * AIM_HEIGHT, 0);
    }

    public static void frame() {
        LivingEntity locked = target;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (locked == null || player == null || minecraft.isPaused()) {
            lastFrame = 0L;
            return;
        }
        long now = System.nanoTime();
        float seconds = lastFrame == 0L ? 0F : Math.min(MAX_FRAME_SECONDS, (now - lastFrame) / 1.0E9F);
        lastFrame = now;
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        Vec3 toward = aimPoint(locked, partialTick).subtract(player.getEyePosition(partialTick));
        float yRot = (float) (Mth.atan2(toward.z, toward.x) * Mth.RAD_TO_DEG) - 90F;
        float xRot = Mth.clamp((float) (-Mth.atan2(toward.y, toward.horizontalDistance()) * Mth.RAD_TO_DEG), -90F, 90F);
        float share = 1F - (float) Math.exp(-FOLLOW_RATE * seconds);

        ShoulderSurfingCamera.follow(yRot, xRot, share);

        float turnY = Mth.wrapDegrees(yRot - player.getYRot()) * share;
        float turnX = Mth.clamp(player.getXRot() + (xRot - player.getXRot()) * share, -90F, 90F) - player.getXRot();
        player.setYRot(player.getYRot() + turnY);
        player.yRotO += turnY;
        player.setXRot(player.getXRot() + turnX);
        player.xRotO += turnX;
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        LivingEntity locked = target;
        if (locked == null) {
            return;
        }
        Vec3 spot = aimPoint(locked, partialTick).subtract(camera.getPosition());
        int colour = colour();
        float red = ((colour >> 16) & 0xFF) / 255F;
        float green = ((colour >> 8) & 0xFF) / 255F;
        float blue = (colour & 0xFF) / 255F;
        float seconds = (System.currentTimeMillis() % 100000L) / 1000F;

        poseStack.pushPose();
        poseStack.translate(spot.x, spot.y, spot.z);
        poseStack.mulPose(camera.rotation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(seconds * MARK_SPIN));
        poseStack.scale(MARK_SIZE, MARK_SIZE, MARK_SIZE);
        VertexConsumer buffer = buffers.getBuffer(RenderType.textSeeThrough(MARK));
        Matrix4f pose = poseStack.last().pose();
        corner(buffer, pose, -0.5F, -0.5F, 0F, 1F, red, green, blue);
        corner(buffer, pose, 0.5F, -0.5F, 1F, 1F, red, green, blue);
        corner(buffer, pose, 0.5F, 0.5F, 1F, 0F, red, green, blue);
        corner(buffer, pose, -0.5F, 0.5F, 0F, 0F, red, green, blue);
        poseStack.popPose();
        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
    }

    private static int colour() {
        JojohaPlayerData data = ClientPlayerDataCache.data;
        return data == null || !data.stand.isPresent() ? 0xFFFFFF
                : StandTypes.byIdOrDefault(data.stand.standId()).auraColor();
    }

    private static void corner(VertexConsumer buffer, Matrix4f pose, float x, float y, float u, float v,
                               float red, float green, float blue) {
        buffer.addVertex(pose, x, y, 0F)
                .setColor(red, green, blue, 1F)
                .setUv(u, v)
                .setLight(LightTexture.FULL_BRIGHT);
    }
}
