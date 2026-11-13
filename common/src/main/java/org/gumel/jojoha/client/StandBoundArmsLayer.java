package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import org.gumel.jojoha.stand.StandEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class StandBoundArmsLayer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public StandBoundArmsLayer(
            RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }

        StandEntity stand = StandEntityLookup.boundStandOf(player);
        if (stand == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        GeoRenderer<StandEntity> renderer = StandArmRender.rendererFor(minecraft, stand);
        if (renderer == null) {
            return;
        }

        BakedGeoModel model = StandArmRender.modelFor(renderer, stand);
        if (model == null) {
            return;
        }

        PlayerModel<AbstractClientPlayer> body = getParentModel();
        StandArmRender.armOn(poseStack, buffers, light, stand, renderer, model, body.rightArm,
                StandModel.BONE_ON_RIGHT, partialTick);
        StandArmRender.armOn(poseStack, buffers, light, stand, renderer, model, body.leftArm,
                StandModel.BONE_ON_LEFT, partialTick);
    }
}
