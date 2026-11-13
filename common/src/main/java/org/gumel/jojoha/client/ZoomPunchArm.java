package org.gumel.jojoha.client;

import org.jetbrains.annotations.Nullable;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.hamon.HamonFx;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ZoomPunchArm {
    private static final float ORIGIN_LIFT = 1.501F;

    private static final float CUBE_TOP = -2F;
    private static final float CUBE_END = 10F;
    private static final float CUBE_LENGTH = CUBE_END - CUBE_TOP;

    private static final float EXTEND_TICKS = 1F;

    private static final float HOLD_TICKS = 2.5F;

    private static final float BODY_ALPHA = 1.0F;

    private static final float GLOW_ALPHA = 0.3F;

    private static final float[] GLOW_PASSES = {1.0F, 0.5F};

    private static final List<Punch> ACTIVE = new ArrayList<>();

    private ZoomPunchArm() {
    }

    public static void begin(int playerId, double x, double y, double z, int ticks) {
        ACTIVE.removeIf(punch -> punch.playerId == playerId);
        ACTIVE.add(new Punch(playerId, new Vec3(x, y, z), ticks));
    }

    public static void tick() {
        Iterator<Punch> it = ACTIVE.iterator();
        while (it.hasNext()) {
            if (--it.next().remaining <= 0) {
                it.remove();
            }
        }
    }

    public record Reach(Vec3 at, float out) {
    }

    @Nullable
    public static Reach reach(Entity entity, float partialTick) {
        for (Punch punch : ACTIVE) {
            if (punch.playerId == entity.getId()) {
                float elapsed = punch.ticks - (punch.remaining - partialTick);
                return new Reach(punch.at, ease(Math.min(1F, elapsed / EXTEND_TICKS)) * fade(elapsed, punch.ticks));
            }
        }
        return null;
    }

    public static boolean hidesArm(AbstractClientPlayer player, float partialTick) {
        for (Punch punch : ACTIVE) {
            if (punch.playerId == player.getId()) {
                return punch.ticks - (punch.remaining - partialTick) <= HOLD_TICKS;
            }
        }
        return false;
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
                              float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ACTIVE.isEmpty() || minecraft.level == null) {
            return;
        }

        Vec3 cameraPos = camera.getPosition();

        for (Punch punch : ACTIVE) {
            Entity entity = minecraft.level.getEntity(punch.playerId);
            if (!(entity instanceof AbstractClientPlayer player)) {
                continue;
            }
            if (!(minecraft.getEntityRenderDispatcher().getRenderer(player)
                    instanceof PlayerRenderer renderer)) {
                continue;
            }

            draw(poseStack, buffers, punch, player, renderer, cameraPos, partialTick);
        }

        if (buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
    }

    private static void draw(PoseStack poseStack, MultiBufferSource buffers, Punch punch,
                             AbstractClientPlayer player, PlayerRenderer renderer,
                             Vec3 cameraPos, float partialTick) {
        float elapsed = punch.ticks - (punch.remaining - partialTick);
        float alpha = fade(elapsed, punch.ticks);
        if (alpha <= 0F) {
            return;
        }

        double px = Mth.lerp(partialTick, player.xo, player.getX());
        double py = Mth.lerp(partialTick, player.yo, player.getY());
        double pz = Mth.lerp(partialTick, player.zo, player.getZ());
        float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);

        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        boolean right = player.getMainArm() == HumanoidArm.RIGHT;
        ModelPart arm = right ? model.rightArm : model.leftArm;
        ModelPart sleeve = right ? model.rightSleeve : model.leftSleeve;

        Vector3f target = toModelSpace(new Vec3(punch.at.x - px, punch.at.y - py, punch.at.z - pz),
                bodyYaw);
        Vector3f aim = new Vector3f(target.x - arm.x, target.y - arm.y, target.z - arm.z);

        float full = aim.length();
        if (full < 1.0E-4F) {
            return;
        }
        aim.normalize();

        float reach = full * ease(Math.min(1F, elapsed / EXTEND_TICKS));
        if (reach <= CUBE_TOP) {
            return;
        }

        int light = Minecraft.getInstance().getEntityRenderDispatcher()
                .getPackedLightCoords(player, partialTick);
        ResourceLocation skin = player.getSkin().texture();

        poseStack.pushPose();
        poseStack.translate(px - cameraPos.x, py - cameraPos.y, pz - cameraPos.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - bodyYaw));
        poseStack.scale(-1F, -1F, 1F);
        poseStack.translate(0F, -ORIGIN_LIFT, 0F);

        poseStack.pushPose();
        poseStack.translate(arm.x / 16F, arm.y / 16F, arm.z / 16F);
        poseStack.mulPose(rotationTo(aim));

        poseStack.translate(0F, CUBE_TOP / 16F, 0F);
        poseStack.scale(1F, (reach - CUBE_TOP) / CUBE_LENGTH, 1F);
        poseStack.translate(0F, -CUBE_TOP / 16F, 0F);
        ghost(poseStack, buffers, arm, sleeve, skin, light, alpha);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(arm.x / 16F, arm.y / 16F, arm.z / 16F);
        poseStack.mulPose(rotationTo(aim));
        poseStack.translate(0F, (reach - CUBE_END) / 16F, 0F);
        ghost(poseStack, buffers, arm, sleeve, skin, light, alpha);
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void ghost(PoseStack poseStack, MultiBufferSource buffers, ModelPart arm,
                              ModelPart sleeve, ResourceLocation skin, int light, float alpha) {
        Rest armRest = Rest.take(arm);
        Rest sleeveRest = Rest.take(sleeve);
        try {
            RenderType body = RenderType.entityTranslucent(skin);
            int white = argb(alpha * BODY_ALPHA, 1F, 1F, 1F);
            arm.render(poseStack, buffers.getBuffer(body), light, OverlayTexture.NO_OVERLAY, white);
            sleeve.render(poseStack, buffers.getBuffer(body), light, OverlayTexture.NO_OVERLAY,
                    white);

            RenderType lit = RenderType.entityTranslucentEmissive(skin);
            for (float strength : GLOW_PASSES) {
                int glow = argb(alpha * GLOW_ALPHA * strength,
                        HamonFx.RED,
                        HamonFx.GREEN,
                        HamonFx.BLUE);
                arm.render(poseStack, buffers.getBuffer(lit), LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY, glow);
            }
        } finally {
            sleeveRest.restore(sleeve);
            armRest.restore(arm);
        }
    }

    private static Vector3f toModelSpace(Vec3 offsetFromFeet, float bodyYaw) {
        Vector3f v = new Vector3f((float) offsetFromFeet.x, (float) offsetFromFeet.y,
                (float) offsetFromFeet.z);
        v.rotateY((float) Math.toRadians(-(180F - bodyYaw)));
        v.set(-v.x, -v.y, v.z);
        v.add(0F, ORIGIN_LIFT, 0F);
        return v.mul(16F);
    }

    private static Quaternionf rotationTo(Vector3f direction) {
        if (Math.abs(direction.y) > 1F - 1.0E-6F) {
            return new Quaternionf().rotationZYX(0F, 0F,
                    direction.y > 0F ? 0F : -(float) Math.PI);
        }
        float xRot = -(float) Math.acos(Mth.clamp(direction.y, -1F, 1F));
        float yRot = (float) Math.atan2(-direction.x, -direction.z);
        return new Quaternionf().rotationZYX(0F, yRot, xRot);
    }

    private static float fade(float elapsed, int ticks) {
        if (elapsed <= HOLD_TICKS) {
            return 1F;
        }
        float left = ticks - elapsed;
        float over = Math.max(1F, ticks - HOLD_TICKS);
        return Mth.clamp(left / over, 0F, 1F);
    }

    private static float ease(float t) {
        float u = 1F - t;
        return 1F - u * u * u;
    }

    private static int argb(float alpha, float red, float green, float blue) {
        return (Math.round(Mth.clamp(alpha, 0F, 1F) * 255F) << 24)
                | (Math.round(red * 255F) << 16)
                | (Math.round(green * 255F) << 8)
                | Math.round(blue * 255F);
    }

    private record Rest(float x, float y, float z, float xRot, float yRot, float zRot,
                        boolean visible) {

        private static Rest take(ModelPart part) {
            Rest rest = new Rest(part.x, part.y, part.z, part.xRot, part.yRot, part.zRot,
                    part.visible);
            part.x = 0F;
            part.y = 0F;
            part.z = 0F;
            part.xRot = 0F;
            part.yRot = 0F;
            part.zRot = 0F;
            part.visible = true;
            return rest;
        }

        private void restore(ModelPart part) {
            part.x = x;
            part.y = y;
            part.z = z;
            part.xRot = xRot;
            part.yRot = yRot;
            part.zRot = zRot;
            part.visible = visible;
        }
    }

    private static final class Punch {
        private final int playerId;
        private final Vec3 at;
        private final int ticks;
        private int remaining;

        private Punch(int playerId, Vec3 at, int ticks) {
            this.playerId = playerId;
            this.at = at;
            this.ticks = Math.max(1, ticks);
            this.remaining = this.ticks;
        }
    }
}
