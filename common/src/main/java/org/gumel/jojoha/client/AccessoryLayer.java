package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.gumel.jojoha.accessory.Accessories;
import org.gumel.jojoha.registry.ModItems;
import org.joml.Vector3f;

public final class AccessoryLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static BubbleGlovesModel gloves;
    private final ModelPart scarf;

    public AccessoryLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                          EntityModelSet models) {
        super(parent);
        gloves = new BubbleGlovesModel(models.bakeLayer(BubbleGlovesModel.LAYER));
        this.scarf = models.bakeLayer(ScarfModels.RING);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        if (Accessories.wearing(player, ModItems.LISA_SCARF.get())) {
            poseStack.pushPose();
            model.head.translateAndRotate(poseStack);
            poseStack.translate(0.0F, ScarfModels.DROP, -ScarfModels.FORWARD);
            scarf.render(poseStack, buffers.getBuffer(RenderType.armorCutoutNoCull(ScarfModels.RING_TEXTURE)), light,
                    OverlayTexture.NO_OVERLAY);
            scarf.translateAndRotate(poseStack);
            ModelPart ring = scarf.getChild("Head");
            ring.translateAndRotate(poseStack);
            ring.getChild("Head_r1").translateAndRotate(poseStack);
            ScarfAnchor.seen(player, poseStack.last().pose().transformPosition(
                    ScarfModels.TAIL_X, ScarfModels.TAIL_Y, ScarfModels.TAIL_Z, new Vector3f()));
            poseStack.popPose();
        }
        if (!wearsGloves(player)) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(RenderType.armorCutoutNoCull(BubbleGlovesModel.TEXTURE));
        draw(poseStack, buffer, light, model.rightArm, gloves.rightArm);
        draw(poseStack, buffer, light, model.leftArm, gloves.leftArm);
    }

    public static void firstPerson(PoseStack poseStack, MultiBufferSource buffers, int light,
                                   AbstractClientPlayer player, ModelPart arm, boolean right) {
        if (gloves == null || !wearsGloves(player)) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(RenderType.armorCutoutNoCull(BubbleGlovesModel.TEXTURE));
        draw(poseStack, buffer, light, arm, right ? gloves.rightArm : gloves.leftArm);
    }

    private static boolean wearsGloves(AbstractClientPlayer player) {
        return Accessories.wearing(player, ModItems.BUBBLE_GLOVES.get());
    }

    private static void draw(PoseStack poseStack, VertexConsumer buffer, int light, ModelPart arm, ModelPart glove) {
        if (!arm.visible) {
            return;
        }
        glove.copyFrom(arm);
        glove.render(poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
    }
}
