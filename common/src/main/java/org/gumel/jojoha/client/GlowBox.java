package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class GlowBox {
    private GlowBox() {
    }

    public static void fill(PoseStack poseStack, VertexConsumer buffer, AABB box, float[] tint, float alpha) {
        draw(poseStack, buffer, box, tint, alpha, false);
    }

    public static void fillLit(PoseStack poseStack, VertexConsumer buffer, AABB box, float[] tint, float alpha) {
        draw(poseStack, buffer, box, tint, alpha, true);
    }

    private static void draw(PoseStack poseStack, VertexConsumer buffer, AABB box, float[] tint, float alpha,
                             boolean lit) {
        Matrix4f pose = poseStack.last().pose();
        float x0 = (float) box.minX;
        float y0 = (float) box.minY;
        float z0 = (float) box.minZ;
        float x1 = (float) box.maxX;
        float y1 = (float) box.maxY;
        float z1 = (float) box.maxZ;
        float[][] corners = {
                {x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0},
                {x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1},
                {x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0},
                {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1},
                {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0},
                {x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1}};
        for (float[] face : corners) {
            for (int i = 0; i < face.length; i += 3) {
                VertexConsumer vertex = buffer.addVertex(pose, face[i], face[i + 1], face[i + 2])
                        .setColor(tint[0], tint[1], tint[2], alpha);
                if (lit) {
                    vertex.setUv(0.5F, 0.5F).setLight(LightTexture.FULL_BRIGHT);
                }
            }
        }
    }
}
