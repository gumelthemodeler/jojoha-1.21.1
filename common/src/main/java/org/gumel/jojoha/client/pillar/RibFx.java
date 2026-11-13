package org.gumel.jojoha.client.pillar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;

import java.util.ArrayList;
import java.util.List;

public final class RibFx {
    private static final ResourceLocation FLASH =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/pillar_flash.png");
    private static final float PX = PixelDraw.PIXEL;
    private static final float ROLL = 30F;
    private static final float[] TIERS = {5F, 8F};
    private static final int EXTEND = 2;
    private static final int RETRACT = 4;
    private static final int RIBS = 8;
    private static final int FLASH_FRAMES = 4;
    private static final float FLASH_SIZE = 3.0F;

    private static final List<Ribs> RIBS_OUT = new ArrayList<>();
    private static final List<Flash> FLASHES = new ArrayList<>();

    private RibFx() {
    }

    public static void clear() {
        RIBS_OUT.clear();
        FLASHES.clear();
    }

    public static void ribs(int entity, int hold) {
        RIBS_OUT.add(new Ribs(entity, hold));
    }

    public static void flash(Vec3 at) {
        FLASHES.add(new Flash(at));
    }

    public static void tick() {
        RIBS_OUT.removeIf(ribs -> ++ribs.age > EXTEND + ribs.hold + RETRACT);
        FLASHES.removeIf(flash -> ++flash.age >= FLASH_FRAMES);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick,
                              ClientLevel level) {
        Vec3 eye = camera.getPosition();
        if (!RIBS_OUT.isEmpty()) {
            VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(PillarModels.RIB_SPIKE_TEXTURE));
            for (Ribs ribs : RIBS_OUT) {
                Entity entity = level.getEntity(ribs.entity);
                if (entity instanceof LivingEntity living) {
                    ribs.draw(poseStack, out, eye, partialTick, level, living);
                }
            }
        }
        if (!FLASHES.isEmpty()) {
            VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(FLASH));
            for (Flash flash : FLASHES) {
                poseStack.pushPose();
                Vec3 at = flash.at.subtract(eye);
                poseStack.translate(at.x, at.y, at.z);
                poseStack.mulPose(camera.rotation());
                float half = FLASH_SIZE / 2;
                float u0 = flash.age / (float) FLASH_FRAMES;
                float u1 = (flash.age + 1) / (float) FLASH_FRAMES;
                PoseStack.Pose pose = poseStack.last();
                PixelDraw.vertex(out, pose, -half, -half, 0, u1, 1, 0xFFFFFFFF, LightTexture.FULL_BRIGHT, 0, 0, 1);
                PixelDraw.vertex(out, pose, half, -half, 0, u0, 1, 0xFFFFFFFF, LightTexture.FULL_BRIGHT, 0, 0, 1);
                PixelDraw.vertex(out, pose, half, half, 0, u0, 0, 0xFFFFFFFF, LightTexture.FULL_BRIGHT, 0, 0, 1);
                PixelDraw.vertex(out, pose, -half, half, 0, u1, 0, 0xFFFFFFFF, LightTexture.FULL_BRIGHT, 0, 0, 1);
                poseStack.popPose();
            }
        }
    }

    private static final class Ribs {
        final int entity;
        final int hold;
        int age;

        Ribs(int entity, int hold) {
            this.entity = entity;
            this.hold = hold;
        }

        float out(float t) {
            if (t < EXTEND) {
                return t / EXTEND;
            }
            if (t < EXTEND + hold) {
                return 1F;
            }
            return Mth.clamp(1F - (t - EXTEND - hold) / RETRACT, 0F, 1F);
        }

        void draw(PoseStack poseStack, VertexConsumer out, Vec3 eye, float partialTick, ClientLevel level,
                  LivingEntity body) {
            float t = age + partialTick;
            float reach = out(t);
            if (reach <= 0.01F) {
                return;
            }
            Vec3 base = body.getPosition(partialTick);
            Vec3 at = base.subtract(eye);
            float yaw = Mth.rotLerp(partialTick, body.yBodyRotO, body.yBodyRot);
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(base.add(0, 1, 0)));
            ModelPart spike = PillarModels.ribSpike();
            for (int rib = 0; rib < RIBS; rib++) {
                float angle = Mth.TWO_PI * (rib + 0.5F) / RIBS;
                float sin = Mth.sin(angle);
                float cos = Mth.cos(angle);
                poseStack.pushPose();
                poseStack.translate(at.x, at.y, at.z);
                poseStack.mulPose(Axis.YP.rotationDegrees(180F - yaw));
                poseStack.scale(-1F, -1F, 1F);
                poseStack.translate(0F, -1.501F, 0F);
                poseStack.translate(sin * 3F * PX, TIERS[rib % TIERS.length] * PX, -cos * 1.5F * PX);
                poseStack.mulPose(Axis.YP.rotation(-angle));
                if (sin < 0F) {
                    poseStack.scale(-1F, 1F, 1F);
                }
                poseStack.mulPose(Axis.ZP.rotationDegrees(rib % 2 == 0 ? ROLL : -ROLL));
                poseStack.scale(1F, 1F, reach);
                spike.render(poseStack, out, light, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
        }
    }

    private static final class Flash {
        final Vec3 at;
        int age;

        Flash(Vec3 at) {
            this.at = at;
        }
    }
}
