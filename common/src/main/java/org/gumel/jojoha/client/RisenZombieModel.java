package org.gumel.jojoha.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.gumel.jojoha.entity.RisenZombie;

public final class RisenZombieModel extends UndeadHumanoidModel<RisenZombie> {

    private static final ResourceLocation MODEL = asset("geo/zombie.geo.json");
    private static final ResourceLocation TEXTURE = asset("textures/entity/mob/zombie.png");
    private static final ResourceLocation ANIMATION = asset("animations/zombie.animation.json");

    private static final float BURIED_DEPTH = 34F;

    private static final float CLIMB_HUNCH = 0.55F;

    private static final float REACH_UP = 2.9F;

    @Override
    public ResourceLocation getModelResource(RisenZombie animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(RisenZombie animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(RisenZombie animatable) {
        return ANIMATION;
    }

    @Override
    protected void poseArms(RisenZombie zombie, float swing, float amount, float age,
                            float partialTick) {
        float attack = zombie.getAttackAnim(partialTick);
        float f = Mth.sin(attack * Mth.PI);
        float g = Mth.sin((1F - (1F - attack) * (1F - attack)) * Mth.PI);
        float out = Mth.PI / (zombie.isAggressive() ? 1.5F : 2.25F) - f * 1.2F + g * 0.4F;
        pose(RIGHT_ARM, out, 0F, 0F);
        pose(LEFT_ARM, out, 0F, 0F);
        bob(age);
    }

    @Override
    protected void poseExtra(RisenZombie zombie, float age, float partialTick) {
        float risen = zombie.emergence(partialTick);
        if (risen >= 1F) {
            return;
        }

        float buried = 1F - risen;
        float depth = buried * buried * (3F - 2F * buried);
        place(BODY, 0F, -BURIED_DEPTH * depth, 0F);
        pose(BODY, CLIMB_HUNCH * buried, 0F, 0F);

        float reach = Mth.lerp(risen, REACH_UP, rotX(RIGHT_ARM));
        pose(RIGHT_ARM, reach + Mth.sin(age * 0.9F) * 0.25F * buried, 0F, 0F);
        pose(LEFT_ARM, reach + Mth.sin(age * 0.9F + Mth.PI) * 0.25F * buried, 0F, 0F);
    }
}
