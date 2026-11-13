package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.ScarfPhysics;
import org.gumel.jojoha.combat.Scarves;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ScarfClient {
    private static final double PIXEL = 1.0 / 16.0;
    private static final double HIDE_NEAR_EYE = 0.6;
    private static final int PIECE_PIXELS = 4;
    private static final double THICKNESS = PIXEL;

    private ScarfClient() {
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Map<UUID, ScarfPhysics.State> states = Scarves.states(true);
        if (mc.level == null) {
            states.clear();
            ScarfAnchor.clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        for (AbstractClientPlayer player : mc.level.players()) {
            if (Scarves.step(player, ScarfAnchor.forTick(player)) == null) {
                ScarfAnchor.forget(player.getUUID());
            }
        }
        states.keySet().removeIf(uuid -> mc.level.getPlayerByUUID(uuid) == null);
    }

    public static void heard(int playerId, int targetId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getEntity(playerId) instanceof Player player) {
            Scarves.heard(player, mc.level.getEntity(targetId));
        }
    }

    public static void render(PoseStack pose, MultiBufferSource buffers, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Map<UUID, ScarfPhysics.State> states = Scarves.states(true);
        if (mc.level == null || states.isEmpty()) {
            return;
        }
        Vec3 cam = camera.getPosition();
        RenderType type = RenderType.entityCutout(ScarfModels.SEGMENT_TEXTURE);
        boolean drew = false;
        for (AbstractClientPlayer player : mc.level.players()) {
            ScarfPhysics.State state = states.get(player.getUUID());
            if (state == null || !state.ready || player.isInvisible()) {
                continue;
            }
            Entity viewer = camera.getEntity();
            boolean own = player == viewer && !camera.isDetached();
            if (own && !state.striking()) {
                continue;
            }
            List<Vec3> curve = new ArrayList<>(ScarfPhysics.NODES);
            curve.add(ScarfAnchor.resolve(player, partialTick));
            for (int i = 1; i < ScarfPhysics.NODES; i++) {
                curve.add(lerp(partialTick, state.shown[i], state.nodes[i]));
            }
            Vec3 right = ScarfPhysics.right(ScarfPhysics.forward(player, partialTick));
            ribbon(pose, buffers.getBuffer(type), cam, curve, right, own, mc);
            drew = true;
        }
        if (drew && buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(type);
        }
    }

    private static void ribbon(PoseStack pose, VertexConsumer out, Vec3 cam, List<Vec3> curve, Vec3 right,
                               boolean own, Minecraft mc) {
        List<Vec3> joints = resample(curve, PIECE_PIXELS * PIXEL);
        int count = joints.size();
        if (count < 2) {
            return;
        }
        Vector3f[] sides = new Vector3f[count];
        Vector3f[] normals = new Vector3f[count];
        Vector3f side = toVector(right);
        for (int k = 0; k < count; k++) {
            Vec3 span = joints.get(Math.min(k + 1, count - 1)).subtract(joints.get(Math.max(k - 1, 0)));
            Vector3f along = span.lengthSqr() < 1.0E-12 ? new Vector3f(0F, -1F, 0F) : toVector(span.normalize());
            side = across(side, along);
            sides[k] = side;
            normals[k] = new Vector3f(side).cross(along).normalize();
        }
        int[] slices = new int[count - 1];
        int total = 0;
        for (int k = 0; k + 1 < count; k++) {
            slices[k] = Math.max(1, (int) Math.round(joints.get(k).distanceTo(joints.get(k + 1)) / PIXEL));
            total += slices[k];
        }

        PoseStack.Pose last = pose.last();
        int pixel = 0;
        for (int k = 0; k + 1 < count; k++) {
            Vec3 a = joints.get(k);
            Vec3 b = joints.get(k + 1);
            if (own && a.distanceToSqr(cam) < HIDE_NEAR_EYE * HIDE_NEAR_EYE) {
                pixel += slices[k];
                continue;
            }
            Vector3f along = toVector(b.subtract(a).normalize());
            Vector3f width = blend(sides[k], sides[k + 1], 0.5);
            Vector3f face = new Vector3f(width).cross(along).normalize();
            int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(a.add(b).scale(0.5)));
            Vec3[] from = corners(a, b, sides, normals, k, 0.0, cam);
            for (int j = 0; j < slices[k]; j++) {
                Vec3[] to = corners(a, b, sides, normals, k, (j + 1) / (double) slices[k], cam);
                slice(out, last, from, to, row(pixel + j, total), light, face, width);
                from = to;
            }
            pixel += slices[k];
            if (k + 2 == count) {
                float v0 = (ScarfModels.SEGMENT_V + ScarfModels.SEGMENT_LENGTH - 1) / (float) ScarfModels.SHEET;
                quad(out, last, from[0], from[1], from[3], from[2], ScarfModels.SEGMENT_U / (float) ScarfModels.SHEET,
                        (ScarfModels.SEGMENT_U + ScarfModels.SEGMENT_WIDTH) / (float) ScarfModels.SHEET,
                        v0, v0 + 1F / ScarfModels.SHEET, light, along, 1F);
            }
        }
    }

    private static void slice(VertexConsumer out, PoseStack.Pose pose, Vec3[] a, Vec3[] b, int row, int light,
                              Vector3f face, Vector3f width) {
        float sheet = ScarfModels.SHEET;
        float u0 = ScarfModels.SEGMENT_U / sheet;
        float u1 = (ScarfModels.SEGMENT_U + ScarfModels.SEGMENT_WIDTH) / sheet;
        float column = 1F / sheet;
        float v0 = (ScarfModels.SEGMENT_V + row) / sheet;
        float v1 = v0 + 1F / sheet;
        quad(out, pose, a[0], a[1], b[1], b[0], u0, u1, v0, v1, light, face, 1F);
        quad(out, pose, b[2], b[3], a[3], a[2], u0, u1, v1, v0, light, face, -1F);
        quad(out, pose, a[1], a[3], b[3], b[1], u1 - column, u1, v0, v1, light, width, 1F);
        quad(out, pose, a[2], a[0], b[0], b[2], u0, u0 + column, v0, v1, light, width, -1F);
    }

    private static Vec3[] corners(Vec3 a, Vec3 b, Vector3f[] sides, Vector3f[] normals, int k, double f, Vec3 cam) {
        Vec3 at = a.add(b.subtract(a).scale(f)).subtract(cam);
        Vector3f side = blend(sides[k], sides[k + 1], f);
        Vector3f normal = blend(normals[k], normals[k + 1], f);
        double half = ScarfModels.SEGMENT_WIDTH * PIXEL * 0.5;
        double skin = THICKNESS * 0.5;
        return new Vec3[]{
                offset(at, side, -half, normal, skin),
                offset(at, side, half, normal, skin),
                offset(at, side, -half, normal, -skin),
                offset(at, side, half, normal, -skin)};
    }

    private static Vector3f blend(Vector3f from, Vector3f to, double f) {
        Vector3f mixed = new Vector3f(from).lerp(to, (float) f);
        return mixed.lengthSquared() < 1.0E-6F ? new Vector3f(from) : mixed.normalize();
    }

    private static Vec3 offset(Vec3 at, Vector3f side, double across, Vector3f normal, double out) {
        return new Vec3(at.x + side.x() * across + normal.x() * out, at.y + side.y() * across + normal.y() * out,
                at.z + side.z() * across + normal.z() * out);
    }

    private static void quad(VertexConsumer out, PoseStack.Pose pose, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4,
                             float uA, float uB, float vA, float vB, int light, Vector3f normal, float facing) {
        vertex(out, pose, p1, uA, vA, light, normal, facing);
        vertex(out, pose, p2, uB, vA, light, normal, facing);
        vertex(out, pose, p3, uB, vB, light, normal, facing);
        vertex(out, pose, p4, uA, vB, light, normal, facing);
    }

    public static int row(int piece, int pieces) {
        int cap = ScarfModels.SEGMENT_CAP;
        int middle = ScarfModels.SEGMENT_LENGTH - 2 * cap;
        if (piece < cap) {
            return piece;
        }
        if (piece >= pieces - cap) {
            return ScarfModels.SEGMENT_LENGTH - (pieces - piece);
        }
        return cap + (piece - cap) % middle;
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Vec3 at, float u, float v, int light,
                               Vector3f normal, float facing) {
        out.addVertex(pose, (float) at.x, (float) at.y, (float) at.z)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, normal.x() * facing, normal.y() * facing, normal.z() * facing);
    }

    private static List<Vec3> resample(List<Vec3> curve, double step) {
        int n = curve.size();
        double[] at = new double[n];
        double total = 0.0;
        for (int i = 1; i < n; i++) {
            total += curve.get(i).distanceTo(curve.get(i - 1));
            at[i] = total;
        }
        List<Vec3> out = new ArrayList<>();
        if (total < 1.0E-4) {
            return out;
        }
        int count = Math.max(1, (int) Math.ceil(total / step));
        double per = total / count;
        int seg = 1;
        for (int k = 0; k <= count; k++) {
            double s = Math.min(total, k * per);
            while (seg < n - 1 && at[seg] < s) {
                seg++;
            }
            double start = at[seg - 1];
            double length = at[seg] - start;
            double f = length < 1.0E-9 ? 0.0 : (s - start) / length;
            out.add(curve.get(seg - 1).add(curve.get(seg).subtract(curve.get(seg - 1)).scale(f)));
        }
        return out;
    }

    private static Vector3f across(Vector3f reference, Vector3f along) {
        Vector3f side = new Vector3f(reference).sub(new Vector3f(along).mul(reference.dot(along)));
        if (side.lengthSquared() < 1.0E-6F) {
            Vector3f spare = Math.abs(along.x()) < 0.9F ? new Vector3f(1F, 0F, 0F) : new Vector3f(0F, 0F, 1F);
            side = spare.sub(new Vector3f(along).mul(spare.dot(along)));
        }
        return side.normalize();
    }

    private static Vector3f toVector(Vec3 v) {
        return new Vector3f((float) v.x, (float) v.y, (float) v.z);
    }

    private static Vec3 lerp(float t, Vec3 from, Vec3 to) {
        return new Vec3(Mth.lerp(t, from.x, to.x), Mth.lerp(t, from.y, to.y), Mth.lerp(t, from.z, to.z));
    }
}
