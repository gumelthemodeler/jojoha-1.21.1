package org.gumel.jojoha.client.pillar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.pillar.PillarMen;

public final class PillarBodyLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final int STONE_STAGES = 8;
    private static final ResourceLocation[] STONE = new ResourceLocation[STONE_STAGES];
    private static final ResourceLocation VEINS =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "textures/entity/pillar/heat_veins.png");
    private static final int[] VEIN_PULSE = {0x99, 0xCC, 0xFF, 0xCC};

    static {
        for (int i = 0; i < STONE_STAGES; i++) {
            STONE[i] = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                    "textures/entity/pillar/petrify_" + (i + 1) + ".png");
        }
    }

    public PillarBodyLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    public static int stoneStage(int stone) {
        if (stone <= 0) {
            return 0;
        }
        return Math.min(STONE_STAGES, (int) Math.ceil(stone * STONE_STAGES / (double) PillarMen.PETRIFY_SECONDS));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
                       float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        int id = player.getId();

        if (PillarClient.on(id, PillarClient.HEAT)) {
            long time = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
            int alpha = VEIN_PULSE[(int) ((time / 4) % VEIN_PULSE.length)];
            VertexConsumer veins = buffers.getBuffer(RenderType.entityTranslucentEmissive(VEINS));
            model.renderToBuffer(poseStack, veins, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    (alpha << 24) | 0xFFFFFF);
        }

        int stage = stoneStage(PillarClient.stone(id));
        if (stage > 0) {
            VertexConsumer stone = buffers.getBuffer(RenderType.entityCutoutNoCull(STONE[stage - 1]));
            model.renderToBuffer(poseStack, stone, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        }

        if (bladesOut(player)) {
            blades(poseStack, buffers, model.rightArm, PillarModels.rightBlade());
            blades(poseStack, buffers, model.leftArm, PillarModels.leftBlade());
        }
    }

    public static boolean bladesOut(AbstractClientPlayer player) {
        return !player.isInvisible() && PillarClient.on(player.getId(), PillarClient.BLADES)
                && player.getMainHandItem().isEmpty();
    }

    public static void firstPerson(PoseStack poseStack, MultiBufferSource buffers, AbstractClientPlayer player,
                                   ModelPart arm, boolean right) {
        if (bladesOut(player)) {
            blades(poseStack, buffers, arm, right ? PillarModels.rightBlade() : PillarModels.leftBlade());
        }
    }

    private static void blades(PoseStack poseStack, MultiBufferSource buffers, ModelPart arm, ModelPart blade) {
        blade(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(PillarModels.LIGHT_BLADE_TEXTURE)), arm, blade,
                0xFFFFFFFF);
        blade(poseStack, buffers.getBuffer(RenderType.eyes(PillarModels.LIGHT_BLADE_TEXTURE)), arm, blade, glow());
        bladeBack(poseStack, buffers.getBuffer(RenderType.eyes(PillarModels.LIGHT_BLADE_TEXTURE)), arm, blade, glow(),
                blade == PillarModels.rightBlade() ? RIGHT_BLADE_PLANE : LEFT_BLADE_PLANE);
    }

    private static final float RIGHT_BLADE_PLANE = -1F / 16F;
    private static final float LEFT_BLADE_PLANE = 1F / 16F;

    private static void bladeBack(PoseStack poseStack, VertexConsumer out, ModelPart arm, ModelPart blade, int colour,
                                  float plane) {
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        poseStack.translate(plane, 0F, 0F);
        poseStack.scale(-1F, 1F, 1F);
        poseStack.translate(-plane, 0F, 0F);
        blade.render(poseStack, out, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, colour);
        poseStack.popPose();
    }

    private static int glow() {
        float time = Minecraft.getInstance().level == null ? 0F
                : Minecraft.getInstance().level.getGameTime() + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        float breath = 0.5F + 0.5F * Mth.sin(time * GLOW_BREATH);
        int level = Math.round(255F * (GLOW_LEAST + (GLOW_MOST - GLOW_LEAST) * breath));
        return 0xFF000000 | level << 16 | level << 8 | level;
    }

    private static final float GLOW_BREATH = 0.12F;
    private static final float GLOW_LEAST = 0.55F;
    private static final float GLOW_MOST = 0.8F;

    public static void blade(PoseStack poseStack, VertexConsumer out, ModelPart arm, ModelPart blade, int colour) {
        poseStack.pushPose();
        arm.translateAndRotate(poseStack);
        blade.render(poseStack, out, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, colour);
        poseStack.popPose();
    }
}
