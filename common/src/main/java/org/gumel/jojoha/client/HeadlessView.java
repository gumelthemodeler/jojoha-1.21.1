package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.vampire.Headless;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class HeadlessView {
    private static final int FORGET_TICKS = 40;
    private static final float HOP_HEIGHT = 0.12F;
    private static final float HOP_RATE = 0.9F;

    private record Seen(int state, long at) {
    }

    private static final Map<Integer, Seen> SEEN = new HashMap<>();

    private static SkullModel model;

    private HeadlessView() {
    }

    public static void init() {
        Headless.clientView = HeadlessView::state;
    }

    public static void heard(int entityId, int state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Seen before = SEEN.get(entityId);
        if (state == Headless.FREE) {
            SEEN.remove(entityId);
        } else {
            SEEN.put(entityId, new Seen(state, minecraft.level.getGameTime()));
        }
        int was = before == null ? Headless.FREE : before.state();
        Entity entity = minecraft.level.getEntity(entityId);
        if (entity != null && (was == Headless.FREE) != (state == Headless.FREE)) {
            entity.refreshDimensions();
        }
    }

    public static int state(Player player) {
        Seen seen = SEEN.get(player.getId());
        if (seen == null) {
            return Headless.FREE;
        }
        if (player.level().getGameTime() - seen.at() > FORGET_TICKS) {
            SEEN.remove(player.getId());
            return Headless.FREE;
        }
        return seen.state();
    }

    public static void clear() {
        SEEN.clear();
    }

    private static SkullModel model() {
        if (model == null) {
            model = new SkullModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_HEAD));
        }
        return model;
    }

    public static ResourceLocation skin(UUID id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getPlayerByUUID(id) instanceof AbstractClientPlayer player) {
            return player.getSkin().texture();
        }
        return DefaultPlayerSkin.get(id).texture();
    }

    public static boolean render(AbstractClientPlayer player, float partialTick, PoseStack poseStack,
                                 MultiBufferSource buffers, int light) {
        int state = state(player);
        if (state == Headless.FREE) {
            return false;
        }
        if (state == Headless.HEAD) {
            float walk = player.walkAnimation.position(partialTick);
            float speed = player.walkAnimation.speed(partialTick);
            float hop = Math.abs(Mth.sin(walk * HOP_RATE)) * HOP_HEIGHT * Math.min(1F, speed * 2F);
            float yaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
            float pitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
            poseStack.pushPose();
            poseStack.translate(0.0F, hop, 0.0F);
            draw(poseStack, buffers, light, player.getSkin().texture(), yaw, pitch,
                    LivingEntityRenderer.getOverlayCoords(player, 0F));
            poseStack.popPose();
        }
        return true;
    }

    public static void draw(PoseStack poseStack, MultiBufferSource buffers, int light, ResourceLocation skin,
                            float yaw, float pitch, int overlay) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - yaw));
        poseStack.scale(-1F, -1F, 1F);
        SkullModel head = model();
        head.setupAnim(0F, 0F, pitch);
        head.renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityTranslucent(skin)), light, overlay);
        poseStack.popPose();
    }

    public static void drawStill(PoseStack poseStack, MultiBufferSource buffers, int light, ResourceLocation skin,
                                 float yaw) {
        draw(poseStack, buffers, light, skin, yaw, 0F, OverlayTexture.NO_OVERLAY);
    }
}
