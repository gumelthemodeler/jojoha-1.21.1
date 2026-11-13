package org.gumel.jojoha.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.entity.VampireEntity;

public final class VampireModel extends UndeadHumanoidModel<VampireEntity> {

    private static final ResourceLocation MODEL = asset("geo/vampire.geo.json");
    private static final ResourceLocation[] TEXTURES = {
            asset("textures/entity/mob/vampire_1.png"),
            asset("textures/entity/mob/vampire_2.png"),
            asset("textures/entity/mob/vampire_3.png"),
            asset("textures/entity/mob/vampire_4.png"),
            asset("textures/entity/mob/vampire_5.png"),
    };
    private static final ResourceLocation ANIMATION = asset("animations/vampire.animation.json");

    private static final float ARM_SWING = 1.0F;

    private static final float STALK = 0.45F;

    private static final float STRIKE = 1.6F;

    @Override
    public ResourceLocation getModelResource(VampireEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(VampireEntity animatable) {
        return TEXTURES[Mth.clamp(animatable.variant(), 0, TEXTURES.length - 1)];
    }

    @Override
    public ResourceLocation getAnimationResource(VampireEntity animatable) {
        return ANIMATION;
    }

    @Override
    protected void poseArms(VampireEntity vampire, float swing, float amount, float age,
                            float partialTick) {
        float stalk = vampire.isAggressive() ? STALK : 0F;
        pose(RIGHT_ARM, stalk + Mth.cos(swing * 0.6662F + Mth.PI) * ARM_SWING * amount, 0F, 0F);
        pose(LEFT_ARM, stalk + Mth.cos(swing * 0.6662F) * ARM_SWING * amount, 0F, 0F);

        add(RIGHT_ARM, chop(vampire, partialTick) * STRIKE, 0F);
        bob(age);
    }
}
