package org.gumel.jojoha.client.pillar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;

import java.util.ArrayList;
import java.util.List;

public final class VesselFx {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/fx/pillar_vessel.png");
    private static final int SEGMENTS = 14;
    private static final float WIDTH = 2F / 16F;
    private static final float TEXTURE_BLOCKS = 2F;
    private static final float PUMP_PER_TICK = 2F / 32F;

    private static final List<Vessel> ACTIVE = new ArrayList<>();

    private VesselFx() {
    }

    public static void add(int from, int to, int extend, int hold, int retract, boolean needle, int finger) {
        ACTIVE.add(new Vessel(from, to, extend, hold, retract, needle, finger));
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static void tick() {
        ACTIVE.removeIf(vessel -> ++vessel.age > vessel.extend + vessel.hold + vessel.retract);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick,
                              ClientLevel level) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Vec3 eye = camera.getPosition();
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        poseStack.pushPose();
        poseStack.translate(-eye.x, -eye.y, -eye.z);
        PoseStack.Pose pose = poseStack.last();
        for (Vessel vessel : ACTIVE) {
            Entity source = level.getEntity(vessel.from);
            Entity target = level.getEntity(vessel.to);
            if (source == null || target == null) {
                continue;
            }
            vessel.draw(out, pose, eye, partialTick, level, source, target);
        }
        poseStack.popPose();
    }

    private static final class Vessel {
        final int from;
        final int to;
        final int extend;
        final int hold;
        final int retract;
        final boolean needle;
        final int finger;
        int age;

        Vessel(int from, int to, int extend, int hold, int retract, boolean needle, int finger) {
            this.from = from;
            this.to = to;
            this.extend = Math.max(1, extend);
            this.hold = hold;
            this.retract = Math.max(1, retract);
            this.needle = needle;
            this.finger = finger;
        }

        float reach(float t) {
            if (t < extend) {
                return t / extend;
            }
            if (t < extend + hold) {
                return 1F;
            }
            return Mth.clamp(1F - (t - extend - hold) / retract, 0F, 1F);
        }

        void draw(VertexConsumer out, PoseStack.Pose pose, Vec3 eye, float partialTick, ClientLevel level,
                  Entity source, Entity target) {
            float t = age + partialTick;
            float reach = reach(t);
            if (reach <= 0.01F) {
                return;
            }
            Vec3 hand = PillarClient.hand(source, partialTick);
            Vec3[] spread = PixelDraw.basis(PillarClient.look(source, partialTick));
            hand = hand.add(spread[1].scale((finger - 1) * 0.06)).add(spread[2].scale((finger % 2) * 0.04));
            Vec3 end = target.getBoundingBox().getCenter().add(0, (finger - 1) * 0.15, 0);
            Vec3[] basis = PixelDraw.basis(end.subtract(hand));
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(hand));
            float pump = -age * PUMP_PER_TICK;
            Vec3 previous = hand;
            double travelled = 0;
            for (int i = 1; i <= SEGMENTS; i++) {
                float along = i / (float) SEGMENTS;
                Vec3 at = hand.add(end.subtract(hand).scale(along * reach));
                double bend = Math.sin(along * Math.PI);
                at = at.add(basis[1].scale(Math.sin(age * 0.6 + i * 0.9 + finger * 2.1) * 0.08 * bend))
                        .add(basis[2].scale(Math.cos(age * 0.5 + i * 0.7 + finger) * 0.06 * bend - 0.12 * bend * (1 - reach)));
                double step = previous.distanceTo(at);
                float u0 = (float) (travelled / TEXTURE_BLOCKS) + pump;
                float u1 = (float) ((travelled + step) / TEXTURE_BLOCKS) + pump;
                float width = needle && i >= SEGMENTS - 1 ? WIDTH * 0.5F : WIDTH;
                PixelDraw.ribbon(out, pose, previous, at, eye, width, u0, u1, 0F, 1F, 0xFFFFFFFF, light);
                travelled += step;
                previous = at;
            }
        }
    }
}
