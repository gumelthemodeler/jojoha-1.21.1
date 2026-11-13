package org.gumel.jojoha.client.pillar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

public final class PixelDraw {
    public static final float PIXEL = 1F / 16F;

    private PixelDraw() {
    }

    public static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                              int argb, int light, float nx, float ny, float nz) {
        out.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    public static void box(VertexConsumer out, PoseStack.Pose pose, float sx, float sy, float sz,
                           float[] uv, int argb, int light) {
        box(out, pose, sx, sy, sz, uv, uv, uv, uv, uv, uv, argb, light);
    }

    public static void box(VertexConsumer out, PoseStack.Pose pose, float sx, float sy, float sz,
                           float[] front, float[] back, float[] left, float[] right, float[] top, float[] bottom,
                           int argb, int light) {
        float x0 = -sx / 2, x1 = sx / 2, y0 = -sy / 2, y1 = sy / 2, z0 = -sz / 2, z1 = sz / 2;
        face(out, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, front, argb, light, 0, 0, 1);
        face(out, pose, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, back, argb, light, 0, 0, -1);
        face(out, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, left, argb, light, -1, 0, 0);
        face(out, pose, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, right, argb, light, 1, 0, 0);
        face(out, pose, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, top, argb, light, 0, 1, 0);
        face(out, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, bottom, argb, light, 0, -1, 0);
    }

    private static void face(VertexConsumer out, PoseStack.Pose pose,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float[] uv, int argb, int light, float nx, float ny, float nz) {
        vertex(out, pose, ax, ay, az, uv[0], uv[3], argb, light, nx, ny, nz);
        vertex(out, pose, bx, by, bz, uv[2], uv[3], argb, light, nx, ny, nz);
        vertex(out, pose, cx, cy, cz, uv[2], uv[1], argb, light, nx, ny, nz);
        vertex(out, pose, dx, dy, dz, uv[0], uv[1], argb, light, nx, ny, nz);
    }

    public static float[] skin(int u, int v, int w, int h) {
        return new float[]{u / 64F, v / 64F, (u + w) / 64F, (v + h) / 64F};
    }

    public static void ribbon(VertexConsumer out, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 eye, float width,
                              float u0, float u1, float v0, float v1, int argb, int light) {
        Vec3 along = b.subtract(a);
        Vec3 toEye = eye.subtract(a.add(b).scale(0.5));
        Vec3 side = along.cross(toEye);
        if (side.lengthSqr() < 1.0E-8) {
            return;
        }
        side = side.normalize().scale(width / 2);
        quad(out, pose, a.add(side), a.subtract(side), b.subtract(side), b.add(side), u0, u1, v0, v1, argb, light);
    }

    public static void quad(VertexConsumer out, PoseStack.Pose pose, Vec3 a0, Vec3 a1, Vec3 b1, Vec3 b0,
                            float u0, float u1, float v0, float v1, int argb, int light) {
        vertex(out, pose, (float) a0.x, (float) a0.y, (float) a0.z, u0, v0, argb, light, 0, 1, 0);
        vertex(out, pose, (float) a1.x, (float) a1.y, (float) a1.z, u0, v1, argb, light, 0, 1, 0);
        vertex(out, pose, (float) b1.x, (float) b1.y, (float) b1.z, u1, v1, argb, light, 0, 1, 0);
        vertex(out, pose, (float) b0.x, (float) b0.y, (float) b0.z, u1, v0, argb, light, 0, 1, 0);
    }

    public static Vec3[] basis(Vec3 axis) {
        Vec3 forward = axis.normalize();
        Vec3 side = forward.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-4) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        Vec3 up = side.cross(forward).normalize();
        return new Vec3[]{forward, side, up};
    }
}
