package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.util.RenderUtil;

final class StandOutline {
    private static final float PIXEL = 1F / 16F;
    private static final float FRAME_SCALE = 5F;

    private StandOutline() {
    }

    static void cubes(PoseStack poseStack, GeoBone bone, VertexConsumer buffer, float width, boolean framed,
                      int light, int overlay, int colour) {
        if (bone.isHidden()) {
            return;
        }
        GeoCube outer = framed ? outermost(bone) : null;
        for (GeoCube cube : bone.getCubes()) {
            boolean flat = cube.size().x() == 0 || cube.size().y() == 0 || cube.size().z() == 0;
            if (framed && !flat && cube != outer) {
                continue;
            }
            poseStack.pushPose();
            RenderUtil.translateToPivotPoint(poseStack, cube);
            RenderUtil.rotateMatrixAroundCube(poseStack, cube);
            RenderUtil.translateAwayFromPivotPoint(poseStack, cube);
            Matrix3f normalState = poseStack.last().normal();
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            Vector3f centre = ManifestFigure.centre(cube);
            for (GeoQuad quad : cube.quads()) {
                if (quad == null) {
                    continue;
                }
                Vector3f normal = normalState.transform(new Vector3f(quad.normal()));
                RenderUtil.fixInvertedFlatCube(cube, normal);
                if (flat) {
                    plain(quad, poseState, normal, buffer, light, overlay, colour);
                } else if (framed) {
                    frame(quad, poseState, normal, buffer, width * FRAME_SCALE * PIXEL, light, overlay, colour);
                } else {
                    hull(quad, centre, poseState, normal, buffer, width * PIXEL, light, overlay, colour);
                }
            }
            poseStack.popPose();
        }
    }

    private static void plain(GeoQuad quad, Matrix4f pose, Vector3f normal, VertexConsumer buffer, int light,
                              int overlay, int colour) {
        for (GeoVertex vertex : quad.vertices()) {
            emit(buffer, pose, vertex.position(), vertex.texU(), vertex.texV(), normal, 1F, light, overlay, colour);
        }
    }

    private static void hull(GeoQuad quad, Vector3f centre, Matrix4f pose, Vector3f normal, VertexConsumer buffer,
                             float grow, int light, int overlay, int colour) {
        GeoVertex[] vertices = quad.vertices();
        for (int i = vertices.length - 1; i >= 0; i--) {
            Vector3f p = new Vector3f(vertices[i].position());
            p.add(Math.signum(p.x - centre.x) * grow, Math.signum(p.y - centre.y) * grow,
                    Math.signum(p.z - centre.z) * grow);
            emit(buffer, pose, p, vertices[i].texU(), vertices[i].texV(), normal, -1F, light, overlay, colour);
        }
    }

    private static void frame(GeoQuad quad, Matrix4f pose, Vector3f normal, VertexConsumer buffer, float band,
                              int light, int overlay, int colour) {
        GeoVertex[] v = quad.vertices();
        if (v.length != 4) {
            return;
        }
        float across = v[0].position().distance(v[1].position());
        float down = v[1].position().distance(v[2].position());
        if (across <= 0F || down <= 0F) {
            return;
        }
        float a = Math.min(0.5F, band / across);
        float d = Math.min(0.5F, band / down);
        strip(v, 0F, 1F, 0F, d, pose, normal, buffer, light, overlay, colour);
        strip(v, 0F, 1F, 1F - d, 1F, pose, normal, buffer, light, overlay, colour);
        strip(v, 0F, a, d, 1F - d, pose, normal, buffer, light, overlay, colour);
        strip(v, 1F - a, 1F, d, 1F - d, pose, normal, buffer, light, overlay, colour);
    }

    private static void strip(GeoVertex[] v, float s0, float s1, float t0, float t1, Matrix4f pose, Vector3f normal,
                              VertexConsumer buffer, int light, int overlay, int colour) {
        corner(v, s0, t0, pose, normal, buffer, light, overlay, colour);
        corner(v, s1, t0, pose, normal, buffer, light, overlay, colour);
        corner(v, s1, t1, pose, normal, buffer, light, overlay, colour);
        corner(v, s0, t1, pose, normal, buffer, light, overlay, colour);
    }

    private static void corner(GeoVertex[] v, float s, float t, Matrix4f pose, Vector3f normal, VertexConsumer buffer,
                               int light, int overlay, int colour) {
        Vector3f top = new Vector3f(v[0].position()).lerp(v[1].position(), s);
        Vector3f bottom = new Vector3f(v[3].position()).lerp(v[2].position(), s);
        float u = lerp(t, lerp(s, v[0].texU(), v[1].texU()), lerp(s, v[3].texU(), v[2].texU()));
        float w = lerp(t, lerp(s, v[0].texV(), v[1].texV()), lerp(s, v[3].texV(), v[2].texV()));
        emit(buffer, pose, top.lerp(bottom, t), u, w, normal, 1F, light, overlay, colour);
    }

    private static float lerp(float t, float a, float b) {
        return a + (b - a) * t;
    }

    private static GeoCube outermost(GeoBone bone) {
        GeoCube best = null;
        float reach = -1F;
        for (GeoCube cube : bone.getCubes()) {
            GeoQuad quad = cube.quads()[0];
            if (quad == null) {
                continue;
            }
            float r = ManifestFigure.centre(cube).distance(quad.vertices()[0].position());
            if (r > reach) {
                reach = r;
                best = cube;
            }
        }
        return best;
    }

    private static void emit(VertexConsumer buffer, Matrix4f pose, Vector3f p, float u, float v, Vector3f normal,
                             float flip, int light, int overlay, int colour) {
        Vector4f placed = pose.transform(new Vector4f(p, 1F));
        buffer.addVertex(placed.x(), placed.y(), placed.z(), colour, u, v, overlay, light,
                normal.x() * flip, normal.y() * flip, normal.z() * flip);
    }
}
