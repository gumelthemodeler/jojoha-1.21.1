package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class TrickTell {
    private static final float SIZE = 0.6F;

    private static final double ABOVE = 0.55;

    private static final int FADE_TICKS = 4;

    private record Tell(ResourceLocation icon, long until) {
    }

    private static final Map<Integer, Tell> TELLS = new HashMap<>();

    private TrickTell() {
    }

    public static void show(int entityId, ResourceLocation move, int ticks) {
        ClientLevel level = Minecraft.getInstance().level;
        ResourceLocation icon = SkillIcons.of(move, true);
        if (level == null || icon == null) {
            return;
        }
        TELLS.put(entityId, new Tell(icon, level.getGameTime() + ticks));
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (TELLS.isEmpty() || level == null) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, Tell>> each = TELLS.entrySet().iterator();
        while (each.hasNext()) {
            Map.Entry<Integer, Tell> entry = each.next();
            Tell tell = entry.getValue();
            Entity caster = level.getEntity(entry.getKey());
            if (caster == null || now >= tell.until()) {
                each.remove();
                continue;
            }
            long left = tell.until() - now;
            float alpha = left >= FADE_TICKS ? 1F : left / (float) FADE_TICKS;
            Vec3 spot = caster.getPosition(partialTick).add(0, caster.getBbHeight() + ABOVE, 0)
                    .subtract(camera.getPosition());
            poseStack.pushPose();
            poseStack.translate(spot.x, spot.y, spot.z);
            poseStack.mulPose(camera.rotation());
            poseStack.scale(SIZE, SIZE, SIZE);
            VertexConsumer buffer = buffers.getBuffer(RenderType.textSeeThrough(tell.icon()));
            Matrix4f pose = poseStack.last().pose();
            corner(buffer, pose, -0.5F, -0.5F, 1F, 1F, alpha);
            corner(buffer, pose, 0.5F, -0.5F, 0F, 1F, alpha);
            corner(buffer, pose, 0.5F, 0.5F, 0F, 0F, alpha);
            corner(buffer, pose, -0.5F, 0.5F, 1F, 0F, alpha);
            poseStack.popPose();
        }
    }

    public static void forget() {
        TELLS.clear();
    }

    private static void corner(VertexConsumer buffer, Matrix4f pose, float x, float y, float u, float v, float alpha) {
        buffer.addVertex(pose, x, y, 0F)
                .setColor(1F, 1F, 1F, alpha)
                .setUv(u, v)
                .setLight(LightTexture.FULL_BRIGHT);
    }
}
