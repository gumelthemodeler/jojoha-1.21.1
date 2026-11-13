package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.combat.StandFist;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.util.RenderUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BarrageGhosts {
    private static final String RIG = "barrage_rig";

    private static final double ECHO_WINDOW_TICKS = 2.0;

    private static final float ECHO_ALPHA = 0.16F;

    private static final float PHANTOM_BODY_ALPHA = 0.2F;

    private static final float PHANTOM_GLOW_ALPHA = 0.1F;

    private static final float FAR_ALPHA = 0.55F;

    private static final float FAR_GROW = 1.2F;

    private static final float FOREARM_ALPHA = 0.5F;

    private static final String[] FOREARMS = {"bone2", "bone3"};

    private record Shot(GeoBone bone, GeoBone forearm, Matrix4f pose, Matrix3f normal) {
    }

    private static final class Frame {
        final double time;
        final Vec3 camera;
        final List<Shot> shots = new ArrayList<>();

        Frame(double time, Vec3 camera) {
            this.time = time;
            this.camera = camera;
        }
    }

    private static final Map<Integer, ArrayDeque<Frame>> ECHOES = new HashMap<>();

    private static Frame recording;

    private static final Matrix4f RIG_POSE = new Matrix4f();

    private static final Matrix3f RIG_NORMAL = new Matrix3f();

    private static boolean rigSeen;

    private BarrageGhosts() {
    }

    public static void forget(Set<Integer> keep) {
        ECHOES.keySet().retainAll(keep);
    }

    static void begin(StandEntity stand, float partialTick) {
        recording = null;
        rigSeen = false;
        int copies = JojohaClientConfig.BARRAGE_ECHO.whole();
        if (!stand.isBarraging() || copies <= 0 || stand.level() != Minecraft.getInstance().level) {
            ECHOES.remove(stand.getId());
            return;
        }
        double now = BarrageLoop.clock(stand, partialTick);
        ArrayDeque<Frame> frames = ECHOES.computeIfAbsent(stand.getId(), id -> new ArrayDeque<>());
        Frame last = frames.peekLast();
        if (last == null || now < last.time || now - last.time >= ECHO_WINDOW_TICKS / copies) {
            recording = new Frame(now, camera());
            frames.addLast(recording);
        }
        while (frames.size() > copies + 2) {
            frames.removeFirst();
        }
    }

    static void rig(GeoBone bone, PoseStack poseStack) {
        if (RIG.equals(bone.getName())) {
            RIG_POSE.set(poseStack.last().pose());
            RIG_NORMAL.set(poseStack.last().normal());
            rigSeen = true;
        }
    }

    static void record(GeoBone bone, GeoBone forearm, PoseStack poseStack) {
        if (recording != null && Math.abs(poseStack.last().pose().determinant()) > 1.0E-12) {
            recording.shots.add(new Shot(bone, forearm, new Matrix4f(poseStack.last().pose()),
                    new Matrix3f(poseStack.last().normal())));
        }
    }

    static void draw(GeoRenderer<StandEntity> renderer, PoseStack poseStack, StandEntity stand, BakedGeoModel model,
                     MultiBufferSource buffers, ResourceLocation texture, int colour, float partialTick,
                     int packedOverlay) {
        echoes(renderer, poseStack, stand, buffers, texture, colour, partialTick, packedOverlay);
        phantoms(renderer, poseStack, stand, model, buffers, texture, colour, partialTick, packedOverlay);
        recording = null;
    }

    private static Vec3 camera() {
        return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
    }

    private static void echoes(GeoRenderer<StandEntity> renderer, PoseStack poseStack, StandEntity stand,
                               MultiBufferSource buffers, ResourceLocation texture, int colour, float partialTick,
                               int packedOverlay) {
        ArrayDeque<Frame> frames = ECHOES.get(stand.getId());
        if (frames == null || frames.isEmpty()) {
            return;
        }
        double now = BarrageLoop.clock(stand, partialTick);
        frames.removeIf(frame -> frame != recording && (now - frame.time > ECHO_WINDOW_TICKS || now < frame.time));
        Vec3 eye = camera();
        float viewAlpha = StandViewAlpha.of(stand) * JojohaClientConfig.BARRAGE_TRAIL_OPACITY.value()
                * BarrageGlow.glow(stand, partialTick);
        RenderType glow = RenderType.eyes(texture);
        for (Frame frame : frames) {
            if (frame == recording) {
                continue;
            }
            float fade = (float) (1.0 - (now - frame.time) / ECHO_WINDOW_TICKS);
            int tint = BarrageGlow.additive(colour, ECHO_ALPHA * fade * fade * viewAlpha);
            Matrix4f shift = new Matrix4f().translation((float) (frame.camera.x - eye.x),
                    (float) (frame.camera.y - eye.y), (float) (frame.camera.z - eye.z));
            for (Shot shot : frame.shots) {
                poseStack.pushPose();
                poseStack.last().pose().set(shift).mul(shot.pose());
                poseStack.last().normal().set(shot.normal());
                boolean hidden = shot.bone().isHidden();
                shot.bone().setHidden(false);
                if (!forearm(renderer, poseStack, shot.bone(), shot.forearm(), buffers, glow, tint, tint,
                        packedOverlay)) {
                    renderer.renderCubesOfBone(poseStack, shot.bone(), buffers.getBuffer(glow),
                            LightTexture.FULL_BRIGHT, packedOverlay, tint);
                }
                shot.bone().setHidden(hidden);
                poseStack.popPose();
            }
        }
    }

    private static void phantoms(GeoRenderer<StandEntity> renderer, PoseStack poseStack, StandEntity stand,
                                 BakedGeoModel model, MultiBufferSource buffers, ResourceLocation texture, int colour,
                                 float partialTick, int packedOverlay) {
        if (!rigSeen) {
            return;
        }
        List<BarrageLoop.Phantom> phantoms = BarrageLoop.phantoms(stand, partialTick);
        GeoBone rig = model.getBone(RIG).orElse(null);
        if (phantoms.isEmpty() || rig == null) {
            return;
        }
        float viewAlpha = StandViewAlpha.of(stand);
        float bodyAlpha = PHANTOM_BODY_ALPHA * viewAlpha * BarrageGlow.fade(stand, partialTick);
        float glowAlpha = PHANTOM_GLOW_ALPHA * viewAlpha * BarrageGlow.glow(stand, partialTick);
        RenderType body = RenderType.entityTranslucentEmissive(texture);
        RenderType glow = RenderType.eyes(texture);
        for (BarrageLoop.Phantom phantom : phantoms) {
            StandFist fist = phantom.fist();
            GeoBone arm = model.getBone(StandModel.FIST_ARMS[fist.slot()]).orElse(null);
            if (arm == null) {
                continue;
            }
            float depth = phantom.far() ? FAR_ALPHA : 1F;
            int bodyTint = argb(colour, bodyAlpha * depth);
            int glowTint = BarrageGlow.additive(colour, glowAlpha * depth);
            float[] kept = StandModel.keepPose(arm);
            boolean hidden = arm.isHidden();
            if (StandModel.reachFist(rig, arm, stand, fist, phantom.shoulder(), phantom.age(),
                    phantom.far() ? FAR_GROW : 1F, partialTick)
                    && arm.getScaleY() != 0F) {
                poseStack.pushPose();
                poseStack.last().pose().set(RIG_POSE);
                poseStack.last().normal().set(RIG_NORMAL);
                RenderUtil.prepMatrixForBone(poseStack, arm);
                arm.setHidden(false);
                GeoBone forearm = model.getBone(forearmName(arm.getName())).orElse(null);
                int skinTint = argb(0xFFFFFF, FOREARM_ALPHA * viewAlpha * BarrageGlow.fade(stand, partialTick) * depth);
                if (forearm(renderer, poseStack, arm, forearm, buffers, body, bodyTint, skinTint, packedOverlay)) {
                    forearm(renderer, poseStack, arm, forearm, buffers, glow, glowTint, glowTint, packedOverlay);
                } else {
                    renderer.renderCubesOfBone(poseStack, arm, buffers.getBuffer(body), LightTexture.FULL_BRIGHT,
                            packedOverlay, bodyTint);
                    renderer.renderCubesOfBone(poseStack, arm, buffers.getBuffer(glow), LightTexture.FULL_BRIGHT,
                            packedOverlay, glowTint);
                }
                poseStack.popPose();
            }
            arm.setHidden(hidden);
            StandModel.restorePose(arm, kept);
        }
    }

    static String forearmName(String arm) {
        return arm.contains("left") ? FOREARMS[1] : FOREARMS[0];
    }

    static boolean forearm(GeoRenderer<StandEntity> renderer, PoseStack poseStack, GeoBone arm,
                           GeoBone forearm, MultiBufferSource buffers, RenderType type, int armTint,
                           int skinTint, int packedOverlay) {
        Vec3[] armEnds = forearm == null ? null : StandModel.armEnds(arm);
        Vec3[] lowEnds = forearm == null ? null : StandModel.armEnds(forearm);
        if (armEnds == null || lowEnds == null) {
            return false;
        }
        VertexConsumer out = buffers.getBuffer(type);
        double elbow = lowEnds[0].y - 0.01;
        for (GeoCube cube : arm.getCubes()) {
            if (bottom(cube) >= elbow) {
                drawCube(renderer, poseStack, cube, out, armTint, packedOverlay);
            }
        }
        Vec3 shift = armEnds[1].subtract(lowEnds[1]);
        poseStack.pushPose();
        poseStack.translate(shift.x / 16.0, shift.y / 16.0, shift.z / 16.0);
        for (GeoCube cube : forearm.getCubes()) {
            drawCube(renderer, poseStack, cube, out, skinTint, packedOverlay);
        }
        poseStack.popPose();
        return true;
    }

    private static void drawCube(GeoRenderer<StandEntity> renderer, PoseStack poseStack, GeoCube cube,
                                 VertexConsumer out, int tint, int packedOverlay) {
        poseStack.pushPose();
        renderer.renderCube(poseStack, cube, out, LightTexture.FULL_BRIGHT, packedOverlay, tint);
        poseStack.popPose();
    }

    private static double bottom(GeoCube cube) {
        double low = Double.MAX_VALUE;
        for (GeoQuad quad : cube.quads()) {
            if (quad == null) {
                continue;
            }
            for (GeoVertex vertex : quad.vertices()) {
                low = Math.min(low, vertex.position().y() * 16.0);
            }
        }
        return low;
    }

    private static int argb(int rgb, float alpha) {
        int a = Mth.clamp(Math.round(alpha * 255F), 0, 255);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
