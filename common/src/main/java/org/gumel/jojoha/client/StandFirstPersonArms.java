package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import org.gumel.jojoha.stand.StandEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public final class StandFirstPersonArms {
    private StandFirstPersonArms() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, int light,
                              AbstractClientPlayer player, HumanoidArm arm,
                              ModelPart hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || player != minecraft.player) {
            return;
        }

        if (StandSkillInput.isPiloting()) {
            return;
        }

        Entity found = StandEntityLookup.localStand(minecraft).orElse(null);
        if (!(found instanceof StandEntity stand) || !stand.getStandType().form().isBound()) {
            return;
        }

        GeoRenderer<StandEntity> renderer = StandArmRender.rendererFor(minecraft, stand);
        if (renderer == null) {
            return;
        }

        BakedGeoModel model = StandArmRender.modelFor(renderer, stand);
        if (model == null) {
            return;
        }

        StandArmRender.armOn(poseStack, buffers, light, stand, renderer, model, hand, boneFor(arm),
                minecraft.getTimer().getGameTimeDeltaPartialTick(false));
    }

    private static String boneFor(HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? StandModel.BONE_ON_RIGHT : StandModel.BONE_ON_LEFT;
    }
}
