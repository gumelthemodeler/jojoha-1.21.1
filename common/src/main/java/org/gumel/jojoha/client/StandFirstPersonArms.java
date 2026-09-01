package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import org.gumel.jojoha.stand.StandEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

/**
 * Draws a bound Stand on the user's own arms in first person.
 *
 * <p>The third-person case is a layer on the player model - see StandBoundArmsLayer - and this is
 * the same picture for the one view where that model is not drawn at all. Both hand the same job to
 * {@link StandArmRender}, which is where the two coordinate systems are reconciled; the only
 * difference between the callers is the frame they hand over.
 *
 * <h2>It appears exactly where the arm appears</h2>
 *
 * <p>Hooked to vanilla's hand rendering rather than to the frame, so it shows on the frames a bare
 * arm shows and no others. Vanilla draws no arm at all while an item is held - what you see holding
 * a sword is the sword - so the vines are absent then too. That is the honest rule for something
 * that lives on the arm, and it needs no separate decision about what a vine wrapped round a held
 * pickaxe ought to look like.
 *
 * <p>Unlike the layer there is no arm to enter first: the hand render has already put the stack in
 * the arm's own frame by the time this is called.
 */
public final class StandFirstPersonArms {
    private StandFirstPersonArms() {
    }

    /**
     * Draws the Stand's matching arm, if the viewer has a bound Stand out.
     *
     * <p>Called with the pose stack in vanilla's own model space - the frame the arm
     * {@code ModelPart} is about to be rendered in.
     */
    public static void render(PoseStack poseStack, MultiBufferSource buffers, int light,
                              AbstractClientPlayer player, HumanoidArm arm) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || player != minecraft.player) {
            return;
        }

        // Flying the Stand already hides the hands outright - see ItemInHandRendererMixin - and
        // arms drawn from an entity the camera is not attached to would be the same mismatch.
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

        StandArmRender.arm(poseStack, buffers, light, stand, renderer, model, boneFor(arm),
                minecraft.getTimer().getGameTimeDeltaPartialTick(false));
    }

    /**
     * The bone to draw for a given hand - the opposite-handed one, deliberately.
     *
     * <p>Named once in StandModel, because the geo file's arm names describe the model rather than
     * the body it grows out of, and two places would otherwise each have to work that out for
     * themselves and one of them would eventually get it wrong.
     */
    private static String boneFor(HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? StandModel.BONE_ON_RIGHT : StandModel.BONE_ON_LEFT;
    }
}
