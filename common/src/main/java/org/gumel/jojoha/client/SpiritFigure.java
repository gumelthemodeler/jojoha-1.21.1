package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

final class SpiritFigure {
    private static final float WAIST_BELOW_NECK = 0.75F;

    private static PlayerModel<LivingEntity> wide;
    private static PlayerModel<LivingEntity> slim;

    private SpiritFigure() {
    }

    static void draw(PoseStack pose, MultiBufferSource buffers, float facing, float time, float bow, float nod,
                     float talking, int argb) {
        PlayerSkin skin = skin();
        PlayerModel<LivingEntity> figure = model(skin.model() == PlayerSkin.Model.SLIM);
        stand(figure, time, bow, nod, talking);

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-facing));
        pose.scale(-1F, -1F, 1F);
        pose.translate(0F, -WAIST_BELOW_NECK, 0F);
        figure.renderToBuffer(pose, buffers.getBuffer(Pass.of(skin.texture())), 0xF000F0, OverlayTexture.NO_OVERLAY, argb);
        pose.popPose();
    }

    private static PlayerSkin skin() {
        AbstractClientPlayer viewer = Minecraft.getInstance().player;
        return viewer != null ? viewer.getSkin() : DefaultPlayerSkin.get(Minecraft.getInstance().getUser().getProfileId());
    }

    private static PlayerModel<LivingEntity> model(boolean thinArms) {
        if (thinArms) {
            if (slim == null) {
                slim = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
            }
            return slim;
        }
        if (wide == null) {
            wide = new PlayerModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        }
        return wide;
    }

    private static void stand(PlayerModel<LivingEntity> figure, float time, float bow, float nod, float talking) {
        figure.setAllVisible(true);
        figure.leftLeg.visible = false;
        figure.rightLeg.visible = false;
        figure.leftPants.visible = false;
        figure.rightPants.visible = false;
        figure.young = false;
        figure.crouching = false;
        figure.riding = false;

        figure.head.xRot = (bow + nod) * Mth.DEG_TO_RAD;
        figure.head.yRot = 0F;
        figure.head.zRot = 0.04F * Mth.sin(time * 0.03F);
        figure.body.xRot = 0F;
        figure.body.yRot = 0F;
        figure.body.zRot = 0F;

        float sway = Mth.cos(time * 0.09F) * 0.05F + 0.05F;
        float drift = Mth.sin(time * 0.067F) * 0.05F;
        figure.rightArm.xRot = drift - talking * (0.45F + 0.1F * Mth.sin(time * 0.2F));
        figure.rightArm.yRot = 0F;
        figure.rightArm.zRot = 0.12F + sway;
        figure.leftArm.xRot = -drift;
        figure.leftArm.yRot = 0F;
        figure.leftArm.zRot = -0.12F - sway;

        figure.hat.copyFrom(figure.head);
        figure.jacket.copyFrom(figure.body);
        figure.rightSleeve.copyFrom(figure.rightArm);
        figure.leftSleeve.copyFrom(figure.leftArm);
    }

    private static final class Pass extends RenderType {

        private static final Map<ResourceLocation, RenderType> BY_SKIN = new HashMap<>();

        static RenderType of(ResourceLocation skin) {
            return BY_SKIN.computeIfAbsent(skin, Pass::new);
        }

        private Pass(ResourceLocation skin) {
            this(skin, new TextureStateShard(skin, false, false));
        }

        private Pass(ResourceLocation skin, TextureStateShard sheet) {
            super("jojoha_dream_spirit_" + skin.getPath(), DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false,
                    false,
                    () -> {
                        RENDERTYPE_EYES_SHADER.setupRenderState();
                        sheet.setupRenderState();
                        LIGHTNING_TRANSPARENCY.setupRenderState();
                        COLOR_WRITE.setupRenderState();
                        NO_CULL.setupRenderState();
                        NO_DEPTH_TEST.setupRenderState();
                    },
                    () -> {
                        NO_DEPTH_TEST.clearRenderState();
                        NO_CULL.clearRenderState();
                        COLOR_WRITE.clearRenderState();
                        LIGHTNING_TRANSPARENCY.clearRenderState();
                        sheet.clearRenderState();
                        RENDERTYPE_EYES_SHADER.clearRenderState();
                    });
        }
    }
}
